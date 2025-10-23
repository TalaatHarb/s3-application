package net.talaatharb.s3.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.s3.dto.CredentialConfig;
import net.talaatharb.s3.exceptions.S3StorageException;

@Slf4j
@RequiredArgsConstructor
public class S3StorageService {

    private final MinioClient minioClient;

    public S3StorageService() {
        this.minioClient = MinioClient.builder()
                .endpoint("https://play.min.io") // replace with your S3-compatible endpoint
                .credentials("minioadmin", "minioadmin") // replace with your credentials
                .build();
    }

    public S3StorageService(CredentialConfig config) {
        this.minioClient = io.minio.MinioClient.builder()
                .endpoint(config.endpoint)
                .credentials(config.accessKey, config.secretKey)
                .build();
    }

    // -------- Bucket Management --------

    public List<String> listBuckets()
            throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
            InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        return minioClient.listBuckets().stream()
                .map(Bucket::name)
                .toList();
    }

    public List<String> listObjects(String bucketName, String prefix)
            throws InvalidKeyException, ErrorResponseException,
            IllegalArgumentException, InsufficientDataException, InternalException, InvalidResponseException,
            NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        List<String> objectNames = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix != null ? prefix : "")
                        .recursive(true)
                        .build());
        for (Result<Item> result : results) {
            objectNames.add(result.get().objectName());
        }
        return objectNames;
    }

    // -------- Upload / Download --------

    public CompletableFuture<Void> uploadFile(String bucketName, String objectName, InputStream inputStream, long size,
            String contentType) {
        return CompletableFuture.runAsync(() -> {
            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, size, -1)
                                .contentType(contentType)
                                .build());
            } catch (Exception e) {
                throw new S3StorageException("Failed to upload: " + objectName, e);
            }
        });
    }

    public CompletableFuture<InputStream> downloadFile(String bucketName, String objectName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build());
            } catch (Exception e) {
                throw new S3StorageException("Failed to download: " + objectName, e);
            }
        });
    }

    // -------- Delete Operations --------

    public void deleteObject(String bucketName, String objectName) throws InvalidKeyException, ErrorResponseException,
            InsufficientDataException, InternalException, InvalidResponseException, NoSuchAlgorithmException,
            ServerException, XmlParserException, IllegalArgumentException, IOException {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
    }

    public void deleteObjects(String bucketName, List<String> objectNames) throws Exception {
        List<DeleteObject> deleteObjects = objectNames.stream()
                .map(DeleteObject::new)
                .toList();

        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(deleteObjects)
                        .build());

        for (Result<DeleteError> result : results) {
            DeleteError error = result.get();
            log.error("Failed to delete {}: {}", error.objectName(), error.message());
        }
    }

    public void deleteBucket(String bucketName, boolean recursive) throws Exception {
        if (recursive) {
            List<String> allObjects = listObjects(bucketName, null);
            if (!allObjects.isEmpty()) {
                deleteObjects(bucketName, allObjects);
            }
        }
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
    }
}
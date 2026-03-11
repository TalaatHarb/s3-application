package net.talaatharb.s3.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
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
import net.talaatharb.s3.dto.ObjectBrowserItem;
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
    // -------- Bucket Management --------

    public List<String> listBuckets()
            throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
            InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        return minioClient.listBuckets().stream()
                .map(Bucket::name)
                .toList();
    }

    public CompletableFuture<Void> createBucket(String bucketName) {
        return CompletableFuture.runAsync(() -> {
            try {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } catch (Exception e) {
                throw new S3StorageException("Failed to create bucket: " + bucketName, e);
            }
        });
    }

    public List<String> listObjects(String bucketName, String prefix)
            throws InvalidKeyException, ErrorResponseException,
            IllegalArgumentException, InsufficientDataException, InternalException, InvalidResponseException,
            NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        List<String> objectNames = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(normalizePrefix(prefix))
                        .recursive(true)
                        .build());
        for (Result<Item> result : results) {
            objectNames.add(result.get().objectName());
        }
        return objectNames;
    }

    public List<ObjectBrowserItem> listBrowserItems(String bucketName, String prefix)
            throws InvalidKeyException, ErrorResponseException,
            IllegalArgumentException, InsufficientDataException, InternalException, InvalidResponseException,
            NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        String normalizedPrefix = normalizePrefix(prefix);
        Map<String, ObjectBrowserItem> entries = new LinkedHashMap<>();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(normalizedPrefix)
                        .recursive(true)
                        .build());

        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();
            if (objectName == null || objectName.isBlank() || objectName.equals(normalizedPrefix)) {
                continue;
            }

            String relativeName = normalizedPrefix.isEmpty()
                    ? objectName
                    : objectName.substring(normalizedPrefix.length());
            if (relativeName.isBlank()) {
                continue;
            }

            int slashIndex = relativeName.indexOf('/');
            if (slashIndex >= 0) {
                String folderName = relativeName.substring(0, slashIndex);
                if (!folderName.isBlank()) {
                    String folderKey = normalizedPrefix + folderName + "/";
                    entries.putIfAbsent(folderKey, new ObjectBrowserItem(folderName, folderKey, true, 0L));
                }
                continue;
            }

            if (!objectName.endsWith("/")) {
                entries.put(objectName, new ObjectBrowserItem(relativeName, objectName, false, item.size()));
            }
        }

        return entries.values().stream()
                .sorted(Comparator.comparing(ObjectBrowserItem::folder).reversed()
                        .thenComparing(ObjectBrowserItem::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<ObjectBrowserItem> listAllObjectItems(String bucketName)
            throws InvalidKeyException, ErrorResponseException,
            IllegalArgumentException, InsufficientDataException, InternalException, InvalidResponseException,
            NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        List<ObjectBrowserItem> objectItems = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .recursive(true)
                        .build());

        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();
            if (objectName == null || objectName.isBlank()) {
                continue;
            }
            boolean folder = objectName.endsWith("/");
            objectItems.add(new ObjectBrowserItem(extractName(objectName, folder), objectName, folder, item.size()));
        }

        return objectItems;
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

    public CompletableFuture<Void> uploadFileFromPath(String bucketName, String objectName, java.nio.file.Path filePath) {
        return CompletableFuture.runAsync(() -> {
            try (java.io.InputStream inputStream = java.nio.file.Files.newInputStream(filePath)) {
                long size = java.nio.file.Files.size(filePath);
                String contentType = java.nio.file.Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                
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

    public CompletableFuture<Void> uploadFolder(String bucketName, java.nio.file.Path folderPath, String prefix) {
        return CompletableFuture.runAsync(() -> {
            String normalizedPrefix = normalizePrefix(prefix);
            try (var pathStream = java.nio.file.Files.walk(folderPath)) {
                pathStream
                    .filter(java.nio.file.Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String relativePath = folderPath.relativize(file).toString().replace("\\", "/");
                            String objectName = !normalizedPrefix.isEmpty()
                                ? normalizedPrefix + relativePath
                                : relativePath;
                            
                            try (java.io.InputStream inputStream = java.nio.file.Files.newInputStream(file)) {
                                long size = java.nio.file.Files.size(file);
                                String contentType = java.nio.file.Files.probeContentType(file);
                                if (contentType == null) {
                                    contentType = "application/octet-stream";
                                }
                                
                                minioClient.putObject(
                                        PutObjectArgs.builder()
                                                .bucket(bucketName)
                                                .object(objectName)
                                                .stream(inputStream, size, -1)
                                                .contentType(contentType)
                                                .build());
                            }
                            log.info("Uploaded: {}", objectName);
                        } catch (Exception e) {
                            throw new S3StorageException("Failed to upload file: " + file, e);
                        }
                    });
            } catch (Exception e) {
                throw new S3StorageException("Failed to upload folder: " + folderPath, e);
            }
        });
    }

    public CompletableFuture<Void> createFolder(String bucketName, String folderPrefix) {
        return CompletableFuture.runAsync(() -> {
            String normalizedPrefix = normalizePrefix(folderPrefix);
            try (InputStream inputStream = InputStream.nullInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(normalizedPrefix)
                                .stream(inputStream, 0, -1)
                                .contentType("application/x-directory")
                                .build());
            } catch (Exception e) {
                throw new S3StorageException("Failed to create folder: " + normalizedPrefix, e);
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

    public CompletableFuture<Void> downloadFolder(String bucketName, String folderPrefix, java.nio.file.Path targetDirectory) {
        return CompletableFuture.runAsync(() -> {
            String normalizedPrefix = normalizePrefix(folderPrefix);
            try {
                java.nio.file.Files.createDirectories(targetDirectory);
                for (String objectName : listObjects(bucketName, normalizedPrefix)) {
                    if (objectName.endsWith("/")) {
                        continue;
                    }
                    String relativeName = objectName.substring(normalizedPrefix.length());
                    java.nio.file.Path outputPath = targetDirectory.resolve(relativeName);
                    java.nio.file.Files.createDirectories(outputPath.getParent());
                    try (InputStream inputStream = minioClient.getObject(
                            GetObjectArgs.builder().bucket(bucketName).object(objectName).build())) {
                        java.nio.file.Files.write(outputPath, inputStream.readAllBytes());
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write downloaded folder: " + normalizedPrefix, e);
            } catch (Exception e) {
                throw new S3StorageException("Failed to download folder: " + normalizedPrefix, e);
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

    public CompletableFuture<Void> deletePrefix(String bucketName, String prefix) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<String> allObjects = listObjects(bucketName, prefix);
                if (!allObjects.isEmpty()) {
                    deleteObjects(bucketName, allObjects);
                }
            } catch (Exception e) {
                throw new S3StorageException("Failed to delete folder: " + prefix, e);
            }
        });
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

    public CompletableFuture<Void> deleteBucketAsync(String bucketName, boolean recursive) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteBucket(bucketName, recursive);
            } catch (Exception e) {
                throw new S3StorageException("Failed to delete bucket: " + bucketName, e);
            }
        });
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }

        String normalizedPrefix = prefix.replace("\\", "/").replaceAll("^/", "");
        if (!normalizedPrefix.endsWith("/")) {
            normalizedPrefix += "/";
        }
        return normalizedPrefix;
    }

    private String extractName(String objectName, boolean folder) {
        String normalized = folder ? objectName.substring(0, objectName.length() - 1) : objectName;
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }
}
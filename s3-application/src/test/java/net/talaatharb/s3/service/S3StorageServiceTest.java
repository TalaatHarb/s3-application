package net.talaatharb.s3.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.Item;

class S3StorageServiceTest {
    private MinioClient minioClient;
    private S3StorageService service;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        service = new S3StorageService(minioClient);
    }

    @Test
    void testListBuckets() throws Exception {
        Bucket bucket = mock(Bucket.class);
        when(bucket.name()).thenReturn("test-bucket");
        when(minioClient.listBuckets()).thenReturn(List.of(bucket));
        List<String> buckets = service.listBuckets();
        assertEquals(List.of("test-bucket"), buckets);
    }

    @Test
    void testListObjects() throws Exception {
        Item item = mock(Item.class);
        when(item.objectName()).thenReturn("obj1");
        @SuppressWarnings("unchecked")
        var result = (Result<Item>) mock(Result.class);
        when(result.get()).thenReturn(item);
        List<Result<Item>> results = List.of(result);
        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(results);
        List<String> objects = service.listObjects("bucket", "prefix");
        assertEquals(List.of("obj1"), objects);
    }

    @Test
    void testDeleteObject() throws Exception {
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));
        assertDoesNotThrow(() -> service.deleteObject("bucket", "obj"));
    }

    @Test
    void testDeleteObjects() throws Exception {
        DeleteError error = mock(DeleteError.class);
        when(error.objectName()).thenReturn("obj");
        when(error.message()).thenReturn("error");
        @SuppressWarnings("unchecked")
        var result = (Result<DeleteError>) mock(Result.class);
        when(result.get()).thenReturn(error);
        List<Result<DeleteError>> results = List.of(result);
        when(minioClient.removeObjects(any(RemoveObjectsArgs.class))).thenReturn(results);
        assertDoesNotThrow(() -> service.deleteObjects("bucket", List.of("obj")));
    }

    @Test
    void testDeleteBucketNonRecursive() throws Exception {
        doNothing().when(minioClient).removeBucket(any(RemoveBucketArgs.class));
        assertDoesNotThrow(() -> service.deleteBucket("bucket", false));
    }

    @Test
    void testDeleteBucketRecursive() throws Exception {
        doNothing().when(minioClient).removeBucket(any(RemoveBucketArgs.class));
        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(new ArrayList<>());
        assertDoesNotThrow(() -> service.deleteBucket("bucket", true));
    }

    // Add more tests for other public methods and exception handling as needed
}
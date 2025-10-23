package net.talaatharb.s3.service;

import io.minio.MinioClient;
import io.minio.messages.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    // Add more tests for other public methods and exception handling as needed
}

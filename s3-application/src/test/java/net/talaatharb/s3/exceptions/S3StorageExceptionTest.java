package net.talaatharb.s3.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3StorageExceptionTest {
    @Test
    void testConstructorWithMessage() {
        S3StorageException ex = new S3StorageException("error");
        assertEquals("error", ex.getMessage());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        Exception cause = new Exception("cause");
        S3StorageException ex = new S3StorageException("error", cause);
        assertEquals("error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}

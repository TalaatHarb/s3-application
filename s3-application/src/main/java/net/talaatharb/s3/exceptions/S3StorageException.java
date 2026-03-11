package net.talaatharb.s3.exceptions;

public class S3StorageException extends RuntimeException {
    public S3StorageException(String message) {
        super(message);
    }

    public S3StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

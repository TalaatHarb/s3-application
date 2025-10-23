package net.talaatharb.s3.service;

public class CredentialConfigException extends Exception {
    public CredentialConfigException(String message) {
        super(message);
    }
    public CredentialConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}

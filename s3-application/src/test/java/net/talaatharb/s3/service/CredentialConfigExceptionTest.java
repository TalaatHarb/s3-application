package net.talaatharb.s3.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CredentialConfigExceptionTest {
    @Test
    void testConstructorWithMessage() {
        CredentialConfigException ex = new CredentialConfigException("error");
        assertEquals("error", ex.getMessage());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        Exception cause = new Exception("cause");
        CredentialConfigException ex = new CredentialConfigException("error", cause);
        assertEquals("error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}

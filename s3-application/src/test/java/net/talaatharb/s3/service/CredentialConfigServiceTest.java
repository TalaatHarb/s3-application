package net.talaatharb.s3.service;

import net.talaatharb.s3.dto.CredentialConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class CredentialConfigServiceTest {
    @TempDir
    Path tempDir;
    CredentialConfigService service;

    @BeforeEach
    void setUp() {
        service = new CredentialConfigService(tempDir);
    }

    @Test
    void testAddAndReadConfig() throws Exception {
        service.addConfig("test", "endpoint", "key", "secret");
        CredentialConfig config = service.readConfig("test");
        assertEquals("endpoint", config.endpoint);
        assertEquals("key", config.accessKey);
        assertEquals("secret", config.secretKey);
    }

    @Test
    void testAddDuplicateConfigThrows() throws Exception {
        service.addConfig("dup", "endpoint", "key", "secret");
        assertThrows(CredentialConfigException.class, () -> service.addConfig("dup", "endpoint", "key", "secret"));
    }

    @Test
    void testUpdateNonexistentConfigThrows() {
        assertThrows(CredentialConfigException.class, () -> service.updateConfig("missing", "endpoint", "key", "secret"));
    }
}

package net.talaatharb.s3.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import net.talaatharb.s3.config.HelperBeans;
import net.talaatharb.s3.dto.CredentialConfig;

@RequiredArgsConstructor
public class CredentialConfigService {
    private static final String PROPERTIES_EXT = ".properties";
    private static final String CONFIG_FOLDER = "credentials-configs";
    private final Path configDir;

    public CredentialConfigService() throws CredentialConfigException {
        String appDir = System.getProperty("user.dir");
        configDir = Paths.get(appDir, CONFIG_FOLDER);
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                throw new CredentialConfigException("Failed to create config directory", e);
            }
        }
    }

    public void addConfig(String configName, String endpoint, String accessKey, String secretKey)
            throws CredentialConfigException {
        Path configPath = configDir.resolve(configName + PROPERTIES_EXT);
        if (Files.exists(configPath)) {
            throw new CredentialConfigException("Configuration already exists: " + configName);
        }
        writeConfig(configPath, configName, endpoint, accessKey, secretKey);
    }

    public void updateConfig(String configName, String endpoint, String accessKey, String secretKey)
            throws CredentialConfigException {
        Path configPath = configDir.resolve(configName + PROPERTIES_EXT);
        if (!Files.exists(configPath)) {
            throw new CredentialConfigException("Configuration does not exist: " + configName);
        }
        writeConfig(configPath, configName, endpoint, accessKey, secretKey);
    }

    public CredentialConfig readConfig(String configName) throws CredentialConfigException {
        Path configPath = configDir.resolve(configName + PROPERTIES_EXT);
        if (!Files.exists(configPath)) {
            throw new CredentialConfigException("Configuration does not exist: " + configName);
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
            props.load(fis);
        } catch (IOException e) {
            throw new CredentialConfigException("Failed to read configuration: " + configName, e);
        }
        String endpoint = props.getProperty("endpoint");
        String accessKey = props.getProperty("accessKey");
        String secretKeyBase64 = props.getProperty("secretKey");
        String secretKey = new String(Base64.getDecoder().decode(secretKeyBase64), StandardCharsets.UTF_8);
        return new CredentialConfig(configName, endpoint, accessKey, secretKey);
    }

    private void writeConfig(Path configPath, String configName, String endpoint, String accessKey, String secretKey)
            throws CredentialConfigException {
        Properties props = new Properties();
        props.setProperty("configName", configName);
        props.setProperty("endpoint", endpoint);
        props.setProperty("accessKey", accessKey);
        props.setProperty("secretKey", Base64.getEncoder().encodeToString(secretKey.getBytes()));
        try (FileOutputStream fos = new FileOutputStream(configPath.toFile())) {
            props.store(fos, "Credential Configuration for " + configName);
        } catch (IOException e) {
            throw new CredentialConfigException("Failed to write configuration: " + configName, e);
        }
    }

    public List<String> listConfigs() throws CredentialConfigException {
        try (var stream = Files.list(configDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(PROPERTIES_EXT))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.properties$", ""))
                    .toList();
        } catch (IOException e) {
            throw new CredentialConfigException("Failed to list configurations", e);
        }
    }
    
    public MinioClient getS3Client(CredentialConfig config) {
        return HelperBeans.buildMinioClient(config);
    }
}
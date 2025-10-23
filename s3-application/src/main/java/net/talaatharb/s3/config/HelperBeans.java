package net.talaatharb.s3.config;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.minio.MinioClient;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.s3.dto.CredentialConfig;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HelperBeans {

    private static ObjectMapper objectMapper;

    public static final ObjectMapper buildObjectMapper() {
        if (objectMapper == null) {
            log.debug("Creating new ObjectMapper bean");
            objectMapper = JsonMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS) // ignore case
                    .enable(SerializationFeature.INDENT_OUTPUT) // pretty format for json
                    .addModule(new JavaTimeModule()) // time module
                    .build();
        } else {
            log.debug("Reusing existing ObjectMapper bean");
        }
        return objectMapper;
    }

    public static final MinioClient buildMinioClient(CredentialConfig config) {
        String decodedSecretKey = new String(config.secretKey);
        return MinioClient.builder()
                .endpoint(config.endpoint)
                .credentials(config.accessKey, decodedSecretKey)
                .build();
    }
}
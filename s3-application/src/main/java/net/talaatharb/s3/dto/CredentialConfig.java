package net.talaatharb.s3.dto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CredentialConfig {
    public final String configName;
    public final String endpoint;
    public final String accessKey;
    public final String secretKey;
}

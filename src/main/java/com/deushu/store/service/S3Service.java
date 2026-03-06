package com.deushu.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public String upload(Path filePath) throws IOException {

        String fileName = UUID.randomUUID() + "-" + filePath.getFileName();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();

        s3Client.putObject(
                putObjectRequest,
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(Files.readAllBytes(filePath))
        );

        return fileName;
    }
}
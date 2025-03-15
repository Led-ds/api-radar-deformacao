package com.ass.br.apiradar.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.time.Duration;

@Configuration
public class S3Config {

    @Bean
    public S3AsyncClient s3AsyncClient() {
        SdkAsyncHttpClient asyncHttpClient = NettyNioAsyncHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(30)) // Tempo limite de conexão
                .readTimeout(Duration.ofSeconds(60)) // Tempo limite de leitura
                .maxConcurrency(50) // Controla número máximo de conexões ativas
                .build();

        return S3AsyncClient.builder()
                .region(Region.US_EAST_1) // Ajuste para sua região da AWS
                .credentialsProvider(ProfileCredentialsProvider.create("terraform"))
                .httpClient(asyncHttpClient)
                .serviceConfiguration(S3Configuration.builder()
                        .checksumValidationEnabled(false) // Desativa validação de checksum (opcional)
                        .pathStyleAccessEnabled(true) // Necessário para alguns provedores S3 compatíveis
                        .build())
                .build();
    }
}

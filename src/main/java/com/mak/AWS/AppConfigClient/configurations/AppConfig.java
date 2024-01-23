package com.mak.AWS.AppConfigClient.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

/**
 * @author Mohammad Aiub Khan
 * Created: 1/20/2024
 */

@Configuration
public class AppConfig {
    @Value("AKIAUSQLI4YPDUC3F5MR")
    private String accessKeyId;

    @Value("WScYlG1kwqdAxToTWZTD1l1yl+mr1SOGIZQ74P5v")
    private String secretKey;

    @Value("us-east-1")
    private String region;

    @Bean
    public AppConfigDataClient appConfigDataClient() {
        //Take credentials from EKS
        //return AppConfigDataClient.builder().region(region).build();
        return AppConfigDataClient.builder()
                .region(Region.of(region))
                .credentialsProvider(() -> AwsBasicCredentials.create(accessKeyId, secretKey))
                .build();
    }

    @Bean
    AppConfigClient appConfigClient() {
        //Region-region = Region.US_EAST_1;
        //Take credentials from EKS
        //return AppConfigClient.builder().region(region).build();

        return AppConfigClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretKey)))
                .build();
    }
}

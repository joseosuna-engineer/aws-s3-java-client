package com.prottonne.aws.s3;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Main {

    @Value("${aws.access.key.id}")
    private String accessKey;

    @Value("${aws.access.key.secret}")
    private String secretKey;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public AmazonS3 s3Client() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        return new AmazonS3Client(credentials);
    }

}

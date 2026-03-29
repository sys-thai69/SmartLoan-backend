package com.smartloan.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
@Profile("!test")
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;

    @Value("${firebase.credentials.json:}")
    private String firebaseCredentialsJson;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(getCredentials())
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
            }
        } catch (IOException e) {
            log.warn("Firebase initialization skipped - credentials not available. This is expected in test environments.");
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    private GoogleCredentials getCredentials() throws IOException {
        // Option 1: JSON credentials as environment variable (for Render/Vercel)
        if (firebaseCredentialsJson != null && !firebaseCredentialsJson.isEmpty()) {
            log.info("Using Firebase credentials from environment variable");
            InputStream stream = new ByteArrayInputStream(
                    firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8)
            );
            return GoogleCredentials.fromStream(stream);
        }

        // Option 2: Path to credentials file (for local development)
        if (firebaseCredentialsPath != null && !firebaseCredentialsPath.isEmpty()) {
            log.info("Using Firebase credentials from file: {}", firebaseCredentialsPath);
            return GoogleCredentials.fromStream(new FileInputStream(firebaseCredentialsPath));
        }

        // Option 3: Default credentials (Google Cloud environment)
        // This will throw an exception if no default credentials are found
        try {
            log.info("Using default Google credentials");
            return GoogleCredentials.getApplicationDefault();
        } catch (IOException e) {
            log.warn("No default Google credentials found: {}. Firebase will not be initialized.", e.getMessage());
            throw e;
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }
}

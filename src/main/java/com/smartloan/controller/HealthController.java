package com.smartloan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic controller for troubleshooting backend/database connectivity
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("backend", "Running");

        // Check database connection
        Map<String, Object> dbStatus = checkDatabase();
        response.put("database", dbStatus);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> checkDbConnection() {
        return ResponseEntity.ok(checkDatabase());
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> dbStatus = new HashMap<>();

        if (dataSource == null) {
            dbStatus.put("status", "UNCONFIGURED");
            dbStatus.put("message", "DataSource not configured");
            return dbStatus;
        }

        try {
            Connection conn = dataSource.getConnection();
            if (conn != null) {
                dbStatus.put("status", "UP");
                dbStatus.put("message", "Database connection successful");
                dbStatus.put("databaseProductName", conn.getMetaData().getDatabaseProductName());
                dbStatus.put("databaseVersion", conn.getMetaData().getDatabaseMajorVersion() + "." + conn.getMetaData().getDatabaseMinorVersion());
                conn.close();
            } else {
                dbStatus.put("status", "DOWN");
                dbStatus.put("message", "Failed to obtain database connection");
            }
        } catch (Exception e) {
            dbStatus.put("status", "DOWN");
            dbStatus.put("message", "Database connection failed: " + e.getMessage());
            dbStatus.put("error", e.getClass().getSimpleName());
        }

        return dbStatus;
    }
}

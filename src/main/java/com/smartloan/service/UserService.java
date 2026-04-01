package com.smartloan.service;

import com.smartloan.dto.*;
import com.smartloan.entity.User;
import com.smartloan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Find user by email, phone, or name
     * Handles flexible input - if it looks like a phone number, search by phone
     */
    public User findUserByFlexibleInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new RuntimeException("Input cannot be empty");
        }

        final String trimmedInput = input.trim();

        // Check if it looks like a phone number (contains only digits, +, -, (, ), spaces)
        String digitsOnly = trimmedInput.replaceAll("[^0-9]", "");
        boolean looksLikePhone = digitsOnly.length() >= 7 && digitsOnly.length() <= 15;

        if (looksLikePhone) {
            // Try to find by phone
            List<User> users = userRepository.findAll();
            for (User user : users) {
                if (user.getPhoneNumber() != null) {
                    String userPhone = user.getPhoneNumber().replaceAll("[^0-9]", "");
                    // Match if phone numbers end with the same digits (flexible matching)
                    if (userPhone.endsWith(digitsOnly) || digitsOnly.endsWith(userPhone)) {
                        return user;
                    }
                }
            }
        }

        // Try email match
        return userRepository.findByEmail(trimmedInput)
                .orElseThrow(() -> new RuntimeException("User not found with email or phone: " + trimmedInput));
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        query = query.toLowerCase().trim();
        List<User> allUsers = userRepository.findAll();
        List<User> results = new ArrayList<>();

        for (User user : allUsers) {
            // Email match
            if (user.getEmail() != null && user.getEmail().toLowerCase().contains(query)) {
                results.add(user);
                continue;
            }

            // Phone match
            if (user.getPhoneNumber() != null) {
                String normalizedPhone = user.getPhoneNumber().replaceAll("[^0-9]", "");
                String normalizedQuery = query.replaceAll("[^0-9]", "");
                if (!normalizedQuery.isEmpty() && (normalizedPhone.contains(normalizedQuery) || normalizedQuery.contains(normalizedPhone))) {
                    results.add(user);
                    continue;
                }
            }

            // Name match
            if (user.getName() != null && user.getName().toLowerCase().contains(query)) {
                results.add(user);
            }
        }

        return results;
    }
}

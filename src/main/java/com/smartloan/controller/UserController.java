package com.smartloan.controller;

import com.smartloan.dto.UserDTO;
import com.smartloan.dto.UpdateProfileRequest;
import com.smartloan.entity.User;
import com.smartloan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(
            @RequestParam String query,
            @AuthenticationPrincipal User currentUser) {
        List<User> users = userRepository.searchByEmailOrName(query);

        // Filter out current user from results
        List<UserDTO> results = users.stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .map(this::toUserDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<UserDTO> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal User currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(request.getName());

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            // Check if phone is already taken by another user
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())
                    && !user.getPhoneNumber().equals(request.getPhoneNumber())) {
                throw new RuntimeException("Phone number already in use");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getProfilePicture() != null && !request.getProfilePicture().isEmpty()) {
            user.setProfilePicture(request.getProfilePicture());
        }

        user = userRepository.save(user);
        return ResponseEntity.ok(toUserDTO(user));
    }

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profilePicture(user.getProfilePicture())
                .role(user.getRole())
                .trustScore(user.getTrustScore())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .createdAt(user.getCreatedAt().toString())
                .build();
    }
}

package com.smartloan.repository;

import com.smartloan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByFirebaseUid(String firebaseUid);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}

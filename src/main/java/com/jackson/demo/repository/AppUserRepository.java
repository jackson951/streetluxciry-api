package com.jackson.demo.repository;
import java.util.UUID;

import com.jackson.demo.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    java.util.List<AppUser> findAllByOrderByCreatedAtDesc();
}

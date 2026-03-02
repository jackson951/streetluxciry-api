package com.jackson.demo.repository;
import java.util.UUID;

import com.jackson.demo.entity.RefreshToken;
import com.jackson.demo.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(AppUser user);
}

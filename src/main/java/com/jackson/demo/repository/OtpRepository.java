package com.jackson.demo.repository;

import com.jackson.demo.entity.Otp;
import com.jackson.demo.model.OtpType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {

    Optional<Otp> findByEmailAndCodeAndType(String email, String code, OtpType type);

    @Query("SELECT o FROM Otp o WHERE o.email = :email AND o.type = :type AND o.expiresAt > :now AND o.used = false ORDER BY o.createdAt DESC")
    List<Otp> findValidOtpsByEmailAndType(@Param("email") String email, @Param("type") OtpType type, @Param("now") Instant now);

    @Query("SELECT o FROM Otp o WHERE o.email = :email AND o.type = :type AND o.expiresAt > :now AND o.used = false ORDER BY o.createdAt DESC")
    Optional<Otp> findLatestValidOtpByEmailAndType(@Param("email") String email, @Param("type") OtpType type, @Param("now") Instant now);

    @Query("SELECT COUNT(o) FROM Otp o WHERE o.email = :email AND o.type = :type AND o.createdAt > :since")
    long countOtpsByEmailAndTypeSince(@Param("email") String email, @Param("type") OtpType type, @Param("since") Instant since);

    void deleteByEmailAndType(String email, OtpType type);
}
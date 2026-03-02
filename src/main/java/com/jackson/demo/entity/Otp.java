package com.jackson.demo.entity;

import com.jackson.demo.model.OtpType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otps")
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType type;

    @Column
    private String purpose; // For forgot password, store the user ID or email

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // Constructors
    public Otp() {}

    public Otp(String email, String code, Instant expiresAt, OtpType type, String purpose) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.type = type;
        this.purpose = purpose;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public OtpType getType() {
        return type;
    }

    public void setType(OtpType type) {
        this.type = type;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
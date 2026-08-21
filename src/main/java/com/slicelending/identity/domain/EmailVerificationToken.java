package com.slicelending.identity.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)

    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)

    private String tokenHash;

    @Column(name = "expires_at", nullable = false)

    private OffsetDateTime expiresAt;

    @Column(name = "used_at")

    private OffsetDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected EmailVerificationToken(){}

    public EmailVerificationToken(
            User user,
            String tokenHash,
            OffsetDateTime expiresAt
    ){
        this.user = Objects.requireNonNull(user, "User must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "Token hash must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration time must not be null");
    }

    public boolean isExpired(OffsetDateTime currentTime){
        return !expiresAt.isAfter(currentTime);
    }

    public boolean isUsed(){
        return usedAt != null;

    }

    public void markAsUsed(OffsetDateTime usedAt){
        this.usedAt = Objects.requireNonNull(usedAt, "used time must not be null");

    }

    public Long getId(){
        return id;
    }
    public User getUser(){
        return user;
    }
    public String getTokenHash(){
        return tokenHash;
    }
    public OffsetDateTime getExpiresAt(){
        return expiresAt;
    }
    public OffsetDateTime getUsedAt(){
        return usedAt;
    }
    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }
}

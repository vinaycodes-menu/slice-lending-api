package com.slicelending.identity.infrastructure;

import com.slicelending.identity.domain.EmailVerificationToken;
import com.slicelending.identity.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    Optional<EmailVerificationToken> findTopByUserOrderByCreatedAtDesc(User user);

long countByUserAndCreatedAtGreaterThanEqual(
        User user,
        OffsetDateTime windowStart
);

List<EmailVerificationToken> findByUserAndUsedAtIsNullAndRevokedAtIsNull(User user);
}

package com.slicelending.identity.application;

import com.slicelending.identity.application.event.EmailVerificationRequestedEvent;
import com.slicelending.identity.domain.AccountStatus;
import com.slicelending.identity.domain.EmailVerificationToken;
import com.slicelending.identity.domain.Role;
import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.EmailVerificationTokenRepository;
import com.slicelending.identity.infrastructure.UserRepository;
import com.slicelending.identity.infrastructure.config.EmailVerificationProperties;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ResendEmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final EmailVerificationProperties emailVerificationProperties;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ResendEmailVerificationService(UserRepository userRepository, EmailVerificationTokenRepository emailVerificationTokenRepository, EmailVerificationTokenService emailVerificationTokenService, EmailVerificationProperties emailVerificationProperties, ApplicationEventPublisher applicationEventPublisher) {
        this.userRepository = userRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.emailVerificationProperties = emailVerificationProperties;
        this.applicationEventPublisher = applicationEventPublisher;


    }

    @Transactional
    public void resend(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailForUpdate(normalizedEmail).orElse(null);

        if (user == null) {
            return;
        }

        if (user.getRole() != Role.CUSTOMER
                || user.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
            return;
        }

        OffsetDateTime currentTime = OffsetDateTime.now();

        Optional<EmailVerificationToken> latestToken =
                emailVerificationTokenRepository
                        .findTopByUserOrderByCreatedAtDesc(user);

        if (latestToken.isPresent()
                && latestToken.get()
                .getCreatedAt()
                .plus(emailVerificationProperties.resendCooldown())
                .isAfter(currentTime)) {
            return;
        }

        // Enforce the hourly verification-email limit
        OffsetDateTime windowStart =
                currentTime.minus(emailVerificationProperties.rateLimitWindow());

        long emailCount =
                emailVerificationTokenRepository
                        .countByUserAndCreatedAtGreaterThanEqual(user, windowStart);

        if (emailCount >= emailVerificationProperties.maxEmailsPerWindow()) {
            return;
        }

        // Revoke previous unused tokens
        List<EmailVerificationToken> unusedTokens =
                emailVerificationTokenRepository
                        .findByUserAndUsedAtIsNullAndRevokedAtIsNull(user);

        unusedTokens.forEach(token -> token.markAsRevoked(currentTime));


        String rawToken = emailVerificationTokenService.createToken(user);
        applicationEventPublisher.publishEvent(
                new EmailVerificationRequestedEvent(
                        user.getEmail(),
                        rawToken
                )
        );
    }

}

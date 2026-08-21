package com.slicelending.identity.application;


import com.slicelending.identity.application.exception.ExpiredVerificationTokenException;
import com.slicelending.identity.application.exception.InvalidVerificationTokenException;
import com.slicelending.identity.domain.AccountStatus;
import com.slicelending.identity.domain.EmailVerificationToken;
import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.EmailVerificationTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class EmailVerificationTokenService {
    private static final Duration TOKEN_EXPIRATION = Duration.ofHours(1);

    private final VerificationTokenGenerator tokenGenerator;
    private final EmailVerificationTokenRepository tokenRepository;


    public EmailVerificationTokenService(VerificationTokenGenerator tokenGenerator, EmailVerificationTokenRepository tokenRepository) {
        this.tokenGenerator = tokenGenerator;
        this.tokenRepository = tokenRepository;
    }
    public String createInitialToken(User user){
        String rawToken = tokenGenerator.generateToken();
        String tokenHash = tokenGenerator.hashToken(rawToken);

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(TOKEN_EXPIRATION);

        EmailVerificationToken verificationToken =
                new EmailVerificationToken(
                        user,
                        tokenHash,
                        expiresAt
                );
        tokenRepository.save(verificationToken);

        return rawToken;
    }

    @Transactional
    public EmailVerificationResult verifyEmail(String rawToken){
        if(rawToken == null || rawToken.isBlank()){
            throw new InvalidVerificationTokenException();
        }
        String tokenHash = tokenGenerator.hashToken(rawToken);

        EmailVerificationToken verificationToken = tokenRepository.findByTokenHash(tokenHash).orElseThrow(InvalidVerificationTokenException::new);
        User user = verificationToken.getUser();

        if(verificationToken.isUsed()){
            if(user.getAccountStatus() == AccountStatus.ACTIVE){
                return EmailVerificationResult.ALREADY_VERIFIED;

            }
            throw new InvalidVerificationTokenException();
        }
        OffsetDateTime currentTime = OffsetDateTime.now();
        if(verificationToken.isExpired(currentTime)){
            throw new ExpiredVerificationTokenException();
        }
        if(user.getAccountStatus() == AccountStatus.ACTIVE){
            verificationToken.markAsUsed(currentTime);
            return EmailVerificationResult.ALREADY_VERIFIED;
        }
        if (user.getAccountStatus() != AccountStatus.PENDING_VERIFICATION){
            throw new InvalidVerificationTokenException();
        }

        user.activateAfterEmailVerification();
        verificationToken.markAsUsed(currentTime);

        return EmailVerificationResult.VERIFIED;
    }
}

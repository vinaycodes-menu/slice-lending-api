package com.slicelending.identity.application;


import com.slicelending.identity.application.exception.ExpiredVerificationTokenException;
import com.slicelending.identity.application.exception.InvalidVerificationTokenException;
import com.slicelending.identity.domain.AccountStatus;
import com.slicelending.identity.domain.EmailVerificationToken;
import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.EmailVerificationTokenRepository;
import com.slicelending.identity.infrastructure.config.EmailVerificationProperties;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class EmailVerificationTokenService {

    private final VerificationTokenGenerator tokenGenerator;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailVerificationProperties properties;

    public EmailVerificationTokenService(VerificationTokenGenerator tokenGenerator, EmailVerificationTokenRepository tokenRepository, EmailVerificationProperties properties) {
        this.tokenGenerator = tokenGenerator;
        this.tokenRepository = tokenRepository;
        this.properties = properties;
    }
    public String createInitialToken(User user) {
        return createToken(user);
    }

    public String createToken(User user){
        String rawToken = tokenGenerator.generateToken();
        String tokenHash = tokenGenerator.hashToken(rawToken);

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(properties.tokenExpiration());

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


        if(verificationToken.isRevoked()){
            throw new InvalidVerificationTokenException();
        }
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

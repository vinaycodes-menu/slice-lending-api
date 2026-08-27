// psql -U slice_app -h localhost -p 5432 -d slice_lending -W

package com.slicelending.identity.application;

import com.slicelending.identity.application.exception.ExpiredVerificationTokenException;
import com.slicelending.identity.application.exception.InvalidVerificationTokenException;
import com.slicelending.identity.domain.AccountStatus;
import com.slicelending.identity.domain.EmailVerificationToken;
import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.EmailVerificationTokenRepository;
import com.slicelending.identity.infrastructure.config.EmailVerificationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenServiceTest {

    @Mock
    private VerificationTokenGenerator tokenGenerator;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailVerificationProperties properties;

    @InjectMocks
    private EmailVerificationTokenService tokenService;

    @Test
    void shouldCreateAndStoreHashedInitialToken() {
        // Arrange
        User user = new User("vinay@example.com", "password-hash");
        String rawToken = "test-raw-token";
        String tokenHash = "a".repeat(64);

        when(tokenGenerator.generateToken()).thenReturn(rawToken);
        when(tokenGenerator.hashToken(rawToken)).thenReturn(tokenHash);
        when(properties.tokenExpiration()).thenReturn(Duration.ofHours(1));

        // Act
        String result = tokenService.createInitialToken(user);

        // Assert
        assertEquals(rawToken, result);

        ArgumentCaptor<EmailVerificationToken> tokenCaptor =
                ArgumentCaptor.forClass(EmailVerificationToken.class);

        verify(tokenRepository).save(tokenCaptor.capture());

        EmailVerificationToken savedToken = tokenCaptor.getValue();

        assertAll(
                () -> assertSame(user, savedToken.getUser()),
                () -> assertEquals(tokenHash, savedToken.getTokenHash()),
                () -> assertFalse(savedToken.isUsed())
        );

        verify(tokenGenerator).generateToken();
        verify(tokenGenerator).hashToken(rawToken);
    }

    @Test
    void shouldRejectBlankVerificationToken() {
        // Act and Assert
        assertThrows(
                InvalidVerificationTokenException.class,
                () -> tokenService.verifyEmail(" ")
        );

        verifyNoInteractions(tokenGenerator, tokenRepository);
    }


    @Test
    void shouldRejectVerificationWhenTokenDoesNotExist() {
        // Arrange
        String rawToken = "random-token";
        String tokenHash = "b".repeat(64);

        when(tokenGenerator.hashToken(rawToken))
                .thenReturn(tokenHash);

        when(tokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                InvalidVerificationTokenException.class,
                () -> tokenService.verifyEmail(rawToken)
        );

        // Verify
        verify(tokenGenerator).hashToken(rawToken);
        verify(tokenRepository).findByTokenHash(tokenHash);
    }

    @Test
    void shouldRejectExpiredVerificationToken() {
        // Arrange
        String rawToken = "expired-raw-token";
        String tokenHash = "c".repeat(64);

        User user = new User("vinay@example.com", "password-hash");

        EmailVerificationToken expiredToken =
                new EmailVerificationToken(
                        user,
                        tokenHash,
                        OffsetDateTime.now().minusMinutes(1)
                );

        when(tokenGenerator.hashToken(rawToken))
                .thenReturn(tokenHash);

        when(tokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(expiredToken));

        // Act and Assert
        assertThrows(
                ExpiredVerificationTokenException.class,
                () -> tokenService.verifyEmail(rawToken)
        );

        assertAll(
                () -> assertEquals(
                        AccountStatus.PENDING_VERIFICATION,
                        user.getAccountStatus()
                ),
                () -> assertFalse(expiredToken.isUsed())
        );
    }

    @Test
    void shouldVerifyEmailAndActivateUser() {
        // Arrange
        String rawToken = "valid-raw-token";
        String tokenHash = "d".repeat(64);

        User user = new User("vinay@example.com", "password-hash");

        EmailVerificationToken validToken =
                new EmailVerificationToken(
                        user,
                        tokenHash,
                        OffsetDateTime.now().plusMinutes(30)
                );

        when(tokenGenerator.hashToken(rawToken))
                .thenReturn(tokenHash);

        when(tokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(validToken));

        // Act
        EmailVerificationResult result =
                tokenService.verifyEmail(rawToken);

        // Assert
        assertAll(
                () -> assertEquals(
                        EmailVerificationResult.VERIFIED,
                        result
                ),
                () -> assertEquals(
                        AccountStatus.ACTIVE,
                        user.getAccountStatus()
                ),
                () -> assertTrue(validToken.isUsed()),
                () -> assertNotNull(validToken.getUsedAt())
        );

        // Verify
        verify(tokenGenerator).hashToken(rawToken);
        verify(tokenRepository).findByTokenHash(tokenHash);
    }

    @Test
    void shouldReturnAlreadyVerifiedWhenUsedTokenIsClickedAgain() {
        // Arrange
        String rawToken = "already-used-token";
        String tokenHash = "e".repeat(64);

        User user = new User("vinay@example.com", "password-hash");
        user.activateAfterEmailVerification();

        EmailVerificationToken usedToken =
                new EmailVerificationToken(
                        user,
                        tokenHash,
                        OffsetDateTime.now().plusMinutes(30)
                );

        OffsetDateTime originalUsedAt = OffsetDateTime.now().minusMinutes(1);
        usedToken.markAsUsed(originalUsedAt);

        when(tokenGenerator.hashToken(rawToken))
                .thenReturn(tokenHash);

        when(tokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(usedToken));

        // Act
        EmailVerificationResult result =
                tokenService.verifyEmail(rawToken);

        // Assert
        assertAll(
                () -> assertEquals(
                        EmailVerificationResult.ALREADY_VERIFIED,
                        result
                ),
                () -> assertEquals(
                        AccountStatus.ACTIVE,
                        user.getAccountStatus()
                ),
                () -> assertEquals(originalUsedAt, usedToken.getUsedAt())
        );
    }

    @Test
    void shouldRejectRevokedVerificationToken() {
        // Arrange
        String rawToken = "revoked-raw-token";
        String tokenHash = "f".repeat(64);

        User user = new User("vinay@example.com", "password-hash");

        EmailVerificationToken revokedToken =
                new EmailVerificationToken(
                        user,
                        tokenHash,
                        OffsetDateTime.now().plusMinutes(30)
                );

        revokedToken.markAsRevoked(OffsetDateTime.now());

        when(tokenGenerator.hashToken(rawToken))
                .thenReturn(tokenHash);

        when(tokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(revokedToken));

        // Act and Assert
        assertThrows(
                InvalidVerificationTokenException.class,
                () -> tokenService.verifyEmail(rawToken)
        );

        assertAll(
                () -> assertEquals(
                        AccountStatus.PENDING_VERIFICATION,
                        user.getAccountStatus()
                ),
                () -> assertFalse(revokedToken.isUsed()),
                () -> assertTrue(revokedToken.isRevoked())
        );
    }
}
package com.slicelending.identity.application;

import com.slicelending.identity.application.event.EmailVerificationRequestedEvent;
import com.slicelending.identity.application.exception.InvalidVerificationTokenException;
import com.slicelending.identity.domain.AccountStatus;
import com.slicelending.identity.domain.EmailVerificationToken;
import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.EmailVerificationTokenRepository;
import com.slicelending.identity.infrastructure.UserRepository;
import com.slicelending.identity.infrastructure.config.EmailVerificationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendEmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailVerificationTokenService tokenService;

    @Mock
    private EmailVerificationProperties properties;

    @Mock
    private ApplicationEventPublisher eventPublisher;


    @InjectMocks
    private ResendEmailVerificationService resendService;

    @Test
    void shouldDoNothingWhenEmailDoesNotExists(){
        // Arrange
        String submittedEmail = "Unknown@Example.com";
        String normalizedEmail = "unknown@example.com";

        when(userRepository.findByEmailForUpdate(normalizedEmail)).thenReturn(Optional.empty());

        // Act
        resendService.resend(submittedEmail);

        // Assert
        verify(userRepository).findByEmailForUpdate(normalizedEmail);

        verifyNoInteractions(
                tokenRepository,
                tokenService,
                properties,
                eventPublisher
        );
    }

    @Test
    void shouldRevokeOldTokenCreateNewTokenAndPublishEvent() {
        // Arrange
        User user = new User("vinay@example.com", "password-hash");

        EmailVerificationToken oldToken =
                new EmailVerificationToken(
                        user,
                        "a".repeat(64),
                        OffsetDateTime.now().plusMinutes(30)
                );

        when(userRepository.findByEmailForUpdate("vinay@example.com"))
                .thenReturn(Optional.of(user));

        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.empty());

        when(properties.rateLimitWindow())
                .thenReturn(Duration.ofHours(1));

        when(properties.maxEmailsPerWindow())
                .thenReturn(5);

        when(tokenRepository.countByUserAndCreatedAtGreaterThanEqual(
                eq(user),
                any(OffsetDateTime.class)
        )).thenReturn(1L);

        when(tokenRepository.findByUserAndUsedAtIsNullAndRevokedAtIsNull(user))
                .thenReturn(List.of(oldToken));

        when(tokenService.createToken(user))
                .thenReturn("new-raw-token");

        // Act
        resendService.resend(" VINAY@EXAMPLE.COM ");

        // Assert
        assertTrue(oldToken.isRevoked());
        assertNotNull(oldToken.getRevokedAt());

        verify(tokenService).createToken(user);

        ArgumentCaptor<EmailVerificationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        EmailVerificationRequestedEvent.class
                );

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        EmailVerificationRequestedEvent event = eventCaptor.getValue();

        assertAll(
                () -> assertEquals("vinay@example.com", event.email()),
                () -> assertEquals("new-raw-token", event.rawToken())
        );
    }

    @Test
    void shouldBlockResendDuringCooldown() {
        // Arrange
        User user = new User("vinay@example.com", "password-hash");

        EmailVerificationToken latestToken =
                mock(EmailVerificationToken.class);

        when(userRepository.findByEmailForUpdate("vinay@example.com"))
                .thenReturn(Optional.of(user));

        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(latestToken));

        when(latestToken.getCreatedAt())
                .thenReturn(OffsetDateTime.now().minusSeconds(30));

        when(properties.resendCooldown())
                .thenReturn(Duration.ofSeconds(60));

        // Act
        resendService.resend("vinay@example.com");

        // Assert
        verify(tokenService, never()).createToken(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(
                tokenRepository,
                never()
        ).findByUserAndUsedAtIsNullAndRevokedAtIsNull(any());
    }

    @Test
    void shouldBlockResendWhenHourlyLimitIsReached() {
        // Arrange
        User user = new User("vinay@example.com", "password-hash");

        when(userRepository.findByEmailForUpdate("vinay@example.com"))
                .thenReturn(Optional.of(user));

        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.empty());

        when(properties.rateLimitWindow())
                .thenReturn(Duration.ofHours(1));

        when(properties.maxEmailsPerWindow())
                .thenReturn(5);

        when(tokenRepository.countByUserAndCreatedAtGreaterThanEqual(
                eq(user),
                any(OffsetDateTime.class)
        )).thenReturn(5L);

        // Act
        resendService.resend("vinay@example.com");

        // Assert
        verify(
                tokenRepository,
                never()
        ).findByUserAndUsedAtIsNullAndRevokedAtIsNull(any());

        verify(tokenService, never()).createToken(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // Test an already-active account

    @Test
    void shouldDoNothingWhenAccountIsAlreadyActive() {
        // Arrange
        User user = new User("vinay@example.com", "password-hash");
        user.activateAfterEmailVerification();

        when(userRepository.findByEmailForUpdate("vinay@example.com"))
                .thenReturn(Optional.of(user));

        // Act
        resendService.resend("vinay@example.com");

        // Assert
        verifyNoInteractions(
                tokenRepository,
                tokenService,
                properties,
                eventPublisher
        );
    }



}

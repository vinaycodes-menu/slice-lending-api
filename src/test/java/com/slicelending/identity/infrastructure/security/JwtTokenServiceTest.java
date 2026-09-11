package com.slicelending.identity.infrastructure.security;


import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtProperties properties;

    @Mock
    private Clock clock;

    @InjectMocks
    private JwtTokenService jwtTokenService;

    @Test
    void shouldGenerateAccessTokenWithExpectedClaims() {

        // Arrange

        Instant issuedAt = Instant.parse("2026-09-01T12:00:00Z");
        Duration expiration = Duration.ofMinutes(15);

        User user = mock(User.class);
        Jwt encodedJwt = mock(Jwt.class);

        when(user.getId()).thenReturn(42L);
        when(clock.instant()).thenReturn(issuedAt);
        when(properties.issuer()).thenReturn("https://api.slicelending.local");
        when(properties.accessTokenExpiration()).thenReturn(expiration);

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(encodedJwt);

        when(encodedJwt.getTokenValue())
                .thenReturn("signed-access-token");

        // Act
        String token = jwtTokenService.generateAccessToken(user);

// Assert
        ArgumentCaptor<JwtEncoderParameters> parametersCaptor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(parametersCaptor.capture());

        JwtEncoderParameters parameters = parametersCaptor.getValue();

        assertAll(
                () -> assertEquals("signed-access-token", token),
                () -> assertEquals(
                        "https://api.slicelending.local",
                        parameters.getClaims().getIssuer().toString()
                ),
                () -> assertEquals(
                        issuedAt,
                        parameters.getClaims().getIssuedAt()
                ),
                () -> assertEquals(
                        issuedAt.plus(expiration),
                        parameters.getClaims().getExpiresAt()
                ),
                () -> assertEquals(
                        "42",
                        parameters.getClaims().getSubject()
                ),
                () -> assertEquals(
                        MacAlgorithm.HS256,
                        parameters.getJwsHeader().getAlgorithm()
                ),
                () -> assertEquals(
                        "JWT",
                        parameters.getJwsHeader().getType()
                ),
                () -> assertFalse(
                        parameters.getClaims().getClaims().containsKey("email")
                ),
                () -> assertFalse(
                        parameters.getClaims().getClaims().containsKey("password")
                )

        );

    }

    @Test
    void shouldRejectUserWithoutPersistedId(){
        // Arrange
        User user = mock(User.class);
        when(user.getId()).thenReturn(null);

        // Act and Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenService.generateAccessToken(user)
        );

        assertEquals(
                "A persisted user is required to generate an access token",
                exception.getMessage()
        );
        verifyNoInteractions(jwtEncoder, clock, properties);
    }



}

package com.slicelending.identity.application;

import com.slicelending.identity.api.LoginRequest;
import com.slicelending.identity.api.LoginResponse;
import com.slicelending.identity.application.exception.InvalidCredentialsException;
import com.slicelending.identity.domain.AccountStatus;
import com.slicelending.identity.domain.Role;
import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.UserRepository;
import com.slicelending.identity.infrastructure.config.JwtProperties;
import com.slicelending.identity.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private LoginService loginService;

    @Test
    void shouldLoginActiveCustomerAndReturnAccessToken(){
        // Arrange : prepare request and active customer
        LoginRequest request = new LoginRequest(
                "VINAY@EXAMPLE.COM",
                "PASSWORD@7673"
        );

        User user = new User(
                "vinay@example.com",
                "stored-password-hash"
        );
        user.activateAfterEmailVerification();

        when(userRepository.findByEmail("vinay@example.com")).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("PASSWORD@7673", "stored-password-hash")).thenReturn(true);

        when(jwtTokenService.generateAccessToken(user)).thenReturn("signed-access-token");

        when(jwtProperties.accessTokenExpiration()).thenReturn(Duration.ofMinutes(15));

        // Act : execute the method begin tested
        LoginResponse response = loginService.login(request);

        // Assert: verify returned response
        assertAll(
                () -> assertEquals(
                        "signed-access-token",
                        response.accessToken()
                ),
                () -> assertEquals(
                        "Bearer",
                        response.tokenType()
                ),
                () -> assertEquals(
                        900,
                        response.expiresIn()
                )
        );

        // verify: confirm important dependency calls
        verify(userRepository)
                .findByEmail("vinay@example.com");

        verify(passwordEncoder).matches("PASSWORD@7673", "stored-password-hash");

        verify(jwtTokenService).generateAccessToken(user);


    }

    @Test
    void shouldRejectUnknownEmailWithoutGeneratingToken() {
        // Arrange
        LoginRequest request = new LoginRequest(
                " UNKNOWN@EXAMPLE.COM ",
                "Unknown@123"
        );

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        // Act and Assert
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> loginService.login(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        // Verify
        verify(userRepository)
                .findByEmail("unknown@example.com");

        verifyNoInteractions(
                passwordEncoder,
                jwtTokenService,
                jwtProperties
        );
    }

    @Test
    void shouldRejectIncorrectPasswordWithoutGeneratingToken(){
        // Arrange

        LoginRequest request = new LoginRequest(
                "VINAY@EXAMPLE.COM",
                "WrongPassword@123"
        );

        User user = new User(
                "VINAY@EXAMPLE.COM",
                "password-hash"
        );
        user.activateAfterEmailVerification();

        when(userRepository.findByEmail("vinay@example.com")).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("WrongPassword@123", "password-hash")).thenReturn(false);

        // Act & Assert

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
        () -> loginService.login(request)

        );

        assertEquals(

                "Invalid email or password",
                exception.getMessage()
        );

        verify(passwordEncoder).matches("WrongPassword@123", "password-hash");

        verifyNoInteractions(jwtTokenService, jwtProperties);
    }

    @Test
    void shouldRejectPendingCustomerWithoutGeneratingToken(){
        // Arrange

        LoginRequest request = new LoginRequest(
                "vinay7673@example.com",
                "Password@123"
        );

        User user = new User("vinay7673@example.com", "password-hash");

        when(userRepository.findByEmail("vinay7673@example.com")).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("Password@123", "password-hash")).thenReturn(true);

        // Act and Assert
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                ()-> loginService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());

        // verify

        verify(passwordEncoder).matches("Password@123", "password-hash" );
        verifyNoInteractions(jwtTokenService, jwtProperties);

    }

    @Test
    void shouldRejectLockedCustomerWithoutGeneratingToken() {
        // Arrange
        LoginRequest request = new LoginRequest(
                "vinay7673@example.com",
                "Password@123"
        );

        User user = mock(User.class);

        when(user.getPasswordHash()).thenReturn("password-hash");
        when(user.getRole()).thenReturn(Role.CUSTOMER);
        when(user.getAccountStatus()).thenReturn(AccountStatus.LOCKED);

        when(userRepository.findByEmail("vinay7673@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("Password@123", "password-hash"))
                .thenReturn(true);

        // Act and Assert
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> loginService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());

        // Verify
        verify(passwordEncoder)
                .matches("Password@123", "password-hash");

        verifyNoInteractions(jwtProperties, jwtTokenService);
    }

    @Test
    void shouldRejectDisabledCustomerWithoutGeneratingToken(){
        LoginRequest request = new LoginRequest("vinay@example.com", "Password@123");

        User user = mock(User.class);

        // Arrange
        when(user.getPasswordHash()).thenReturn("password-hash");
        when(user.getRole()).thenReturn(Role.CUSTOMER);
        when(user.getAccountStatus()).thenReturn(AccountStatus.DISABLED);

        when(userRepository.findByEmail("vinay@example.com")).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("Password@123", "password-hash")).thenReturn(true);

        // Act and Assert
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> loginService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());

        // verify
        verify(passwordEncoder).matches("Password@123", "password-hash");
         verifyNoInteractions(jwtTokenService, jwtProperties);

    }

    @Test
    void shouldRejectNonCustomerWithoutGeneratingToken(){
        LoginRequest request = new LoginRequest(
                "vinay7673@example.com",
                "Password@123"
        );

        User user = mock(User.class);
        when(userRepository.findByEmail("vinay7673@example.com")).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("password-hash");
        when(user.getRole()).thenReturn(Role.ADMIN);
        when(passwordEncoder.matches("Password@123", "password-hash"))
                .thenReturn(true);
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> loginService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verify(passwordEncoder)
                .matches("Password@123", "password-hash");

        verify(user).getRole();


        verifyNoInteractions(jwtTokenService, jwtProperties);

    }


}

package com.slicelending.identity.infrastructure.security;

import com.slicelending.identity.domain.AccountStatus;
import com.slicelending.identity.domain.Role;
import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.Optional;

import static java.nio.file.Files.size;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerJwtAuthenticationConverterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerJwtAuthenticationConverter converter;

    @Test
    void shouldAuthenticateActiveCustomerWithCurrentRole() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        User user = mock(User.class);

        when(jwt.getSubject()).thenReturn("24");
        when(userRepository.findById(24L))
                .thenReturn(Optional.of(user));
        when(user.getAccountStatus())
                .thenReturn(AccountStatus.ACTIVE);
        when(user.getRole())
                .thenReturn(Role.CUSTOMER);

        // Act
        AbstractAuthenticationToken authentication =
                converter.convert(jwt);

        // Assert
        assertAll(
                () -> assertTrue(authentication.isAuthenticated()),
                () -> assertEquals("24", authentication.getName()),
                () -> assertEquals(1, authentication.getAuthorities().size()),
                () -> assertTrue(
                        authentication.getAuthorities()
                                .stream()
                                .anyMatch(authority ->
                                        authority.getAuthority()
                                                .equals("ROLE_CUSTOMER")
                                )
                )
        );

        verify(userRepository).findById(24L);
    }

    @Test
    void shouldRejectTokenWithoutSubject(){
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(null);
        InvalidBearerTokenException exception = assertThrows(
            InvalidBearerTokenException.class,
        () -> converter.convert(jwt)
        );
        assertEquals("Invalid access token", exception.getMessage());

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectTokenWithNonNumericSubject(){
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("not-a-user-id");
        InvalidBearerTokenException exception = assertThrows(
                InvalidBearerTokenException.class,
                () -> converter.convert(jwt)
        );
        assertEquals("Invalid access token", exception.getMessage());

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectTokenWhenUserDoesNotExist(){
        Jwt jwt = mock(Jwt.class);

        when(jwt.getSubject()).thenReturn("42");
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        InvalidBearerTokenException exception = assertThrows(
                InvalidBearerTokenException.class,
                () -> converter.convert(jwt)
        );

        assertEquals("Invalid access token", exception.getMessage());

        verify(userRepository).findById(42L);

    }

    @Test
    void shouldRejectTokenWhenAccountIsLocked(){
        // Arrange
        Jwt jwt = mock(Jwt.class);
        User user = mock(User.class);
         when(jwt.getSubject()).thenReturn("42");
         when(userRepository.findById(42L)).thenReturn(Optional.of(user));
         when(user.getAccountStatus()).thenReturn(AccountStatus.LOCKED);

         // Assert and Act
        InvalidBearerTokenException exception = assertThrows(
                InvalidBearerTokenException.class,
                () -> converter.convert(jwt)
        );

        assertEquals("Invalid access token", exception.getMessage());

        // verify
        verify(userRepository).findById(42L);
        verify(user, never()).getRole();

    }

}

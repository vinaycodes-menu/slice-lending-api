package com.slicelending.identity.application;

import com.slicelending.identity.api.RegisterCustomerResponse;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.slicelending.customer.domain.CustomerProfile;
import com.slicelending.customer.infrastructure.CustomerProfileRepository;
import com.slicelending.identity.api.RegisterCustomerRequest;
import com.slicelending.identity.application.exception.DuplicateEmailException;
import com.slicelending.identity.domain.AccountStatus;
import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        // Arrange
        RegisterCustomerRequest request = new RegisterCustomerRequest(
                "vinay@example.com",
                "secretpass@123",
                "Vinay",
                "Reddy",
                "+19728086806"
        );

        when(userRepository.existsByEmailIgnoreCase("vinay@example.com"))
                .thenReturn(true);

        // Act and Assert
        assertThrows(
                DuplicateEmailException.class,
                () -> registrationService.register(request)
        );

        verify(userRepository)
                .existsByEmailIgnoreCase("vinay@example.com");

        verifyNoInteractions(
                passwordEncoder,
                customerProfileRepository
        );
    }

    @Test
    void shouldRegisterCustomerSuccessfully() {
        // Arrange
        RegisterCustomerRequest request = new RegisterCustomerRequest(
                "vinay@example.com",
                "secretpass@123",
                "Vinay",
                "Reddy",
                "+19724409333"
        );

        when(userRepository.existsByEmailIgnoreCase("vinay@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secretpass@123")).thenReturn("hashed-password");


        User savedUser = mock(User.class);

        when(savedUser.getId()).thenReturn(1L);
        when(savedUser.getEmail()).thenReturn("vinay@example.com");
        when(savedUser.getAccountStatus())
                .thenReturn(AccountStatus.PENDING_VERIFICATION);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        CustomerProfile savedCustomerProfile = mock(CustomerProfile.class);

        when(savedCustomerProfile.getId()).thenReturn(10L);
        when(savedCustomerProfile.getFirstName()).thenReturn("Vinay");
        when(savedCustomerProfile.getLastName()).thenReturn("Reddy");

        when(customerProfileRepository.save(any(CustomerProfile.class)))
                .thenReturn(savedCustomerProfile);

        // Act
        RegisterCustomerResponse response =
                registrationService.register(request);

// Assert
        assertAll(
                () -> assertEquals(1L, response.userId()),
                () -> assertEquals(10L, response.customerProfileId()),
                () -> assertEquals("vinay@example.com", response.email()),
                () -> assertEquals("Vinay", response.firstName()),
                () -> assertEquals("Reddy", response.lastName()),
                () -> assertEquals(
                        AccountStatus.PENDING_VERIFICATION,
                        response.accountStatus()
                )
        );

// Verify
        verify(userRepository)
                .existsByEmailIgnoreCase("vinay@example.com");

        verify(passwordEncoder)
                .encode("secretpass@123");

        verify(userRepository)
                .save(any(User.class));

        verify(customerProfileRepository)
                .save(any(CustomerProfile.class));

    }


}
package com.slicelending.identity.application;

import com.slicelending.customer.domain.CustomerProfile;
import com.slicelending.customer.infrastructure.CustomerProfileRepository;
import com.slicelending.identity.api.RegisterCustomerRequest;
import com.slicelending.identity.api.RegisterCustomerResponse;
import com.slicelending.identity.application.exception.DuplicateEmailException;
import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RegistrationService {
    private final UserRepository userRepository;
    private  final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, CustomerProfileRepository customerProfileRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterCustomerResponse register(RegisterCustomerRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {

            throw new DuplicateEmailException();
        }
        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(normalizedEmail, passwordHash);
        User savedUser = userRepository.save(user);

        CustomerProfile customerProfile = new CustomerProfile(savedUser, request.firstName().trim(), request.lastName().trim(), request.phoneNumber().trim());
        CustomerProfile savedCustomerProfile = customerProfileRepository.save(customerProfile);
        return new RegisterCustomerResponse(savedUser.getId(), savedCustomerProfile.getId(), savedUser.getEmail(), savedCustomerProfile.getFirstName(), savedCustomerProfile.getLastName(), savedUser.getAccountStatus(), "Registration successful");
    }
}

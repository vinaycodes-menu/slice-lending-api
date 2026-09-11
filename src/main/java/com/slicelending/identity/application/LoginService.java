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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    public LoginResponse login(LoginRequest request){
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );
        if (!passwordMatches || user.getRole() != Role.CUSTOMER
        || user.getAccountStatus() != AccountStatus.ACTIVE){
            throw new InvalidCredentialsException();

        }

        String accessToken = jwtTokenService.generateAccessToken(user);

        Long expiresIn = jwtProperties
                .accessTokenExpiration()
                .toSeconds();
        return new LoginResponse(
                accessToken,
                "Bearer",
                expiresIn
        );


    }

}

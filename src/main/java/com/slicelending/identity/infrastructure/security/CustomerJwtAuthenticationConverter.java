package com.slicelending.identity.infrastructure.security;

import com.slicelending.identity.domain.AccountStatus;
import com.slicelending.identity.domain.User;
import com.slicelending.identity.infrastructure.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public final UserRepository userRepository;

    public CustomerJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()){
            throw new InvalidBearerTokenException("Invalid access token");

        }
        long userId;

        try {
            userId = Long.parseLong(subject);
        }catch (NumberFormatException exception){
            throw new InvalidBearerTokenException("Invalid access token");
        }

        if (userId <=0){
            throw new InvalidBearerTokenException("Invalid access token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new InvalidBearerTokenException("Invalid access token"));
        if (user.getAccountStatus() != AccountStatus.ACTIVE){
            throw new InvalidBearerTokenException("Invalid access token");
        }
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                "ROLE_" + user.getRole().name()
        );
        return new JwtAuthenticationToken(
                jwt,
                List.of(authority),
                subject
        );
    }
}

package com.slicelending.identity.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.util.Base64;


@Configuration
public class JwtConfig {
    @Bean
    SecretKey jwtSigningKey(JwtProperties properties){
        final byte[] keyBytes;

        try{
            keyBytes = Base64.getDecoder().decode(properties.signingKey());

        }catch (IllegalArgumentException exception){
            throw new IllegalStateException(
                    "JWT signing key must be valid Base64",
                    exception
            );
        }

        if (keyBytes.length < 25){
            throw new IllegalStateException(
                    "JWT signing key must contain at least 32 bytes"

            );
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey){
        return NimbusJwtEncoder.withSecretKey(jwtSigningKey).algorithm(MacAlgorithm.HS256).build();
    }
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties properties){
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));

        return decoder;
    }

    @Bean
    Clock jwtClock(){
        return Clock.systemUTC();
    }



}

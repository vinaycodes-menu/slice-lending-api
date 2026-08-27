package com.slicelending.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(
        Duration tokenExpiration,
        Duration resendCooldown,
        Duration rateLimitWindow,
        int maxEmailsPerWindow
) {
}

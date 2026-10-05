package com.slicelending.loanapplication.infrastructure.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "app.loan-application")
public record LoanApplicationProperties(
        @NotNull BigDecimal minimumAmount,
        @NotNull BigDecimal maximumAmount
) {
}
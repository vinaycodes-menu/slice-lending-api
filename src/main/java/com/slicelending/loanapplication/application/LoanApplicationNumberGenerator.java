package com.slicelending.loanapplication.application;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class LoanApplicationNumberGenerator {

    public String generate() {
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase(Locale.ROOT);

        return "APP-" + randomPart;
    }
}
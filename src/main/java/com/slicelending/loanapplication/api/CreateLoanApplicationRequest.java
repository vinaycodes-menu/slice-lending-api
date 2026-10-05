package com.slicelending.loanapplication.api;

import com.slicelending.loanapplication.domain.LoanPurpose;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateLoanApplicationRequest(
        @NotNull
        @Digits(integer = 17, fraction = 2)
        BigDecimal requestedAmount,
        @NotNull
        LoanPurpose loanPurpose
) {
}

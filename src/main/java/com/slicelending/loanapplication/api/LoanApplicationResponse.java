package com.slicelending.loanapplication.api;

import com.slicelending.loanapplication.domain.LoanApplicationStatus;
import com.slicelending.loanapplication.domain.LoanPurpose;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LoanApplicationResponse(
        String applicationNumber,
        BigDecimal requestedAmount,
        LoanPurpose loanPurpose,
        LoanApplicationStatus status,
        OffsetDateTime createdAt
) {
}

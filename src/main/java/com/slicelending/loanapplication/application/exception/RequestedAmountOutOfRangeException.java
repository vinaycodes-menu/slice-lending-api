package com.slicelending.loanapplication.application.exception;

import java.math.BigDecimal;

public class RequestedAmountOutOfRangeException extends RuntimeException {

    public RequestedAmountOutOfRangeException(
            BigDecimal minimumAmount,
            BigDecimal maximumAmount
    ) {
        super("Requested amount must be between"
                + minimumAmount
                + " and "
                + maximumAmount
        );
    }
}

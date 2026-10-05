package com.slicelending.loanapplication.api;

import com.slicelending.common.api.error.ApiErrorResponse;
import com.slicelending.loanapplication.application.exception.RequestedAmountOutOfRangeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice(assignableTypes = LoanApplicationController.class)
public class LoanApplicationExceptionHandler {

    @ExceptionHandler(RequestedAmountOutOfRangeException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestedAmountOutOfRange(
            RequestedAmountOutOfRangeException exception,
            HttpServletRequest request
    ){
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "REQUESTED_AMOUNT_OUT_OF_RANGE",
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequestBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ){
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "INVALID_REQUEST_BODY",
                "Request body contains invalid or unsupported values",
                request.getRequestURI(),
                Map.of()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }
}

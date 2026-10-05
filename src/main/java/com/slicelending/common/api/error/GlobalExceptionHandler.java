package com.slicelending.common.api.error;

import com.slicelending.identity.application.ResendEmailVerificationService;
import com.slicelending.identity.application.exception.DuplicateEmailException;
import com.slicelending.identity.application.exception.ExpiredVerificationTokenException;
import com.slicelending.identity.application.exception.InvalidCredentialsException;
import com.slicelending.identity.application.exception.InvalidVerificationTokenException;
import com.slicelending.loanapplication.application.exception.ActiveLoanApplicationAlreadyExistsException;
import com.slicelending.loanapplication.application.exception.CustomerNotEligibleForLoanApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception,
            HttpServletRequest request
    ){
        HttpStatus status = HttpStatus.CONFLICT;
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "Duplicate_email_found",
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

   @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> methodInvalid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
   ) {
       Map<String, String> fieldErrors = new LinkedHashMap<>();
       for (FieldError fieldError : exception.getBindingResult().getFieldErrors()){
           fieldErrors.putIfAbsent(
                   fieldError.getField(),
                   fieldError.getDefaultMessage()
           );
       }
       HttpStatus status = HttpStatus.BAD_REQUEST;
       ApiErrorResponse errorResponse = new ApiErrorResponse(
               Instant.now(),
               status.value(),
               "BAD_REQUEST",
               exception.getMessage(),
               request.getRequestURI(),
               fieldErrors
       );

       return ResponseEntity.status(status).body(errorResponse);

   }

   @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidVerificationToken(
            InvalidVerificationTokenException exception, HttpServletRequest request
   ){
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "Invalid_verification",
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
        return ResponseEntity.status(status).body(errorResponse);
   }

   @ExceptionHandler(ExpiredVerificationTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleExpiredVerificationToken(
            ExpiredVerificationTokenException exception, HttpServletRequest request
   ){
        HttpStatus status = HttpStatus.GONE;
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "EXPIRED_VERIFICATION_TOKEN",
                request.getRequestURI(),
                exception.getMessage(),
                Map.of()
        );
        return  ResponseEntity.status(status).body(errorResponse);





   }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request
    ){
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "MISSING_REQUEST_PARAMETER",
                "Required request parameter ' " +
                        exception.getParameterName() +
                        " ' is missing",
                request.getRequestURI(),
                Map.of()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception, HttpServletRequest request){
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "INVALID_CREDENTIALS",
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()


        );
        return ResponseEntity.status(status).body(errorResponse);

    }

    @ExceptionHandler(ActiveLoanApplicationAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleActiveLoanApplicationAlreadyExists(
            ActiveLoanApplicationAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "ACTIVE_LOAN_APPLICATION_ALREADY_EXISTS",
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(CustomerNotEligibleForLoanApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomerNotEligible(
            CustomerNotEligibleForLoanApplicationException exception,
            HttpServletRequest request
    ){
        HttpStatus status = HttpStatus.CONFLICT;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "CUSTOMER_NOT_ELIGIBLE",
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.status(status).body(errorResponse);
    }

}

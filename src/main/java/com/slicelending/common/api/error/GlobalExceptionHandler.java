package com.slicelending.common.api.error;

import com.slicelending.identity.application.exception.DuplicateEmailException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

}

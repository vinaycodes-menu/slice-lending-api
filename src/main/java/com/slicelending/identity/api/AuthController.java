package com.slicelending.identity.api;

import com.slicelending.identity.application.EmailVerificationResult;
import com.slicelending.identity.application.EmailVerificationTokenService;
import com.slicelending.identity.application.RegistrationService;
import com.slicelending.identity.application.ResendEmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final ResendEmailVerificationService resendEmailVerificationService;

    public AuthController(RegistrationService registrationService, EmailVerificationTokenService emailVerificationTokenService, ResendEmailVerificationService resendEmailVerificationService) {
        this.registrationService = registrationService;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.resendEmailVerificationService = resendEmailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterCustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        RegisterCustomerResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(@RequestParam String token) {
        EmailVerificationResult results = emailVerificationTokenService.verifyEmail(token);
        EmailVerificationResponse response = switch (results) {
            case VERIFIED -> new EmailVerificationResponse(
                    "VERIFIED",
                    "Email verified successfully"
            );

            case ALREADY_VERIFIED -> new EmailVerificationResponse(
                    "ALREADY_VERFIED",
                    "Email has already been verified"
            );
        };
        return ResponseEntity.ok(response);

    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ResendVerificationResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        resendEmailVerificationService.resend(request.email());

        ResendVerificationResponse response = new ResendVerificationResponse(
                "If an eligible account exists for this email, " + "a new verification message will be sent. "
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}



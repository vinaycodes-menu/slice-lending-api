package com.slicelending.identity.api;

import com.slicelending.identity.application.EmailVerificationResult;
import com.slicelending.identity.application.EmailVerificationTokenService;
import com.slicelending.identity.application.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final EmailVerificationTokenService emailVerificationTokenService;

    public AuthController(RegistrationService registrationService, EmailVerificationTokenService emailVerificationTokenService) {
        this.registrationService = registrationService;
        this.emailVerificationTokenService = emailVerificationTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterCustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request){
        RegisterCustomerResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

   @GetMapping("/verify-email")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(@RequestParam String token){
        EmailVerificationResult results = emailVerificationTokenService.verifyEmail(token);
        EmailVerificationResponse response = switch (results){
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
}

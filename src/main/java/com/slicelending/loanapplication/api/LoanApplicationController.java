package com.slicelending.loanapplication.api;

import com.slicelending.loanapplication.application.CreateLoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/loan-applications")
public class LoanApplicationController {

    private final CreateLoanApplicationService createLoanApplicationService;

    public LoanApplicationController(CreateLoanApplicationService createLoanApplicationService) {
        this.createLoanApplicationService = createLoanApplicationService;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<LoanApplicationResponse> createDraft(
            @Valid @RequestBody CreateLoanApplicationRequest request,
            Authentication authentication
    ){
        Long authenticatedUserId = Long.parseLong(authentication.getName());

        LoanApplicationResponse response =
                createLoanApplicationService.create(authenticatedUserId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{applicationNumber}")
                .buildAndExpand(response.applicationNumber())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }
}

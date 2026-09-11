package com.slicelending.customer.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/me")
    public CurrentCustomerResponse getCurrentCustomer(
            Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());

        return new CurrentCustomerResponse(
                userId,
                "CUSTOMER"
        );
    }


}

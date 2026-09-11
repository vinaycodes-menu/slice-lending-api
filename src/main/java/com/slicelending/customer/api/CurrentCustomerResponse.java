package com.slicelending.customer.api;

public record CurrentCustomerResponse(
        Long userId,
        String role
) {
}

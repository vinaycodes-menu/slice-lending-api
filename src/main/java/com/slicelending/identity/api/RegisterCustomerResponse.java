package com.slicelending.identity.api;

import com.slicelending.identity.domain.AccountStatus;

public record RegisterCustomerResponse(
        Long userId,
        Long customerProfileId,
        String email,
        String firstName,
        String lastName,
        AccountStatus accountStatus,
        String message

) {
}

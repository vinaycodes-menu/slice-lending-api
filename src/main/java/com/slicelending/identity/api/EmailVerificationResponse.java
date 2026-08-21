package com.slicelending.identity.api;

public record EmailVerificationResponse(
        String status,
        String message
) {
}

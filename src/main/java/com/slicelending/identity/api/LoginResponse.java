package com.slicelending.identity.api;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

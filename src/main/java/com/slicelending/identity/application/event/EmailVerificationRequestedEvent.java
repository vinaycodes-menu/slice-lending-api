package com.slicelending.identity.application.event;

public record EmailVerificationRequestedEvent(
        String email,
        String rawToken
) {

}

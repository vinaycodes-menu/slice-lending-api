package com.slicelending.identity.application.exception;

public class ExpiredVerificationTokenException extends RuntimeException{

    public ExpiredVerificationTokenException(){
        super("Verification token has expired");
    }
}

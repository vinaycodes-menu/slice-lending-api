package com.slicelending.identity.application.exception;

public class InvalidVerificationTokenException extends RuntimeException{
    public InvalidVerificationTokenException(){
        super("Verification token is invalid");
    }
}

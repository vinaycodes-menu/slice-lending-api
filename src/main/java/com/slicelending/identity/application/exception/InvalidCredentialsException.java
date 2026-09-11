package com.slicelending.identity.application.exception;

public class InvalidCredentialsException extends RuntimeException{

    public InvalidCredentialsException(){
        super("Invalid email or password");
    }
}

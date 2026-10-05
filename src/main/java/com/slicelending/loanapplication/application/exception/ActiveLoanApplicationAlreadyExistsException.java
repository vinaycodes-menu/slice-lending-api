package com.slicelending.loanapplication.application.exception;

public class ActiveLoanApplicationAlreadyExistsException extends RuntimeException{

    public ActiveLoanApplicationAlreadyExistsException(){
        super("An active loan application already exists for this customer");
    }

    public ActiveLoanApplicationAlreadyExistsException(Throwable cause){
        super(
                "An active loan application already exists for this customer", cause
        );
    }
}

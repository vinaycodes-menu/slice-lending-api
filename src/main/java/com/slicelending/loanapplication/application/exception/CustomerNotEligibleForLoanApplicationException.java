package com.slicelending.loanapplication.application.exception;

public class CustomerNotEligibleForLoanApplicationException extends RuntimeException{

    public CustomerNotEligibleForLoanApplicationException(){
        super("Customer account is not eligible to create a loan application");
    }

}

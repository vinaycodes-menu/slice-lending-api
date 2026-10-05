package com.slicelending.loanapplication.application;

import com.slicelending.customer.domain.CustomerProfile;
import com.slicelending.customer.infrastructure.CustomerProfileRepository;
import com.slicelending.identity.domain.AccountStatus;
import com.slicelending.identity.domain.Role;
import com.slicelending.identity.domain.User;
import com.slicelending.loanapplication.api.CreateLoanApplicationRequest;
import com.slicelending.loanapplication.api.LoanApplicationResponse;
import com.slicelending.loanapplication.application.exception.ActiveLoanApplicationAlreadyExistsException;
import com.slicelending.loanapplication.application.exception.CustomerNotEligibleForLoanApplicationException;
import com.slicelending.loanapplication.application.exception.RequestedAmountOutOfRangeException;
import com.slicelending.loanapplication.domain.LoanApplication;
import com.slicelending.loanapplication.domain.LoanApplicationStatus;
import com.slicelending.loanapplication.infrastructure.LoanApplicationRepository;
import com.slicelending.loanapplication.infrastructure.config.LoanApplicationProperties;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CreateLoanApplicationService {

    private final CustomerProfileRepository customerProfileRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicationNumberGenerator loanApplicationNumberGenerator;
    private final LoanApplicationProperties properties;

    public CreateLoanApplicationService(CustomerProfileRepository customerProfileRepository, LoanApplicationRepository loanApplicationRepository, LoanApplicationNumberGenerator loanApplicationNumberGenerator, LoanApplicationProperties properties) {
        this.customerProfileRepository = customerProfileRepository;
        this.loanApplicationRepository = loanApplicationRepository;
        this.loanApplicationNumberGenerator = loanApplicationNumberGenerator;
        this.properties = properties;
    }

    private void validateRequestedAmount(BigDecimal requestedAmount){
        if (requestedAmount == null
        || requestedAmount.compareTo(properties.minimumAmount()) < 0
        || requestedAmount.compareTo(properties.maximumAmount()) > 0){
            throw new RequestedAmountOutOfRangeException(
                    properties.minimumAmount(),
                    properties.maximumAmount()
            );
        }


    }

    private CustomerProfile loadEligibleCustomer(Long authenticatedUserId){
        if (authenticatedUserId == null || authenticatedUserId <= 0){
            throw new CustomerNotEligibleForLoanApplicationException();
        }
        CustomerProfile customerProfile = customerProfileRepository
                .findByUser_Id(authenticatedUserId)
                .orElseThrow(CustomerNotEligibleForLoanApplicationException::new);
        User user = customerProfile.getUser();

        if (user.getRole() != Role.CUSTOMER
        || user.getAccountStatus() != AccountStatus.ACTIVE){
            throw new CustomerNotEligibleForLoanApplicationException();
        }
        return customerProfile;
    }

    private void ensureNoActiveApplication(CustomerProfile customerProfile){
        boolean activeApplicationExists =
                loanApplicationRepository.existsByCustomerProfile_IdAndStatus(
                        customerProfile.getId(),
                        LoanApplicationStatus.DRAFT
                );
        if (activeApplicationExists){
            throw new ActiveLoanApplicationAlreadyExistsException();
        }

    }
    private boolean isDuplicateDraftConstraint(Throwable error) {
        while (error != null) {
            if (error instanceof org.hibernate.exception.ConstraintViolationException violation) {
                return "uk_loan_applications_one_draft_per_customer"
                        .equals(violation.getConstraintName());
            }
            error = error.getCause();
        }
        return false;
    }

    @Transactional
    public LoanApplicationResponse create(
            Long authenticatedUserId,
            CreateLoanApplicationRequest request
    ){
        CustomerProfile customerProfile =
                loadEligibleCustomer(authenticatedUserId);

        validateRequestedAmount(request.requestedAmount());
        ensureNoActiveApplication(customerProfile);

        String applicationNumber = loanApplicationNumberGenerator.generate();

        LoanApplication loanApplication = new LoanApplication(
                applicationNumber,
                customerProfile,
                request.requestedAmount(),
                request.loanPurpose()
        );

        LoanApplication savedApplication;

        try {
            savedApplication = loanApplicationRepository.saveAndFlush(loanApplication);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateDraftConstraint(exception)) {
                throw new ActiveLoanApplicationAlreadyExistsException(exception);
            }
            throw exception;

        }
        return new LoanApplicationResponse(
                savedApplication.getApplicationNumber(),
                savedApplication.getRequestedAmount(),
                savedApplication.getLoanPurpose(),
                savedApplication.getStatus(),
                savedApplication.getCreatedAt()
        );
    }




}

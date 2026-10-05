package com.slicelending.loanapplication.infrastructure;

import com.slicelending.loanapplication.domain.LoanApplication;
import com.slicelending.loanapplication.domain.LoanApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    boolean existsByCustomerProfile_IdAndStatus(
            Long customerProfileId,
            LoanApplicationStatus status
    );


}

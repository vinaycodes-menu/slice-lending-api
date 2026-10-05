package com.slicelending.loanapplication.domain;

import com.slicelending.customer.domain.CustomerProfile;
import jakarta.persistence.*;
import jdk.jshell.Snippet;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "loan_applications")
public class LoanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "application_number",
            nullable = false,
            unique = true,
            length = 36,
            updatable = false
    )
    private String applicationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customerProfile;

    @Column(
            name = "requested_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "loan_purpose", nullable = false, length = 40
    )
    private LoanPurpose loanPurpose;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status", nullable = false, length = 30
    )
    private LoanApplicationStatus status;

    @Version
    @Column(
            name = "version", nullable = false
    )
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LoanApplication(){

    }

    public LoanApplication(
            String applicationNumber,
            CustomerProfile customerProfile,
            BigDecimal requestedAmount,
            LoanPurpose loanPurpose
    ){
        this.applicationNumber = applicationNumber;
        this.customerProfile = customerProfile;
        this.requestedAmount = requestedAmount ;
        this.loanPurpose = loanPurpose ;
        this.status = LoanApplicationStatus.DRAFT;

    }

    public Long getId(){
        return id;
    }
    public String getApplicationNumber(){
        return applicationNumber;
    }
    public CustomerProfile getCustomerProfile(){
        return customerProfile;
    }
    public BigDecimal getRequestedAmount(){
        return requestedAmount;
    }
    public LoanPurpose getLoanPurpose(){
        return loanPurpose;
    }
    public LoanApplicationStatus getStatus(){
        return status;
    }
    public Long getVersion(){
        return version;
    }
    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }
    public OffsetDateTime getUpdatedAt(){
        return updatedAt;
    }




}

package com.slicelending.loanapplication.application;

import com.slicelending.customer.domain.CustomerProfile;
import com.slicelending.customer.infrastructure.CustomerProfileRepository;
import com.slicelending.identity.domain.User;
import com.slicelending.loanapplication.api.CreateLoanApplicationRequest;
import com.slicelending.loanapplication.api.LoanApplicationResponse;
import com.slicelending.loanapplication.application.exception.ActiveLoanApplicationAlreadyExistsException;
import com.slicelending.loanapplication.application.exception.CustomerNotEligibleForLoanApplicationException;
import com.slicelending.loanapplication.application.exception.RequestedAmountOutOfRangeException;
import com.slicelending.loanapplication.domain.LoanApplication;
import com.slicelending.loanapplication.domain.LoanApplicationStatus;
import com.slicelending.loanapplication.domain.LoanPurpose;
import com.slicelending.loanapplication.infrastructure.LoanApplicationRepository;
import com.slicelending.loanapplication.infrastructure.config.LoanApplicationProperties;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class CreateLoanApplicationServiceTest {

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanApplicationNumberGenerator loanApplicationNumberGenerator;


    @Mock
    private LoanApplicationProperties properties;

    @InjectMocks
    private CreateLoanApplicationService createLoanApplicationService;

    @Test
    void shouldCreateDraftLoanApplicationForEligibleCustomer() {
        // Arrange
        Long authenticatedUserId = 7L;
        Long customerProfileId = 11L;

        CreateLoanApplicationRequest request =
                new CreateLoanApplicationRequest(
                        new BigDecimal("15000.00"),
                        LoanPurpose.DEBT_CONSOLIDATION
                );

        User user = new User(
                "customer@example.com",
                "password-hash"
        );
        user.activateAfterEmailVerification();

        CustomerProfile customerProfile =
                mock(CustomerProfile.class);

        when(customerProfile.getId())
                .thenReturn(customerProfileId);
        when(customerProfile.getUser())
                .thenReturn(user);

        when(customerProfileRepository.findByUser_Id(authenticatedUserId))
                .thenReturn(Optional.of(customerProfile));

        when(properties.minimumAmount())
                .thenReturn(new BigDecimal("1000.00"));
        when(properties.maximumAmount())
                .thenReturn(new BigDecimal("100000.00"));

        when(loanApplicationRepository
                .existsByCustomerProfile_IdAndStatus(
                        customerProfileId,
                        LoanApplicationStatus.DRAFT
                ))
                .thenReturn(false);

        String applicationNumber =
                "APP-550E8400E29B41D4A716446655440000";

        when(loanApplicationNumberGenerator.generate())
                .thenReturn(applicationNumber);

        when(loanApplicationRepository.saveAndFlush(
                any(LoanApplication.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LoanApplicationResponse response =
                createLoanApplicationService.create(
                        authenticatedUserId,
                        request
                );

        // Assert
        assertAll(
                () -> assertEquals(
                        applicationNumber,
                        response.applicationNumber()
                ),
                () -> assertEquals(
                        new BigDecimal("15000.00"),
                        response.requestedAmount()
                ),
                () -> assertEquals(
                        LoanPurpose.DEBT_CONSOLIDATION,
                        response.loanPurpose()
                ),
                () -> assertEquals(
                        LoanApplicationStatus.DRAFT,
                        response.status()
                )
        );

        // Verify the entity sent to the repository
        ArgumentCaptor<LoanApplication> captor =
                ArgumentCaptor.forClass(LoanApplication.class);

        verify(loanApplicationRepository)
                .saveAndFlush(captor.capture());

        LoanApplication capturedApplication =
                captor.getValue();

        assertAll(
                () -> assertSame(
                        customerProfile,
                        capturedApplication.getCustomerProfile()
                ),
                () -> assertEquals(
                        LoanApplicationStatus.DRAFT,
                        capturedApplication.getStatus()
                )
        );
    }

    @Test
    void shouldRejectCustomerWhoAlreadyHasDraft(){
        Long userId = 7L;
        Long profileId = 11L;

        User user = new User("customer@example.com", "password-hash");
        user.activateAfterEmailVerification();

        CustomerProfile profile = mock(CustomerProfile.class);
        when(profile.getUser()).thenReturn(user);
        when(profile.getId()).thenReturn(profileId);

        when(customerProfileRepository.findByUser_Id(userId))
                .thenReturn(Optional.of(profile));
        when(properties.minimumAmount())
                .thenReturn(new BigDecimal("1000.00"));
        when(properties.maximumAmount()).thenReturn(new BigDecimal("100000.00"));

        when(loanApplicationRepository.existsByCustomerProfile_IdAndStatus(
                profileId, LoanApplicationStatus.DRAFT
        )).thenReturn(true);

        CreateLoanApplicationRequest request = new CreateLoanApplicationRequest(
                new BigDecimal("15000.00"),
                LoanPurpose.DEBT_CONSOLIDATION
        );

        assertThrows(
                ActiveLoanApplicationAlreadyExistsException.class,
                () -> createLoanApplicationService.create(userId, request)
        );

        verifyNoInteractions(loanApplicationNumberGenerator);
        verify(loanApplicationRepository, never()).saveAndFlush(any(LoanApplication.class));
    }

    @Test
    void shouldRejectLowerAmountLimit (){
        Long userId = 7L;


        User user = new User("vinay@email.com", "password-hash");
        user.activateAfterEmailVerification();

        CustomerProfile profile = mock(CustomerProfile.class);
        when(profile.getUser()).thenReturn(user);

        when(customerProfileRepository.findByUser_Id(userId))
                .thenReturn(Optional.of(profile));
        when(properties.minimumAmount()).thenReturn(new BigDecimal("1000.00"));


        CreateLoanApplicationRequest request = new CreateLoanApplicationRequest(
                new BigDecimal("999.99"),
                LoanPurpose.DEBT_CONSOLIDATION
        );

        assertThrows(
                RequestedAmountOutOfRangeException.class,
                () -> createLoanApplicationService.create(userId, request)
        );

        verifyNoInteractions(loanApplicationRepository, loanApplicationNumberGenerator);

    }

    @Test
    void shouldRejectUpperAmountLimit(){
        Long userId = 7L;

        User user = new User("vinay@email.com", "password-hash");
        user.activateAfterEmailVerification();

        CustomerProfile profile = mock(CustomerProfile.class);
        when(profile.getUser()).thenReturn(user);

        when(customerProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));
        when(properties.minimumAmount()).thenReturn(new BigDecimal("1000.00"));

        when(properties.maximumAmount()).thenReturn(new BigDecimal("100000.00"));

        CreateLoanApplicationRequest request = new CreateLoanApplicationRequest(
                new BigDecimal("100000.01"),
                LoanPurpose.DEBT_CONSOLIDATION
        );

        assertThrows(
                RequestedAmountOutOfRangeException.class,
                ()-> createLoanApplicationService.create(userId, request)
        );

        verifyNoInteractions(loanApplicationRepository, loanApplicationNumberGenerator);




    }

    @Test
    void shouldNotTreatDatabaseErrorAsDraft() {
        Long userId = 8L;

        User user = new User("vinay@example.com", "password-hash");
        user.activateAfterEmailVerification();

        CustomerProfile profile = mock(CustomerProfile.class);
        when(profile.getUser()).thenReturn(user);
        when(profile.getId()).thenReturn(12L);
        when(customerProfileRepository.findByUser_Id(userId))
                .thenReturn(Optional.of(profile));

        when(properties.minimumAmount())
                .thenReturn(new BigDecimal("1000.00"));
        when(properties.maximumAmount())
                .thenReturn(new BigDecimal("100000.00"));

        CreateLoanApplicationRequest request = new CreateLoanApplicationRequest(
                new BigDecimal("15000.00"),
                LoanPurpose.DEBT_CONSOLIDATION
        );

        when(loanApplicationNumberGenerator.generate())
                .thenReturn("APP-550E8400E29B41D4A716446655440000");

        when(loanApplicationRepository.saveAndFlush(any(LoanApplication.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Application number is already in use"
                ));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> createLoanApplicationService.create(userId, request)
        );
    }

    @Test
    void shouldTranslateDuplicateDraftDatabaseConstraint(){
        Long userId = 11L;

        User user = new User("vinay@email.com", "password-hash");
        user.activateAfterEmailVerification();


        CustomerProfile profile = mock(CustomerProfile.class);
        when(profile.getUser()).thenReturn(user);
        when(profile.getId()).thenReturn(12L);
        when(customerProfileRepository.findByUser_Id(userId))
                .thenReturn(Optional.of(profile));

        when(properties.minimumAmount()).thenReturn(new BigDecimal("1000.00"));
        when(properties.maximumAmount()).thenReturn(new BigDecimal("100000"));

        CreateLoanApplicationRequest request = new CreateLoanApplicationRequest(
                new BigDecimal("15000.00"),
                LoanPurpose.DEBT_CONSOLIDATION
        );

        when(loanApplicationNumberGenerator.generate())
                .thenReturn("APP-550E8400E29B41D4A716446655440000");

        org.hibernate.exception.ConstraintViolationException constraintError =
                mock(org.hibernate.exception.ConstraintViolationException.class);

        when(constraintError.getConstraintName())
                .thenReturn("uk_loan_applications_one_draft_per_customer");
        DataIntegrityViolationException dataBaseError =
                new DataIntegrityViolationException(
                        "Duplicate draft",
                        constraintError
                );
        DataIntegrityViolationException databaseError =
                new DataIntegrityViolationException(
                        "Duplicate draft",
                        constraintError
                );
        when(loanApplicationRepository.saveAndFlush(any(LoanApplication.class))).thenThrow(databaseError);

        ActiveLoanApplicationAlreadyExistsException thrown = assertThrows(
                ActiveLoanApplicationAlreadyExistsException.class,
                () -> createLoanApplicationService.create(userId, request)
        );

        assertSame(databaseError, thrown.getCause());
    }

    @Test
    void shouldRejectPendingCustomer(){
        Long userId = 11L;

        User user = new User("vinay@example.com", "password-hash");
        CustomerProfile profile = mock(CustomerProfile.class);

        when(profile.getUser()).thenReturn(user);
        when(customerProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));

        CreateLoanApplicationRequest request = new CreateLoanApplicationRequest(
                new BigDecimal("15000.00"),
                LoanPurpose.DEBT_CONSOLIDATION
        );

        assertThrows(
                CustomerNotEligibleForLoanApplicationException.class,
                () -> createLoanApplicationService.create(userId, request)
        );
        verifyNoInteractions(
                properties,
                loanApplicationNumberGenerator,
                loanApplicationRepository
        );
    }
}
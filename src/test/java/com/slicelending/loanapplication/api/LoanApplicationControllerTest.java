package com.slicelending.loanapplication.api;


import com.slicelending.loanapplication.application.CreateLoanApplicationService;
import com.slicelending.loanapplication.application.exception.ActiveLoanApplicationAlreadyExistsException;
import com.slicelending.loanapplication.application.exception.RequestedAmountOutOfRangeException;
import com.slicelending.loanapplication.domain.LoanApplicationStatus;
import com.slicelending.loanapplication.domain.LoanPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateLoanApplicationService createLoanApplicationService;

    @Test
    void shouldReturnUnauthorizedWithoutAccessToken() throws Exception{
        mockMvc.perform(
                post("/api/v1/loan-applications")
                        .contentType("application/json")
                        .content("""
                                {
                                "requestedAmount" : 15000.00,
                                "loanPurpose" : "DEBT_CONSOLIDATION"
                                }
                                """)

        ).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenForNonCustomerRole() throws Exception {
        mockMvc.perform(
                post("/api/v1/loan-applications")
                        .with(user("42").roles("ADMIN"))
                        .contentType("application/json")
                        .content("""
                            {
                              "requestedAmount": 15000.00,
                              "loanPurpose": "DEBT_CONSOLIDATION"
                            }
                            """)
        ).andExpect(status().isForbidden());

        verifyNoInteractions(createLoanApplicationService);
    }

    @Test
    void shouldCreateDraftForAuthenticatedCustomer() throws Exception {
        String applicationNumber =
                "APP-550E8400E29B41D4A716446655440000";

        OffsetDateTime createdAt =
                OffsetDateTime.parse("2026-09-22T15:00:00Z");

        LoanApplicationResponse response =
                new LoanApplicationResponse(
                        applicationNumber,
                        new BigDecimal("15000.00"),
                        LoanPurpose.DEBT_CONSOLIDATION,
                        LoanApplicationStatus.DRAFT,
                        createdAt
                );

        when(createLoanApplicationService.create(
                eq(42L),
                any(CreateLoanApplicationRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/loan-applications")
                                .with(user("42").roles("CUSTOMER"))
                                .contentType("application/json")
                                .content("""
                            {
                              "requestedAmount": 15000.00,
                              "loanPurpose": "DEBT_CONSOLIDATION"
                            }
                            """)
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/v1/loan-applications/"
                                + applicationNumber
                ))
                .andExpect(jsonPath("$.applicationNumber")
                        .value(applicationNumber))
                .andExpect(jsonPath("$.requestedAmount")
                        .value(15000.00))
                .andExpect(jsonPath("$.loanPurpose")
                        .value("DEBT_CONSOLIDATION"))
                .andExpect(jsonPath("$.status")
                        .value("DRAFT"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-09-22T15:00:00Z"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.customerId").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist());

        verify(createLoanApplicationService).create(
                eq(42L),
                any(CreateLoanApplicationRequest.class)
        );
    }

    @Test
    void shouldReturnBadRequestWhenRequestedAmountIsMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/loan-applications")
                                .with(user("42").roles("CUSTOMER"))
                                .contentType("application/json")
                                .content("""
                            {
                              "loanPurpose": "DEBT_CONSOLIDATION"
                            }
                            """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.requestedAmount").exists());

        verifyNoInteractions(createLoanApplicationService);
    }

    @Test
    void shouldReturnConflictWhenActiveApplicationAlreadyExists()
            throws Exception {

        when(createLoanApplicationService.create(
                eq(42L),
                any(CreateLoanApplicationRequest.class)
        )).thenThrow(
                new ActiveLoanApplicationAlreadyExistsException()
        );

        mockMvc.perform(
                        post("/api/v1/loan-applications")
                                .with(user("42").roles("CUSTOMER"))
                                .contentType("application/json")
                                .content("""
                            {
                              "requestedAmount": 15000.00,
                              "loanPurpose": "DEBT_CONSOLIDATION"
                            }
                            """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("ACTIVE_LOAN_APPLICATION_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value(
                        "An active loan application already exists for this customer"
                ));
    }

    @Test
    void shouldReturnBadRequestForUnsupportedLoanPurpose() throws Exception {
        mockMvc.perform(
                        post("/api/v1/loan-applications")
                                .with(user("42").roles("CUSTOMER"))
                                .contentType("application/json")
                                .content("""
                            {
                              "requestedAmount": 15000.00,
                              "loanPurpose": "VACATION"
                            }
                            """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value(
                        "Request body contains invalid or unsupported values"
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/v1/loan-applications"
                ))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());

        verifyNoInteractions(createLoanApplicationService);
    }

    @Test
    void shouldReturnBadRequestWhenRequestedAmountIsOutOfRange()
            throws Exception {

        when(createLoanApplicationService.create(
                eq(42L),
                any(CreateLoanApplicationRequest.class)
        )).thenThrow(
                new RequestedAmountOutOfRangeException(
                        new BigDecimal("1000.00"),
                        new BigDecimal("100000.00")
                )
        );

        mockMvc.perform(
                        post("/api/v1/loan-applications")
                                .with(user("42").roles("CUSTOMER"))
                                .contentType("application/json")
                                .content("""
                            {
                              "requestedAmount": 999.99,
                              "loanPurpose": "DEBT_CONSOLIDATION"
                            }
                            """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("REQUESTED_AMOUNT_OUT_OF_RANGE"));
    }

    @Test
    void shouldReturnBadRequestForMoreThanTwoDecimalPlaces()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/loan-applications")
                                .with(user("42").roles("CUSTOMER"))
                                .contentType("application/json")
                                .content("""
                            {
                              "requestedAmount": 15000.123,
                              "loanPurpose": "DEBT_CONSOLIDATION"
                            }
                            """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.requestedAmount").exists());

        verifyNoInteractions(createLoanApplicationService);
    }

    @Test
    void shouldUseAuthenticatedUserAndServerControlledStatus()
            throws Exception {

        String applicationNumber =
                "APP-550E8400E29B41D4A716446655440000";

        LoanApplicationResponse response =
                new LoanApplicationResponse(
                        applicationNumber,
                        new BigDecimal("15000.00"),
                        LoanPurpose.DEBT_CONSOLIDATION,
                        LoanApplicationStatus.DRAFT,
                        OffsetDateTime.parse("2026-09-25T05:00:00Z")
                );

        when(createLoanApplicationService.create(
                eq(42L),
                any(CreateLoanApplicationRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/loan-applications")
                                .with(user("42").roles("CUSTOMER"))
                                .contentType("application/json")
                                .content("""
                            {
                              "requestedAmount": 15000.00,
                              "loanPurpose": "DEBT_CONSOLIDATION",
                              "userId": 999,
                              "customerId": 999,
                              "status": "APPROVED"
                            }
                            """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(createLoanApplicationService).create(
                eq(42L),
                argThat(request ->
                        request.requestedAmount()
                                .compareTo(new BigDecimal("15000.00")) == 0
                                && request.loanPurpose()
                                == LoanPurpose.DEBT_CONSOLIDATION
                )
        );
    }


}

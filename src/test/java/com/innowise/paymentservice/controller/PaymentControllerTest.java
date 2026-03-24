package com.innowise.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.paymentservice.config.RequestAuthContext;
import com.innowise.paymentservice.config.RequesterRole;
import com.innowise.paymentservice.exception.GlobalExceptionHandler;
import com.innowise.paymentservice.model.dto.PaymentRequestDto;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import com.innowise.paymentservice.model.dto.TotalAmountResponseDto;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import com.innowise.paymentservice.service.AccessPolicyService;
import com.innowise.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AccessPolicyService accessPolicyService;

    @Test
    void createShouldReturnCreatedPayment() throws Exception {
        PaymentRequestDto request = new PaymentRequestDto(1L, 2L, BigDecimal.valueOf(19.99));
        PaymentResponseDto response = new PaymentResponseDto(
                "64f0c3d5a3a8435b2dd5d911",
                1L,
                2L,
                PaymentStatus.SUCCESS,
                Instant.parse("2026-03-18T12:00:00Z"),
                BigDecimal.valueOf(19.99)
        );

        when(paymentService.create(any(PaymentRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.orderId").value(1));
    }

    @Test
    void createShouldValidateRequestBody() throws Exception {
        PaymentRequestDto request = new PaymentRequestDto(null, 2L, BigDecimal.valueOf(19.99));

        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("orderId is required"));
    }

    @Test
    void getPaymentsShouldReturnFilteredList() throws Exception {
        PaymentResponseDto response = new PaymentResponseDto(
                "64f0c3d5a3a8435b2dd5d911",
                1L,
                2L,
                PaymentStatus.SUCCESS,
                Instant.parse("2026-03-18T12:00:00Z"),
                BigDecimal.valueOf(19.99)
        );

        when(paymentService.getPayments(2L, null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/payments")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "USER")
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(2));
    }

    @Test
    void getCurrentUserTotalShouldUseHeaders() throws Exception {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-31T00:00:00Z");
        when(accessPolicyService.requireContext(any())).thenReturn(new RequestAuthContext(5L, RequesterRole.USER, null));
        when(paymentService.totalSumForCurrentUser(from, to, 5L)).thenReturn(
                new TotalAmountResponseDto(from, to, 5L, BigDecimal.valueOf(120.50))
        );

        mockMvc.perform(get("/api/payments/sum/current")
                        .header("X-User-Id", "5")
                        .header("X-User-Role", "USER")
                        .param("from", "2026-03-01T00:00:00Z")
                        .param("to", "2026-03-31T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.totalAmount").value(120.50));

        verify(accessPolicyService).requireUserOrAdmin(any());
    }

    @Test
    void deleteShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/payments/64f0c3d5a3a8435b2dd5d911")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isNoContent());

        verify(paymentService).delete(eq("64f0c3d5a3a8435b2dd5d911"));
    }
}

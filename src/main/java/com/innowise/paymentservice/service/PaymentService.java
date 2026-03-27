package com.innowise.paymentservice.service;

import com.innowise.paymentservice.config.RequestAuthContext;
import com.innowise.paymentservice.model.dto.PaymentRequestDto;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import com.innowise.paymentservice.model.dto.TotalAmountResponseDto;
import com.innowise.paymentservice.model.entity.PaymentStatus;

import java.time.Instant;
import java.util.List;

public interface PaymentService {
    PaymentResponseDto create(PaymentRequestDto requestDto);
    PaymentResponseDto getById(String id);
    PaymentResponseDto getById(String id, RequestAuthContext context);
    List<PaymentResponseDto> getPayments(Long userId, Long orderId, PaymentStatus status);
    List<PaymentResponseDto> getPayments(Long userId, Long orderId, PaymentStatus status, RequestAuthContext context);
    PaymentResponseDto update(String id, PaymentRequestDto requestDto);
    void delete(String id);
    TotalAmountResponseDto totalSumForCurrentUser(Instant from, Instant to, Long userId);
    TotalAmountResponseDto totalSumForAllUsers(Instant from, Instant to);
}

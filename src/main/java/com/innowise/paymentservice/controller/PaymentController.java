package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.model.dto.PaymentRequestDto;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import com.innowise.paymentservice.model.dto.TotalAmountResponseDto;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import com.innowise.paymentservice.service.AccessPolicyService;
import com.innowise.paymentservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final AccessPolicyService accessPolicyService;

    public PaymentController(PaymentService paymentService, AccessPolicyService accessPolicyService) {
        this.paymentService = paymentService;
        this.accessPolicyService = accessPolicyService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody PaymentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.create(request));
    }

    @GetMapping("/{id}")
    public PaymentResponseDto getPaymentById(@PathVariable String id) {
        return paymentService.getById(id);
    }

    @GetMapping
    public List<PaymentResponseDto> getPayments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) PaymentStatus status
    ) {
        return paymentService.getPayments(userId, orderId, status);
    }

    @PutMapping("/{id}")
    public PaymentResponseDto updatePayment(
            @PathVariable String id,
            @Valid @RequestBody PaymentRequestDto request
    ) {
        return paymentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable String id) {
        paymentService.delete(id);
    }

    @GetMapping("/sum/current")
    public TotalAmountResponseDto getCurrentUserTotal(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        accessPolicyService.requireUserOrAdmin(request);
        Long currentUserId = accessPolicyService.requireContext(request).userId();
        return paymentService.totalSumForCurrentUser(from, to, currentUserId);
    }

    @GetMapping("/sum/all")
    public TotalAmountResponseDto getAllUsersTotal(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        accessPolicyService.requireAdmin(request);
        return paymentService.totalSumForAllUsers(from, to);
    }
}

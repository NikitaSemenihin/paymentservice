package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.RandomNumberClient;
import com.innowise.paymentservice.event.PaymentEventPublisher;
import com.innowise.paymentservice.exception.BadRequestException;
import com.innowise.paymentservice.exception.PaymentNotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.dto.PaymentRequestDto;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import com.innowise.paymentservice.model.dto.TotalAmountResponseDto;
import com.innowise.paymentservice.model.entity.Payment;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumberClient randomNumberClient;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            RandomNumberClient randomNumberClient,
            PaymentEventPublisher paymentEventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.randomNumberClient = randomNumberClient;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Override
    public PaymentResponseDto create(PaymentRequestDto requestDto) {
        Payment payment = paymentMapper.toEntity(requestDto);
        payment.setTimestamp(Instant.now());
        payment.setStatus(resolvePaymentStatus());
        PaymentResponseDto response = paymentMapper.toResponse(paymentRepository.save(payment));
        paymentEventPublisher.publishPaymentCreated(response);
        return response;
    }

    @Override
    public PaymentResponseDto getById(String id) {
        return paymentMapper.toResponse(findPaymentById(id));
    }

    @Override
    public List<PaymentResponseDto> getPayments(Long userId, Long orderId, PaymentStatus status) {
        long filtersCount = countPresentFilters(userId, orderId, status);
        if (filtersCount != 1) {
            throw new BadRequestException("Exactly one filter must be provided: userId, orderId or status");
        }

        List<Payment> payments;

        if (userId != null) {
            payments = paymentRepository.findAllByUserId(userId);
        } else if (orderId != null) {
            payments = paymentRepository.findAllByOrderId(orderId);
        } else {
            payments = paymentRepository.findAllByStatus(status);
        }

        return payments.stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    public PaymentResponseDto update(String id, PaymentRequestDto requestDto) {
        Payment existingPayment = findPaymentById(id);
        existingPayment.setOrderId(requestDto.orderId());
        existingPayment.setUserId(requestDto.userId());
        existingPayment.setPaymentAmount(requestDto.paymentAmount());
        return paymentMapper.toResponse(paymentRepository.save(existingPayment));
    }

    @Override
    public void delete(String id) {
        Payment existingPayment = findPaymentById(id);
        paymentRepository.delete(existingPayment);
    }

    @Override
    public TotalAmountResponseDto totalSumForCurrentUser(Instant from, Instant to, Long userId) {
        validateDateRange(from, to);
        return new TotalAmountResponseDto(
                from,
                to,
                userId,
                paymentRepository.totalSumForCurrentUser(from, to, userId)
        );
    }

    @Override
    public TotalAmountResponseDto totalSumForAllUsers(Instant from, Instant to) {
        validateDateRange(from, to);
        return new TotalAmountResponseDto(
                from,
                to,
                null,
                paymentRepository.totalSumForAllUsers(from, to)
        );
    }

    private PaymentStatus resolvePaymentStatus() {
        int randomValue = randomNumberClient.getRandomNumber();
        return randomValue % 2 == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }

    private Payment findPaymentById(String id) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Payment id must be a valid ObjectId");
        }

        return paymentRepository.findById(objectId)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new BadRequestException("Both from and to dates must be provided");
        }
        if (!from.isBefore(to)) {
            throw new BadRequestException("from must be earlier than to");
        }
    }

    private long countPresentFilters(Long userId, Long orderId, PaymentStatus status) {
        long count = 0;
        if (userId != null) {
            count++;
        }
        if (orderId != null) {
            count++;
        }
        if (status != null) {
            count++;
        }
        return count;
    }
}

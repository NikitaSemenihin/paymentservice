package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.RandomNumberClient;
import com.innowise.paymentservice.config.RequestAuthContext;
import com.innowise.paymentservice.exception.BadRequestException;
import com.innowise.paymentservice.exception.ForbiddenException;
import com.innowise.paymentservice.exception.PaymentNotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.dto.PaymentRequestDto;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import com.innowise.paymentservice.model.dto.TotalAmountResponseDto;
import com.innowise.paymentservice.model.entity.Payment;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import com.innowise.paymentservice.outbox.OutboxEventFactory;
import com.innowise.paymentservice.outbox.OutboxEventRepository;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumberClient randomNumberClient;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventFactory outboxEventFactory;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            RandomNumberClient randomNumberClient,
            OutboxEventRepository outboxEventRepository,
            OutboxEventFactory outboxEventFactory
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.randomNumberClient = randomNumberClient;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventFactory = outboxEventFactory;
    }

    @Override
    @Transactional
    public PaymentResponseDto create(PaymentRequestDto requestDto) {
        Payment payment = paymentMapper.toEntity(requestDto);
        payment.setTimestamp(Instant.now());
        payment.setStatus(resolvePaymentStatus());
        Payment savedPayment = paymentRepository.save(payment);
        PaymentResponseDto response = paymentMapper.toResponse(savedPayment);
        outboxEventRepository.save(outboxEventFactory.paymentCreated(response));
        return response;
    }

    @Override
    public PaymentResponseDto getById(String id) {
        return paymentMapper.toResponse(findPaymentById(id));
    }

    @Override
    public PaymentResponseDto getById(String id, RequestAuthContext context) {
        Payment payment = findPaymentById(id);
        ensureCanAccessPayment(payment, context);
        return paymentMapper.toResponse(payment);
    }

    @Override
    public List<PaymentResponseDto> getPayments(Long userId, Long orderId, PaymentStatus status) {
        return filterPayments(resolveAdminPayments(userId, orderId, status), userId, orderId, status);
    }

    @Override
    public List<PaymentResponseDto> getPayments(Long userId, Long orderId, PaymentStatus status, RequestAuthContext context) {
        if (context.isAdmin()) {
            return getPayments(userId, orderId, status);
        }

        if (context.userId() == null) {
            throw new ForbiddenException("Missing requester user id");
        }

        if (userId != null && !context.userId().equals(userId)) {
            throw new ForbiddenException("You can only access your own payments");
        }

        return filterPayments(
                paymentRepository.findAllByUserId(context.userId()),
                context.userId(),
                orderId,
                status
        );
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

    private void ensureCanAccessPayment(Payment payment, RequestAuthContext context) {
        if (context.isAdmin()) {
            return;
        }
        if (context.userId() != null && context.userId().equals(payment.getUserId())) {
            return;
        }
        throw new ForbiddenException("You do not have access to this payment");
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new BadRequestException("Both from and to dates must be provided");
        }
        if (!from.isBefore(to)) {
            throw new BadRequestException("from must be earlier than to");
        }
    }

    private List<Payment> resolveAdminPayments(Long userId, Long orderId, PaymentStatus status) {
        if (userId != null && orderId == null && status == null) {
            return paymentRepository.findAllByUserId(userId);
        }
        if (userId == null && orderId != null && status == null) {
            return paymentRepository.findAllByOrderId(orderId);
        }
        if (userId == null && orderId == null && status != null) {
            return paymentRepository.findAllByStatus(status);
        }
        return paymentRepository.findAll();
    }

    private List<PaymentResponseDto> filterPayments(
            List<Payment> payments,
            Long userId,
            Long orderId,
            PaymentStatus status
    ) {
        return payments.stream()
                .filter(payment -> userId == null || userId.equals(payment.getUserId()))
                .filter(payment -> orderId == null || orderId.equals(payment.getOrderId()))
                .filter(payment -> status == null || status == payment.getStatus())
                .map(paymentMapper::toResponse)
                .toList();
    }
}

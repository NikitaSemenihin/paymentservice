package com.innowise.paymentservice.service;

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
import com.innowise.paymentservice.service.impl.PaymentServiceImpl;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createShouldSetSuccessStatusForEvenRandomValue() {
        PaymentRequestDto request = new PaymentRequestDto(10L, 20L, BigDecimal.valueOf(15.25));
        Payment payment = new Payment();
        Payment savedPayment = new Payment(
                new ObjectId("64f0c3d5a3a8435b2dd5d911"),
                10L,
                20L,
                PaymentStatus.SUCCESS,
                Instant.now(),
                BigDecimal.valueOf(15.25)
        );
        PaymentResponseDto response = new PaymentResponseDto(
                "64f0c3d5a3a8435b2dd5d911",
                10L,
                20L,
                PaymentStatus.SUCCESS,
                savedPayment.getTimestamp(),
                BigDecimal.valueOf(15.25)
        );

        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(randomNumberClient.getRandomNumber()).thenReturn(8);
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(paymentMapper.toResponse(savedPayment)).thenReturn(response);

        PaymentResponseDto actual = paymentService.create(request);

        assertThat(actual.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTimestamp()).isNotNull();
        verify(paymentRepository).save(payment);
        verify(paymentEventPublisher).publishPaymentCreated(response);
    }

    @Test
    void createShouldSetFailedStatusForOddRandomValue() {
        PaymentRequestDto request = new PaymentRequestDto(10L, 20L, BigDecimal.valueOf(15.25));
        Payment payment = new Payment();

        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(randomNumberClient.getRandomNumber()).thenReturn(7);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(new PaymentResponseDto(
                null,
                10L,
                20L,
                PaymentStatus.FAILED,
                Instant.now(),
                BigDecimal.valueOf(15.25)
        ));

        PaymentResponseDto actual = paymentService.create(request);

        assertThat(actual.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentEventPublisher).publishPaymentCreated(actual);
    }

    @Test
    void getPaymentsShouldFailWhenFilterCountIsInvalid() {
        assertThatThrownBy(() -> paymentService.getPayments(1L, 2L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Exactly one filter must be provided");
    }

    @Test
    void getPaymentsShouldReturnPaymentsByStatus() {
        Payment payment = new Payment(
                new ObjectId("64f0c3d5a3a8435b2dd5d911"),
                10L,
                20L,
                PaymentStatus.SUCCESS,
                Instant.now(),
                BigDecimal.ONE
        );
        PaymentResponseDto response = new PaymentResponseDto(
                "64f0c3d5a3a8435b2dd5d911",
                10L,
                20L,
                PaymentStatus.SUCCESS,
                payment.getTimestamp(),
                BigDecimal.ONE
        );

        when(paymentRepository.findAllByStatus(PaymentStatus.SUCCESS)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        List<PaymentResponseDto> actual = paymentService.getPayments(null, null, PaymentStatus.SUCCESS);

        assertThat(actual).containsExactly(response);
    }

    @Test
    void getPaymentsShouldReturnPaymentsByUserId() {
        Payment payment = new Payment(
                new ObjectId("64f0c3d5a3a8435b2dd5d912"),
                15L,
                20L,
                PaymentStatus.SUCCESS,
                Instant.now(),
                BigDecimal.TEN
        );
        PaymentResponseDto response = new PaymentResponseDto(
                "64f0c3d5a3a8435b2dd5d912",
                15L,
                20L,
                PaymentStatus.SUCCESS,
                payment.getTimestamp(),
                BigDecimal.TEN
        );

        when(paymentRepository.findAllByUserId(20L)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        List<PaymentResponseDto> actual = paymentService.getPayments(20L, null, null);

        assertThat(actual).containsExactly(response);
    }

    @Test
    void getPaymentsShouldReturnPaymentsByOrderId() {
        Payment payment = new Payment(
                new ObjectId("64f0c3d5a3a8435b2dd5d913"),
                15L,
                20L,
                PaymentStatus.FAILED,
                Instant.now(),
                BigDecimal.valueOf(11.50)
        );
        PaymentResponseDto response = new PaymentResponseDto(
                "64f0c3d5a3a8435b2dd5d913",
                15L,
                20L,
                PaymentStatus.FAILED,
                payment.getTimestamp(),
                BigDecimal.valueOf(11.50)
        );

        when(paymentRepository.findAllByOrderId(15L)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        List<PaymentResponseDto> actual = paymentService.getPayments(null, 15L, null);

        assertThat(actual).containsExactly(response);
    }

    @Test
    void getByIdShouldReturnMappedResponseForExistingPayment() {
        String paymentId = "64f0c3d5a3a8435b2dd5d914";
        Payment payment = new Payment(
                new ObjectId(paymentId),
                10L,
                20L,
                PaymentStatus.SUCCESS,
                Instant.now(),
                BigDecimal.valueOf(22.40)
        );
        PaymentResponseDto response = new PaymentResponseDto(
                paymentId,
                10L,
                20L,
                PaymentStatus.SUCCESS,
                payment.getTimestamp(),
                BigDecimal.valueOf(22.40)
        );

        when(paymentRepository.findById(new ObjectId(paymentId))).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        PaymentResponseDto actual = paymentService.getById(paymentId);

        assertThat(actual).isEqualTo(response);
    }

    @Test
    void getByIdShouldRejectInvalidObjectId() {
        assertThatThrownBy(() -> paymentService.getById("invalid-id"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Payment id must be a valid ObjectId");
    }

    @Test
    void getByIdShouldThrowWhenPaymentDoesNotExist() {
        String paymentId = "64f0c3d5a3a8435b2dd5d915";

        when(paymentRepository.findById(new ObjectId(paymentId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById(paymentId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining(paymentId);
    }

    @Test
    void deleteShouldRemoveExistingPayment() {
        String paymentId = "64f0c3d5a3a8435b2dd5d911";
        Payment payment = new Payment();
        payment.setId(new ObjectId(paymentId));

        when(paymentRepository.findById(new ObjectId(paymentId))).thenReturn(Optional.of(payment));

        paymentService.delete(paymentId);

        verify(paymentRepository).delete(payment);
    }

    @Test
    void updateShouldRefreshEditablePaymentFieldsOnly() {
        String paymentId = "64f0c3d5a3a8435b2dd5d916";
        Instant initialTimestamp = Instant.parse("2026-03-20T08:00:00Z");
        Payment existingPayment = new Payment(
                new ObjectId(paymentId),
                1L,
                2L,
                PaymentStatus.FAILED,
                initialTimestamp,
                BigDecimal.ONE
        );
        PaymentRequestDto request = new PaymentRequestDto(30L, 40L, BigDecimal.valueOf(77.70));
        Payment savedPayment = new Payment(
                new ObjectId(paymentId),
                30L,
                40L,
                PaymentStatus.FAILED,
                initialTimestamp,
                BigDecimal.valueOf(77.70)
        );
        PaymentResponseDto response = new PaymentResponseDto(
                paymentId,
                30L,
                40L,
                PaymentStatus.FAILED,
                savedPayment.getTimestamp(),
                BigDecimal.valueOf(77.70)
        );

        when(paymentRepository.findById(new ObjectId(paymentId))).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(existingPayment)).thenReturn(savedPayment);
        when(paymentMapper.toResponse(savedPayment)).thenReturn(response);

        PaymentResponseDto actual = paymentService.update(paymentId, request);

        assertThat(actual).isEqualTo(response);
        assertThat(existingPayment.getOrderId()).isEqualTo(30L);
        assertThat(existingPayment.getUserId()).isEqualTo(40L);
        assertThat(existingPayment.getPaymentAmount()).isEqualByComparingTo("77.70");
        assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(existingPayment.getTimestamp()).isEqualTo(initialTimestamp);
    }

    @Test
    void totalSumForCurrentUserShouldReturnRepositoryValue() {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-31T00:00:00Z");

        when(paymentRepository.totalSumForCurrentUser(from, to, 55L)).thenReturn(BigDecimal.valueOf(120.75));

        TotalAmountResponseDto actual = paymentService.totalSumForCurrentUser(from, to, 55L);

        assertThat(actual).isEqualTo(new TotalAmountResponseDto(from, to, 55L, BigDecimal.valueOf(120.75)));
    }

    @Test
    void totalSumForCurrentUserShouldRejectInvalidDateRange() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> paymentService.totalSumForCurrentUser(now, now, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("from must be earlier than to");
    }

    @Test
    void totalSumForAllUsersShouldReturnRepositoryValue() {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-31T00:00:00Z");

        when(paymentRepository.totalSumForAllUsers(from, to)).thenReturn(BigDecimal.valueOf(320.10));

        TotalAmountResponseDto actual = paymentService.totalSumForAllUsers(from, to);

        assertThat(actual).isEqualTo(new TotalAmountResponseDto(from, to, null, BigDecimal.valueOf(320.10)));
    }

    @Test
    void totalSumForAllUsersShouldRejectMissingDateBoundary() {
        Instant to = Instant.parse("2026-03-31T00:00:00Z");

        assertThatThrownBy(() -> paymentService.totalSumForAllUsers(null, to))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Both from and to dates must be provided");
    }
}

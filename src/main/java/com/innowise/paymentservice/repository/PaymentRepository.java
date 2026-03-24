package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.model.entity.Payment;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
public interface PaymentRepository extends MongoRepository<Payment, ObjectId>, PaymentRepositoryCustom {
    List<Payment> findAllByOrderId(Long orderId);
    List<Payment> findAllByUserId(Long userId);
    List<Payment> findAllByStatus(PaymentStatus status);
}

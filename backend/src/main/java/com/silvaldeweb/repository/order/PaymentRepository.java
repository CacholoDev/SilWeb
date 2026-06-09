package com.silvaldeweb.repository.order;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silvaldeweb.model.order.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);
}

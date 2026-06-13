package com.silvaldeweb.repository.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.silvaldeweb.model.order.Order;
import com.silvaldeweb.model.order.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @EntityGraph(attributePaths = {"items", "payment", "shipment"})
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"items", "payment", "shipment"})
    List<Order> findByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"items", "payment", "shipment"})
    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);

    @EntityGraph(attributePaths = {"items", "payment", "shipment"})
    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);
}

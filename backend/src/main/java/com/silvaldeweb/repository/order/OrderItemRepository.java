package com.silvaldeweb.repository.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silvaldeweb.model.order.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
}

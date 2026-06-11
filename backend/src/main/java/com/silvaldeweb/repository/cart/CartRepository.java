package com.silvaldeweb.repository.cart;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silvaldeweb.model.cart.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomerId(Long customerId);
}

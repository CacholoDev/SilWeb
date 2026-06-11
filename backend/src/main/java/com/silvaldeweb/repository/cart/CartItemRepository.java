package com.silvaldeweb.repository.cart;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silvaldeweb.model.cart.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);
}

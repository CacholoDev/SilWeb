package com.silvaldeweb.repository.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silvaldeweb.model.product.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    List<Product> findByActive(boolean active);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByActiveAndCategoryId(boolean active, Long categoryId);
}

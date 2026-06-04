package com.silvaldeweb.repository.category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silvaldeweb.model.category.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Category> findByActive(boolean active);
}

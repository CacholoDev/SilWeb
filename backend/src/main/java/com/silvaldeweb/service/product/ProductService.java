package com.silvaldeweb.service.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.silvaldeweb.dto.product.ProductCreateRequest;
import com.silvaldeweb.dto.product.ProductResponse;
import com.silvaldeweb.dto.product.ProductUpdateRequest;
import com.silvaldeweb.exception.category.CategoryNotFoundException;
import com.silvaldeweb.exception.product.ProductAlreadyExistsException;
import com.silvaldeweb.exception.product.ProductNotFoundException;
import com.silvaldeweb.model.category.Category;
import com.silvaldeweb.model.product.Product;
import com.silvaldeweb.repository.category.CategoryRepository;
import com.silvaldeweb.repository.product.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        String name = request.name().trim();
        String sku = request.sku().trim();

        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ProductAlreadyExistsException(sku);
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

        Product.ProductBuilder builder = Product.builder()
                .name(name)
                .sku(sku)
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .category(category);

        if (request.active() != null) {
            builder.active(request.active());
        }

        Product saved = productRepository.save(builder.build());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(Boolean active, Long categoryId) {
        List<Product> products;
        if (active == null && categoryId == null) {
            products = productRepository.findAll();
        } else if (active != null && categoryId != null) {
            products = productRepository.findByActiveAndCategoryId(active, categoryId);
        } else if (active != null) {
            products = productRepository.findByActive(active);
        } else {
            products = productRepository.findByCategoryId(categoryId);
        }

        return products.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = findById(id);
        String name = request.name().trim();
        String sku = request.sku().trim();

        if (!product.getSku().equalsIgnoreCase(sku) && productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ProductAlreadyExistsException(sku);
        }

        if (!product.getCategory().getId().equals(request.categoryId())) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));
            product.setCategory(category);
        }

        product.setName(name);
        product.setSku(sku);
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        if (request.active() != null) {
            product.setActive(request.active());
        }

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
    }

    private Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getActive(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

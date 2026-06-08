package com.silvaldeweb.service.product;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        log.info("Creating product sku='{}' name='{}'", request.sku(), request.name());
        String name = request.name().trim();
        String sku = request.sku().trim();

        if (productRepository.existsBySkuIgnoreCase(sku)) {
            log.warn("Product sku='{}' already exists, refusing to create", sku);
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
        log.info("Product created id={} sku='{}'", saved.getId(), saved.getSku());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(Boolean active, Long categoryId) {
        log.info("Listing products active={} categoryId={}", active, categoryId);
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

        log.info("Found {} products", products.size());
        return products.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        log.info("Getting product id={}", id);
        return toResponse(findById(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        log.info("Updating product id={} newSku='{}'", id, request.sku());
        Product product = findById(id);
        String name = request.name().trim();
        String sku = request.sku().trim();

        if (!product.getSku().equalsIgnoreCase(sku) && productRepository.existsBySkuIgnoreCase(sku)) {
            log.warn("Product sku='{}' already exists, refusing to update", sku);
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
        log.info("Product updated id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting product id={}", id);
        try {
            Product product = findById(id);
            log.info("Product id={} found, executing delete", id);
            productRepository.delete(product);
            log.info("Product id={} deleted", id);
        } catch (ProductNotFoundException exception) {
            log.warn("Delete failed: product id={} not found", id);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Delete failed for product id={}", id, exception);
            throw exception;
        }
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

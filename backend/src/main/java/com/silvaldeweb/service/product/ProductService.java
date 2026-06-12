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
import com.silvaldeweb.model.audit.Action;
import com.silvaldeweb.model.category.Category;
import com.silvaldeweb.model.product.Product;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.category.CategoryRepository;
import com.silvaldeweb.repository.product.ProductRepository;
import com.silvaldeweb.service.audit.AuditLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ProductResponse create(ProductCreateRequest request, User actor) {
        log.info("Creating product sku='{}' name='{}' actor={}", request.sku(), request.name(),
                actor != null ? actor.getEmail() : "null");
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
        auditLogService.record(actor, Action.CREATE, "Product", saved.getId(),
                "sku=" + saved.getSku() + " name=" + saved.getName() + " price=" + saved.getPrice(), null);
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
    public ProductResponse update(Long id, ProductUpdateRequest request, User actor) {
        log.info("Updating product id={} newSku='{}' actor={}", id, request.sku(),
                actor != null ? actor.getEmail() : "null");
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

        java.math.BigDecimal previousPrice = product.getPrice();
        Integer previousStock = product.getStock();
        Boolean previousActive = product.getActive();

        product.setName(name);
        product.setSku(sku);
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        if (request.active() != null) {
            product.setActive(request.active());
        }

        Product saved = productRepository.save(product);
        StringBuilder diff = new StringBuilder();
        if (previousPrice != null && previousPrice.compareTo(saved.getPrice()) != 0) {
            diff.append("price: ").append(previousPrice).append(" -> ").append(saved.getPrice()).append("; ");
        }
        if (!previousStock.equals(saved.getStock())) {
            diff.append("stock: ").append(previousStock).append(" -> ").append(saved.getStock()).append("; ");
        }
        if (previousActive != null && !previousActive.equals(saved.getActive())) {
            diff.append("active: ").append(previousActive).append(" -> ").append(saved.getActive()).append("; ");
        }
        if (diff.length() > 0) {
            auditLogService.record(actor, Action.UPDATE, "Product", saved.getId(), diff.toString().trim(), null);
        }
        log.info("Product updated id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, User actor) {
        log.info("Deleting product id={} actor={}", id, actor != null ? actor.getEmail() : "null");
        try {
            Product product = findById(id);
            String metadata = "sku=" + product.getSku() + " name=" + product.getName() + " stock=" + product.getStock();
            log.info("Product id={} found, executing delete", id);
            productRepository.delete(product);
            auditLogService.record(actor, Action.DELETE, "Product", id, metadata, null);
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

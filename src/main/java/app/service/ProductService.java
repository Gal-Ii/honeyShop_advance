package app.service;

import app.exception.InvalidProductDataException;
import app.exception.ProductNotFoundException;
import app.exception.ProductAlreadyExistsException;
import app.exception.UnauthorizedActionException;
import app.model.entity.product.Product;
import app.model.entity.user.User;
import app.repository.product.ProductRepository;
import app.web.dto.product.ProductCreateRequest;
import app.web.dto.product.ProductUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(cacheNames = "activeProducts")
    public List<Product> getAllActiveProducts() {

        return productRepository.findAllByIsActiveTrue();
    }

    @Cacheable(cacheNames = "allProducts")
    public List<Product> getAllProducts() {

        return productRepository.findAll();
    }

    @Cacheable(
            cacheNames = "productsById",
            key = "#id"
    )
    public Product getById(UUID id) {

        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "No product with [%s] id.".formatted(id)
                ));
    }

    @CacheEvict(
            cacheNames = {"activeProducts", "allProducts"},
            allEntries = true
    )
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public Product create(ProductCreateRequest request, User currentUser) {
        if (request == null) {
            throw new InvalidProductDataException("Product request is required.");
        }

        if (currentUser == null) {
            throw new UnauthorizedActionException("User must be logged in.");
        }

        String name = request.getName() == null ? null : request.getName().trim();

        if (productRepository.existsByName(name)) {
            throw new ProductAlreadyExistsException("Product with this name already exists.");
        }

        LocalDateTime now = LocalDateTime.now();

        Product product = Product.builder()
                .name(name)
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .items(request.getItems())
                .isActive(Boolean.TRUE.equals(request.getIsActive()))
                .createdOn(now)
                .updatedOn(now)
                .build();

        Product savedProduct = productRepository.save(product);

        log.info(
                "Product created: id={}, name={}",
                savedProduct.getId(),
                savedProduct.getName()
        );

        return savedProduct;
    }

    @Caching(evict = {
            @CacheEvict(
                    cacheNames = {"activeProducts", "allProducts"},
                    allEntries = true
            ),
            @CacheEvict(
                    cacheNames = "productsById",
                    key = "#id"
            )
    })
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public Product update(UUID id, ProductUpdateRequest updateRequest, User currentUser) {

        Product updatedProduct = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product does not exist."));

        if (currentUser == null) {
            throw new UnauthorizedActionException("User must be logged in.");
        }

        String name = updateRequest.getName() == null ? null : updateRequest.getName().trim();

        if (name == null || name.isBlank()) {
            throw new InvalidProductDataException("Product name is required.");
        }

        if (productRepository.existsByNameAndIdNot(name, id)) {
            throw new ProductAlreadyExistsException("Product with this name already exists.");
        }

        LocalDateTime now = LocalDateTime.now();

        updatedProduct.setName(name);
        updatedProduct.setDescription(updateRequest.getDescription());
        updatedProduct.setPrice(updateRequest.getPrice());
        updatedProduct.setImageUrl(updateRequest.getImageUrl());
        updatedProduct.setItems(updateRequest.getItems());
        updatedProduct.setIsActive(Boolean.TRUE.equals(updateRequest.getIsActive()));
        updatedProduct.setUpdatedOn(now);

        Product savedProduct = productRepository.save(updatedProduct);

        log.info(
                "Product updated: id={}, name={}",
                savedProduct.getId(),
                savedProduct.getName()
        );

        return savedProduct;

    }

    @Caching(evict = {
            @CacheEvict(
                    cacheNames = {"activeProducts", "allProducts"},
                    allEntries = true
            ),
            @CacheEvict(
                    cacheNames = "productsById",
                    key = "#id"
            )
    })
    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    public Product delete(UUID id, User currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedActionException("User must be logged in.");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product does not exist."));

        product.setIsActive(false);
        product.setUpdatedOn(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);

        log.info(
                "Product deactivated: id={}, name={}",
                savedProduct.getId(),
                savedProduct.getName()
        );

        return savedProduct;
    }

    @Transactional
    @CacheEvict(
            cacheNames = {
                    "activeProducts",
                    "allProducts",
                    "productsById"
            },
            allEntries = true
    )
    public int deactivateOutOfStockProducts() {

        List<Product> products =
                productRepository
                        .findAllByIsActiveTrueAndItemsLessThanEqual(0);

        if (products.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();

        products.forEach(product -> {
            product.setIsActive(false);
            product.setUpdatedOn(now);
        });

        productRepository.saveAll(products);

        log.info(
                "Automatically deactivated [{}] out-of-stock products.",
                products.size()
        );

        return products.size();
    }
}

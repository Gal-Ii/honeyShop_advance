package app.service;

import app.model.entity.product.Product;
import app.model.entity.user.User;
import app.repository.product.ProductRepository;
import app.web.dto.product.ProductCreateRequest;
import app.web.dto.product.ProductUpdateRequest;
import app.exception.ProductAlreadyExistsException;
import app.exception.UnauthorizedActionException;
import app.exception.InvalidProductDataException;
import app.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void createShouldSaveProductWhenRequestIsValid() {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("  Акациев мед  ")
                .description("Натурален акациев мед.")
                .price(new BigDecimal("15.50"))
                .imageUrl("/images/akatsia.png")
                .items(10)
                .isActive(true)
                .build();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        when(productRepository.existsByName("Акациев мед"))
                .thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.create(request, currentUser);

        assertNotNull(result);
        assertEquals("Акациев мед", result.getName());
        assertEquals(new BigDecimal("15.50"), result.getPrice());
        assertEquals(10, result.getItems());
        assertTrue(result.getIsActive());
        assertNotNull(result.getCreatedOn());
        assertNotNull(result.getUpdatedOn());

        ArgumentCaptor<Product> productCaptor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();

        assertEquals("Акациев мед", savedProduct.getName());
        assertEquals("Натурален акациев мед.", savedProduct.getDescription());

        verify(productRepository).existsByName("Акациев мед");
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void createShouldThrowExceptionWhenProductNameAlreadyExists() {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Acacia honey")
                .description("Natural honey.")
                .price(new BigDecimal("15.50"))
                .imageUrl("/images/akatsia.png")
                .items(10)
                .isActive(true)
                .build();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        when(productRepository.existsByName("Acacia honey"))
                .thenReturn(true);

        ProductAlreadyExistsException exception = assertThrows(
                ProductAlreadyExistsException.class,
                () -> productService.create(request, currentUser)
        );

        assertEquals(
                "Product with this name already exists.",
                exception.getMessage()
        );

        verify(productRepository).existsByName("Acacia honey");
        verify(productRepository, never()).save(any(Product.class));
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void createShouldThrowExceptionWhenCurrentUserIsNull() {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Acacia honey")
                .description("Natural honey.")
                .price(new BigDecimal("15.50"))
                .imageUrl("/images/akatsia.png")
                .items(10)
                .isActive(true)
                .build();

        UnauthorizedActionException exception = assertThrows(
                UnauthorizedActionException.class,
                () -> productService.create(request, null)
        );

        assertEquals(
                "User must be logged in.",
                exception.getMessage()
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void createShouldThrowExceptionWhenRequestIsNull() {
        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        InvalidProductDataException exception = assertThrows(
                InvalidProductDataException.class,
                () -> productService.create(null, currentUser)
        );

        assertEquals(
                "Product request is required.",
                exception.getMessage()
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void getByIdShouldReturnProductWhenProductExists() {
        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .id(productId)
                .name("Acacia honey")
                .price(new BigDecimal("15.50"))
                .items(10)
                .isActive(true)
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        Product result = productService.getById(productId);

        assertNotNull(result);
        assertSame(product, result);
        assertEquals(productId, result.getId());
        assertEquals("Acacia honey", result.getName());

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void getByIdShouldThrowExceptionWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getById(productId)
        );

        assertEquals(
                "No product with [%s] id.".formatted(productId),
                exception.getMessage()
        );

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void updateShouldUpdateAndSaveProductWhenRequestIsValid() {
        UUID productId = UUID.randomUUID();

        Product existingProduct = Product.builder()
                .id(productId)
                .name("Old honey")
                .description("Old description")
                .price(new BigDecimal("10.00"))
                .imageUrl("/images/old.png")
                .items(5)
                .isActive(true)
                .build();

        ProductUpdateRequest request = ProductUpdateRequest.builder()
                .name("  Updated honey  ")
                .description("Updated description")
                .price(new BigDecimal("18.50"))
                .imageUrl("/images/updated.png")
                .items(20)
                .isActive(false)
                .build();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.existsByNameAndIdNot(
                "Updated honey",
                productId
        )).thenReturn(false);

        when(productRepository.save(existingProduct))
                .thenReturn(existingProduct);

        Product result = productService.update(
                productId,
                request,
                currentUser
        );

        assertSame(existingProduct, result);
        assertEquals("Updated honey", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertEquals(new BigDecimal("18.50"), result.getPrice());
        assertEquals("/images/updated.png", result.getImageUrl());
        assertEquals(20, result.getItems());
        assertFalse(result.getIsActive());
        assertNotNull(result.getUpdatedOn());

        verify(productRepository).findById(productId);
        verify(productRepository).existsByNameAndIdNot(
                "Updated honey",
                productId
        );
        verify(productRepository).save(existingProduct);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void updateShouldThrowExceptionWhenProductNameIsBlank() {
        UUID productId = UUID.randomUUID();

        ProductUpdateRequest request = ProductUpdateRequest.builder()
                .name("   ")
                .description("Updated description")
                .price(new BigDecimal("18.50"))
                .items(20)
                .isActive(true)
                .build();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        InvalidProductDataException exception = assertThrows(
                InvalidProductDataException.class,
                () -> productService.update(
                        productId,
                        request,
                        currentUser
                )
        );

        assertEquals(
                "Product name is required.",
                exception.getMessage()
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void updateShouldThrowExceptionWhenProductNameAlreadyExists() {
        UUID productId = UUID.randomUUID();

        Product existingProduct = Product.builder()
                .id(productId)
                .name("Old honey")
                .price(new BigDecimal("10.00"))
                .items(5)
                .isActive(true)
                .build();

        ProductUpdateRequest request = ProductUpdateRequest.builder()
                .name("Existing honey")
                .description("Updated description")
                .price(new BigDecimal("18.50"))
                .items(20)
                .isActive(true)
                .build();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.existsByNameAndIdNot(
                "Existing honey",
                productId
        )).thenReturn(true);

        ProductAlreadyExistsException exception = assertThrows(
                ProductAlreadyExistsException.class,
                () -> productService.update(
                        productId,
                        request,
                        currentUser
                )
        );

        assertEquals(
                "Product with this name already exists.",
                exception.getMessage()
        );

        verify(productRepository).findById(productId);
        verify(productRepository).existsByNameAndIdNot(
                "Existing honey",
                productId
        );
        verify(productRepository, never()).save(any(Product.class));
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void deleteShouldDeactivateAndSaveProduct() {
        UUID productId = UUID.randomUUID();

        Product existingProduct = Product.builder()
                .id(productId)
                .name("Acacia honey")
                .price(new BigDecimal("15.50"))
                .items(10)
                .isActive(true)
                .build();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(existingProduct))
                .thenReturn(existingProduct);

        Product result = productService.delete(
                productId,
                currentUser
        );

        assertSame(existingProduct, result);
        assertFalse(result.getIsActive());
        assertNotNull(result.getUpdatedOn());

        verify(productRepository).findById(productId);
        verify(productRepository).save(existingProduct);
        verify(productRepository, never()).delete(any(Product.class));
        verifyByProductIdWasNotDeleted(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void deleteShouldThrowExceptionWhenCurrentUserIsNull() {
        UUID productId = UUID.randomUUID();

        UnauthorizedActionException exception = assertThrows(
                UnauthorizedActionException.class,
                () -> productService.delete(productId, null)
        );

        assertEquals(
                "User must be logged in.",
                exception.getMessage()
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void deactivateOutOfStockProductsShouldDeactivateAndSaveProducts() {
        Product firstProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("First honey")
                .items(0)
                .isActive(true)
                .build();

        Product secondProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Second honey")
                .items(0)
                .isActive(true)
                .build();

        List<Product> outOfStockProducts = List.of(
                firstProduct,
                secondProduct
        );

        when(productRepository
                .findAllByIsActiveTrueAndItemsLessThanEqual(0))
                .thenReturn(outOfStockProducts);

        int result =
                productService.deactivateOutOfStockProducts();

        assertEquals(2, result);

        assertFalse(firstProduct.getIsActive());
        assertFalse(secondProduct.getIsActive());

        assertNotNull(firstProduct.getUpdatedOn());
        assertNotNull(secondProduct.getUpdatedOn());

        assertEquals(
                firstProduct.getUpdatedOn(),
                secondProduct.getUpdatedOn()
        );

        verify(productRepository)
                .findAllByIsActiveTrueAndItemsLessThanEqual(0);

        verify(productRepository).saveAll(outOfStockProducts);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void deactivateOutOfStockProductsShouldReturnZeroWhenNoProductsExist() {
        when(productRepository
                .findAllByIsActiveTrueAndItemsLessThanEqual(0))
                .thenReturn(List.of());

        int result =
                productService.deactivateOutOfStockProducts();

        assertEquals(0, result);

        verify(productRepository)
                .findAllByIsActiveTrueAndItemsLessThanEqual(0);

        verify(productRepository, never())
                .saveAll(anyList());

        verifyNoMoreInteractions(productRepository);
    }

    private void verifyByProductIdWasNotDeleted(UUID productId) {
        verify(productRepository, never()).deleteById(productId);
    }

    @Test
    void createShouldRejectMissingPrice() {
        ProductCreateRequest request =
                validCreateRequest();

        request.setPrice(null);

        assertInvalidCreateRequest(
                request,
                "Product price is required."
        );
    }

    @Test
    void createShouldRejectNonPositivePrice() {
        ProductCreateRequest request =
                validCreateRequest();

        request.setPrice(BigDecimal.ZERO);

        assertInvalidCreateRequest(
                request,
                "Product price must be positive."
        );
    }

    @Test
    void createShouldRejectMissingQuantity() {
        ProductCreateRequest request =
                validCreateRequest();

        request.setItems(null);

        assertInvalidCreateRequest(
                request,
                "Product quantity is required."
        );
    }

    @Test
    void createShouldRejectNegativeQuantity() {
        ProductCreateRequest request =
                validCreateRequest();

        request.setItems(-1);

        assertInvalidCreateRequest(
                request,
                "Product quantity cannot be negative."
        );
    }

    @Test
    void createShouldRejectMissingActiveStatus() {
        ProductCreateRequest request =
                validCreateRequest();

        request.setIsActive(null);

        assertInvalidCreateRequest(
                request,
                "Product active status is required."
        );
    }

    @Test
    void createShouldRejectTooLongDescription() {
        ProductCreateRequest request =
                validCreateRequest();

        request.setDescription("a".repeat(1001));

        assertInvalidCreateRequest(
                request,
                "Description must be up to 1000 symbols."
        );
    }

    private void assertInvalidCreateRequest(
            ProductCreateRequest request,
            String expectedMessage) {

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        InvalidProductDataException exception =
                assertThrows(
                        InvalidProductDataException.class,
                        () -> productService.create(
                                request,
                                currentUser
                        )
                );

        assertEquals(
                expectedMessage,
                exception.getMessage()
        );

        verifyNoInteractions(productRepository);
    }

    private ProductCreateRequest validCreateRequest() {
        return ProductCreateRequest.builder()
                .name("Acacia honey")
                .description("Natural Bulgarian honey.")
                .price(new BigDecimal("10.00"))
                .imageUrl("/images/acacia.png")
                .items(10)
                .isActive(true)
                .build();
    }

    @Test
    void createShouldRejectPriceWithTooManyDecimals() {
        ProductCreateRequest request =
                validCreateRequest();

        request.setPrice(new BigDecimal("10.123"));

        assertInvalidCreateRequest(
                request,
                "Product price must have up to 8 whole digits and 2 decimal digits."
        );
    }

    @Test
    void createShouldRejectTooLongImagePath() {
        ProductCreateRequest request =
                validCreateRequest();

        request.setImageUrl("a".repeat(501));

        assertInvalidCreateRequest(
                request,
                "Image path must be up to 500 symbols."
        );
    }

    @Test
    void getByIdShouldRejectNullId() {
        InvalidProductDataException exception =
                assertThrows(
                        InvalidProductDataException.class,
                        () -> productService.getById(null)
                );

        assertEquals(
                "Product id is required.",
                exception.getMessage()
        );

        verifyNoInteractions(productRepository);
    }
}

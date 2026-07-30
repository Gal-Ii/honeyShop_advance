package app.service;

import app.exception.InvalidCartDataException;
import app.exception.NotEnoughQuantityException;
import app.exception.UnauthorizedActionException;
import app.model.entity.cartitem.CartItem;
import app.model.entity.product.Product;
import app.model.entity.user.User;
import app.repository.cartitem.CartItemRepository;
import app.repository.product.ProductRepository;
import app.web.dto.cart.CartItemResponse;
import app.web.dto.cart.AddToCartRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(
                cartItemRepository,
                productRepository
        );
    }

    @Test
    void getCartShouldReturnMappedCartItems() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Acacia honey")
                .imageUrl("/images/akatsia.png")
                .price(new BigDecimal("12.50"))
                .items(10)
                .isActive(true)
                .build();

        CartItem cartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(user)
                .product(product)
                .quantity(2)
                .build();

        when(cartItemRepository.findAllByUser(user))
                .thenReturn(List.of(cartItem));

        List<CartItemResponse> result =
                cartService.getCart(user);

        assertEquals(1, result.size());

        CartItemResponse response = result.get(0);

        assertEquals(cartItem.getId(), response.getId());
        assertEquals(product.getId(), response.getProductId());
        assertEquals("Acacia honey", response.getProductName());
        assertEquals(
                "/images/akatsia.png",
                response.getImageUrl()
        );
        assertEquals(
                new BigDecimal("12.50"),
                response.getPrice()
        );
        assertEquals(2, response.getQuantity());
        assertEquals(
                new BigDecimal("25.00"),
                response.getTotalPrice()
        );

        verify(cartItemRepository).findAllByUser(user);
        verifyNoInteractions(productRepository);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void addToCartShouldCreateNewCartItem() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Acacia honey")
                .imageUrl("/images/akatsia.png")
                .price(new BigDecimal("12.50"))
                .items(10)
                .isActive(true)
                .build();

        AddToCartRequest request =
                AddToCartRequest.builder()
                        .productId(product.getId())
                        .quantity(2)
                        .build();

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByUserAndProduct(
                user,
                product
        )).thenReturn(Optional.empty());

        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CartItemResponse result =
                cartService.addToCart(user, request);

        assertEquals(product.getId(), result.getProductId());
        assertEquals("Acacia honey", result.getProductName());
        assertEquals(2, result.getQuantity());
        assertEquals(
                new BigDecimal("25.00"),
                result.getTotalPrice()
        );

        verify(productRepository).findById(product.getId());

        verify(cartItemRepository).findByUserAndProduct(
                user,
                product
        );

        verify(cartItemRepository).save(argThat(cartItem ->
                cartItem.getUser() == user
                        && cartItem.getProduct() == product
                        && cartItem.getQuantity() == 2
                        && cartItem.getCreatedOn() != null
                        && cartItem.getUpdatedOn() != null
        ));

        verifyNoMoreInteractions(
                productRepository,
                cartItemRepository
        );
    }

    @Test
    void addToCartShouldIncreaseQuantityWhenItemAlreadyExists() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Acacia honey")
                .imageUrl("/images/akatsia.png")
                .price(new BigDecimal("12.50"))
                .items(10)
                .isActive(true)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(user)
                .product(product)
                .quantity(3)
                .build();

        AddToCartRequest request =
                AddToCartRequest.builder()
                        .productId(product.getId())
                        .quantity(2)
                        .build();

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByUserAndProduct(
                user,
                product
        )).thenReturn(Optional.of(existingCartItem));

        when(cartItemRepository.save(existingCartItem))
                .thenReturn(existingCartItem);

        CartItemResponse result =
                cartService.addToCart(user, request);

        assertEquals(5, result.getQuantity());
        assertEquals(
                new BigDecimal("62.50"),
                result.getTotalPrice()
        );

        assertEquals(5, existingCartItem.getQuantity());
        assertNotNull(existingCartItem.getUpdatedOn());

        verify(productRepository).findById(product.getId());

        verify(cartItemRepository).findByUserAndProduct(
                user,
                product
        );

        verify(cartItemRepository).save(existingCartItem);

        verifyNoMoreInteractions(
                productRepository,
                cartItemRepository
        );
    }

    @Test
    void addToCartShouldThrowExceptionWhenProductIsInactive() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Inactive honey")
                .price(new BigDecimal("12.50"))
                .items(10)
                .isActive(false)
                .build();

        AddToCartRequest request =
                AddToCartRequest.builder()
                        .productId(product.getId())
                        .quantity(2)
                        .build();

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        InvalidCartDataException exception = assertThrows(
                InvalidCartDataException.class,
                () -> cartService.addToCart(user, request)
        );

        assertEquals(
                "Product is not active",
                exception.getMessage()
        );

        verify(productRepository).findById(product.getId());
        verifyNoInteractions(cartItemRepository);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void addToCartShouldThrowExceptionWhenQuantityExceedsStock() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Acacia honey")
                .price(new BigDecimal("12.50"))
                .items(1)
                .isActive(true)
                .build();

        AddToCartRequest request =
                AddToCartRequest.builder()
                        .productId(product.getId())
                        .quantity(2)
                        .build();

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        NotEnoughQuantityException exception = assertThrows(
                NotEnoughQuantityException.class,
                () -> cartService.addToCart(user, request)
        );

        assertEquals(
                "Not enough product quantity.",
                exception.getMessage()
        );

        verify(productRepository).findById(product.getId());
        verifyNoInteractions(cartItemRepository);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void removeFromCartShouldDeleteOwnedCartItem() {
        UUID userId = UUID.randomUUID();
        UUID cartItemId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .build();

        CartItem cartItem = CartItem.builder()
                .id(cartItemId)
                .user(user)
                .product(product)
                .quantity(2)
                .build();

        when(cartItemRepository.findById(cartItemId))
                .thenReturn(Optional.of(cartItem));

        cartService.removeFromCart(user, cartItemId);

        verify(cartItemRepository).findById(cartItemId);
        verify(cartItemRepository).delete(cartItem);

        verifyNoInteractions(productRepository);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void removeFromCartShouldThrowExceptionForAnotherUsersItem() {
        UUID currentUserId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID cartItemId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(currentUserId)
                .build();

        User owner = User.builder()
                .id(ownerId)
                .build();

        CartItem cartItem = CartItem.builder()
                .id(cartItemId)
                .user(owner)
                .quantity(2)
                .build();

        when(cartItemRepository.findById(cartItemId))
                .thenReturn(Optional.of(cartItem));

        UnauthorizedActionException exception = assertThrows(
                UnauthorizedActionException.class,
                () -> cartService.removeFromCart(
                        currentUser,
                        cartItemId
                )
        );

        assertEquals(
                "Cart item does not belong to current user",
                exception.getMessage()
        );

        verify(cartItemRepository).findById(cartItemId);
        verify(cartItemRepository, never())
                .delete(any(CartItem.class));

        verifyNoInteractions(productRepository);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void clearCartShouldDeleteAllItemsForUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .build();

        cartService.clearCart(user);

        verify(cartItemRepository).deleteAllByUser(user);
        verifyNoInteractions(productRepository);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void clearCartShouldThrowExceptionWhenUserIsNull() {
        UnauthorizedActionException exception = assertThrows(
                UnauthorizedActionException.class,
                () -> cartService.clearCart(null)
        );

        assertEquals(
                "User must be logged in.",
                exception.getMessage()
        );

        verifyNoInteractions(
                cartItemRepository,
                productRepository
        );
    }

    @Test
    void removeExpiredCartItemsShouldDeleteItemsCreatedBeforeExpirationDate() {

        LocalDateTime expirationDate =
                LocalDateTime.now().minusDays(7);

        when(cartItemRepository
                .deleteAllByCreatedOnBefore(expirationDate))
                .thenReturn(3);

        int removedItems =
                cartService.removeExpiredCartItems(expirationDate);

        assertEquals(3, removedItems);

        verify(cartItemRepository)
                .deleteAllByCreatedOnBefore(expirationDate);
    }

    @Test
    void removeExpiredCartItemsShouldRejectNullExpirationDate() {

        assertThrows(
                InvalidCartDataException.class,
                () -> cartService.removeExpiredCartItems(null)
        );

        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void addToCartShouldRejectNullRequest() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .build();

        InvalidCartDataException exception =
                assertThrows(
                        InvalidCartDataException.class,
                        () -> cartService.addToCart(
                                user,
                                null
                        )
                );

        assertEquals(
                "Cart request is required.",
                exception.getMessage()
        );

        verifyNoInteractions(
                cartItemRepository,
                productRepository
        );
    }

    @Test
    void addToCartShouldRejectMissingProductId() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .build();

        AddToCartRequest request =
                new AddToCartRequest();

        request.setQuantity(1);

        InvalidCartDataException exception =
                assertThrows(
                        InvalidCartDataException.class,
                        () -> cartService.addToCart(
                                user,
                                request
                        )
                );

        assertEquals(
                "Product id is required.",
                exception.getMessage()
        );

        verifyNoInteractions(
                cartItemRepository,
                productRepository
        );
    }

    @Test
    void addToCartShouldRejectNullQuantity() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .build();

        AddToCartRequest request =
                new AddToCartRequest();

        request.setProductId(UUID.randomUUID());
        request.setQuantity(null);

        InvalidCartDataException exception =
                assertThrows(
                        InvalidCartDataException.class,
                        () -> cartService.addToCart(
                                user,
                                request
                        )
                );

        assertEquals(
                "Cart quantity is required.",
                exception.getMessage()
        );

        verifyNoInteractions(
                cartItemRepository,
                productRepository
        );
    }

    @Test
    void addToCartShouldRejectNonPositiveQuantity() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .build();

        AddToCartRequest request =
                new AddToCartRequest();

        request.setProductId(UUID.randomUUID());
        request.setQuantity(0);

        InvalidCartDataException exception =
                assertThrows(
                        InvalidCartDataException.class,
                        () -> cartService.addToCart(
                                user,
                                request
                        )
                );

        assertEquals(
                "Cart quantity must be positive.",
                exception.getMessage()
        );

        verifyNoInteractions(
                cartItemRepository,
                productRepository
        );
    }
}

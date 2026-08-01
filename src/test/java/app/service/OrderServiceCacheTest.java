package app.service;

import app.exception.InvalidCartDataException;
import app.model.entity.cartitem.CartItem;
import app.model.entity.order.Order;
import app.model.entity.product.Product;
import app.model.entity.user.User;
import app.repository.cartitem.CartItemRepository;
import app.repository.order.OrderRepository;
import app.repository.orderitem.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(
        OrderServiceCacheTest.CacheTestConfiguration.class
)
class OrderServiceCacheTest {

    private static final String ACTIVE_PRODUCTS =
            "activeProducts";

    private static final String ALL_PRODUCTS =
            "allProducts";

    private static final String PRODUCTS_BY_ID =
            "productsById";

    private static final String ACTIVE_CACHE_KEY =
            "active-key";

    private static final String ALL_CACHE_KEY =
            "all-key";

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(
                cartItemRepository,
                orderRepository,
                orderItemRepository
        );

        cache(ACTIVE_PRODUCTS).clear();
        cache(ALL_PRODUCTS).clear();
        cache(PRODUCTS_BY_ID).clear();
    }

    @Test
    void createOrderShouldEvictProductCachesAfterSuccess() {
        UUID productId = UUID.randomUUID();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();

        Product product = Product.builder()
                .id(productId)
                .name("Acacia honey")
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

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        populateProductCaches(productId);

        orderService.createOrder(user);

        assertNull(
                cache(ACTIVE_PRODUCTS)
                        .get(ACTIVE_CACHE_KEY)
        );

        assertNull(
                cache(ALL_PRODUCTS)
                        .get(ALL_CACHE_KEY)
        );

        assertNull(
                cache(PRODUCTS_BY_ID)
                        .get(productId)
        );
    }

    @Test
    void createOrderShouldKeepProductCachesWhenOrderFails() {
        UUID productId = UUID.randomUUID();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();

        when(cartItemRepository.findAllByUser(user))
                .thenReturn(List.of());

        populateProductCaches(productId);

        assertThrows(
                InvalidCartDataException.class,
                () -> orderService.createOrder(user)
        );

        assertNotNull(
                cache(ACTIVE_PRODUCTS)
                        .get(ACTIVE_CACHE_KEY)
        );

        assertNotNull(
                cache(ALL_PRODUCTS)
                        .get(ALL_CACHE_KEY)
        );

        assertNotNull(
                cache(PRODUCTS_BY_ID)
                        .get(productId)
        );
    }

    private void populateProductCaches(UUID productId) {
        cache(ACTIVE_PRODUCTS).put(
                ACTIVE_CACHE_KEY,
                "active-products-value"
        );

        cache(ALL_PRODUCTS).put(
                ALL_CACHE_KEY,
                "all-products-value"
        );

        cache(PRODUCTS_BY_ID).put(
                productId,
                "product-value"
        );
    }

    private Cache cache(String cacheName) {
        return requireNonNull(
                cacheManager.getCache(cacheName)
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CacheTestConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    ACTIVE_PRODUCTS,
                    ALL_PRODUCTS,
                    PRODUCTS_BY_ID
            );
        }

        @Bean
        CartItemRepository cartItemRepository() {
            return mock(CartItemRepository.class);
        }

        @Bean
        OrderRepository orderRepository() {
            return mock(OrderRepository.class);
        }

        @Bean
        OrderItemRepository orderItemRepository() {
            return mock(OrderItemRepository.class);
        }

        @Bean
        OrderService orderService(
                CartItemRepository cartItemRepository,
                OrderRepository orderRepository,
                OrderItemRepository orderItemRepository) {

            return new OrderService(
                    cartItemRepository,
                    orderRepository,
                    orderItemRepository
            );
        }
    }
}

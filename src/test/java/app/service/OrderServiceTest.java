package app.service;

import app.exception.InvalidOrderDataException;
import app.exception.UnauthorizedActionException;
import app.exception.InvalidCartDataException;
import app.exception.NotEnoughQuantityException;
import app.model.entity.orderitem.OrderItem;
import app.repository.cartitem.CartItemRepository;
import app.repository.order.OrderRepository;
import app.repository.orderitem.OrderItemRepository;
import app.model.entity.user.User;
import app.model.entity.cartitem.CartItem;
import app.model.entity.order.Order;
import app.model.entity.order.OrderStatus;
import app.model.entity.product.Product;
import app.web.dto.order.OrderResponse;
import app.web.dto.order.UpdateOrderStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                cartItemRepository,
                orderRepository,
                orderItemRepository
        );
    }

    @Test
    void createOrderShouldThrowExceptionWhenUserIsNull() {
        UnauthorizedActionException exception = assertThrows(
                UnauthorizedActionException.class,
                () -> orderService.createOrder(null)
        );

        assertEquals(
                "No logged user",
                exception.getMessage()
        );

        verifyNoInteractions(
                cartItemRepository,
                orderRepository,
                orderItemRepository
        );
    }

    @Test
    void createOrderShouldThrowExceptionWhenCartIsEmpty() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();

        when(cartItemRepository.findAllByUser(user))
                .thenReturn(List.of());

        InvalidCartDataException exception = assertThrows(
                InvalidCartDataException.class,
                () -> orderService.createOrder(user)
        );

        assertEquals(
                "Cart is empty",
                exception.getMessage()
        );

        verify(cartItemRepository).findAllByUser(user);
        verifyNoInteractions(
                orderRepository,
                orderItemRepository
        );
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void createOrderShouldCreateOrderAndClearCart() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
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
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(orderId);
                    return order;
                });

        Order result = orderService.createOrder(user);

        assertNotNull(result);
        assertEquals(orderId, result.getId());
        assertSame(user, result.getUser());
        assertEquals(
                new BigDecimal("25.00"),
                result.getTotalPrice()
        );
        assertEquals(OrderStatus.NEW, result.getStatus());
        assertNotNull(result.getCreatedOn());
        assertNotNull(result.getUpdatedOn());

        assertEquals(8, product.getItems());

        verify(cartItemRepository).findAllByUser(user);

        verify(orderRepository).save(argThat(order ->
                order.getUser() == user
                        && order.getTotalPrice()
                        .compareTo(new BigDecimal("25.00")) == 0
                        && order.getStatus() == OrderStatus.NEW
        ));

        verify(orderItemRepository).saveAll(argThat(items -> {
            var iterator = items.iterator();

            if (!iterator.hasNext()) {
                return false;
            }

            var item = iterator.next();

            return !iterator.hasNext()
                    && item.getOrder() == result
                    && item.getProduct() == product
                    && item.getProductName().equals("Acacia honey")
                    && item.getQuantity() == 2
                    && item.getUnitPrice()
                    .compareTo(new BigDecimal("12.50")) == 0
                    && item.getTotalPrice()
                    .compareTo(new BigDecimal("25.00")) == 0;
        }));

        verify(cartItemRepository).deleteAllByUser(user);

        verifyNoMoreInteractions(
                cartItemRepository,
                orderRepository,
                orderItemRepository
        );
    }

    @Test
    void createOrderShouldThrowExceptionWhenQuantityIsInsufficient() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Acacia honey")
                .price(new BigDecimal("12.50"))
                .items(1)
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

        NotEnoughQuantityException exception = assertThrows(
                NotEnoughQuantityException.class,
                () -> orderService.createOrder(user)
        );

        assertEquals(
                "Not enough product quantity: Acacia honey",
                exception.getMessage()
        );

        assertEquals(1, product.getItems());

        verify(cartItemRepository).findAllByUser(user);

        verifyNoInteractions(
                orderRepository,
                orderItemRepository
        );

        verify(cartItemRepository, never())
                .deleteAllByUser(user);

        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void getMyOrdersShouldReturnMappedOrderResponses() {
        UUID orderId = UUID.randomUUID();
        LocalDateTime createdOn = LocalDateTime.now();

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Ivan Ivanov")
                .email("ivan@example.com")
                .build();

        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .totalPrice(new BigDecimal("25.00"))
                .status(OrderStatus.NEW)
                .createdOn(createdOn)
                .updatedOn(createdOn)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .productName("Acacia honey")
                .quantity(2)
                .unitPrice(new BigDecimal("12.50"))
                .totalPrice(new BigDecimal("25.00"))
                .build();

        when(orderRepository
                .findAllByUserOrderByCreatedOnDesc(user))
                .thenReturn(List.of(order));

        when(orderItemRepository.findAllByOrder(order))
                .thenReturn(List.of(orderItem));

        List<OrderResponse> result =
                orderService.getMyOrders(user);

        assertEquals(1, result.size());

        OrderResponse response = result.get(0);

        assertEquals(orderId, response.getId());
        assertEquals("Ivan Ivanov", response.getCustomerName());
        assertEquals(
                "ivan@example.com",
                response.getCustomerEmail()
        );
        assertEquals(
                new BigDecimal("25.00"),
                response.getTotalPrice()
        );
        assertEquals(OrderStatus.NEW, response.getStatus());
        assertEquals(createdOn, response.getCreatedOn());
        assertEquals(1, response.getItems().size());

        assertEquals(
                "Acacia honey",
                response.getItems().get(0).getProductName()
        );
        assertEquals(
                2,
                response.getItems().get(0).getQuantity()
        );
        assertEquals(
                new BigDecimal("12.50"),
                response.getItems().get(0).getUnitPrice()
        );
        assertEquals(
                new BigDecimal("25.00"),
                response.getItems().get(0).getTotalPrice()
        );

        verify(orderRepository)
                .findAllByUserOrderByCreatedOnDesc(user);
        verify(orderItemRepository).findAllByOrder(order);

        verifyNoInteractions(cartItemRepository);
        verifyNoMoreInteractions(
                orderRepository,
                orderItemRepository
        );
    }

    @Test
    void updateStatusShouldUpdateOrderAndReturnResponse() {
        UUID orderId = UUID.randomUUID();
        LocalDateTime createdOn = LocalDateTime.now();

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Ivan Ivanov")
                .email("ivan@example.com")
                .build();

        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .totalPrice(new BigDecimal("25.00"))
                .status(OrderStatus.NEW)
                .createdOn(createdOn)
                .updatedOn(createdOn)
                .build();

        UpdateOrderStatusRequest request =
                UpdateOrderStatusRequest.builder()
                        .status(OrderStatus.SENT)
                        .build();

        when(orderRepository.findById(orderId))
                .thenReturn(java.util.Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        when(orderItemRepository.findAllByOrder(order))
                .thenReturn(List.of());

        OrderResponse result =
                orderService.updateStatus(orderId, request);

        assertEquals(orderId, result.getId());
        assertEquals(OrderStatus.SENT, result.getStatus());
        assertEquals("Ivan Ivanov", result.getCustomerName());
        assertEquals(
                "ivan@example.com",
                result.getCustomerEmail()
        );
        assertEquals(
                new BigDecimal("25.00"),
                result.getTotalPrice()
        );
        assertTrue(result.getItems().isEmpty());

        assertEquals(OrderStatus.SENT, order.getStatus());
        assertNotNull(order.getUpdatedOn());

        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(order);
        verify(orderItemRepository).findAllByOrder(order);

        verifyNoInteractions(cartItemRepository);
        verifyNoMoreInteractions(
                orderRepository,
                orderItemRepository
        );
    }

    @Test
    void updateStatusShouldThrowExceptionWhenOrderIdIsNull() {
        UpdateOrderStatusRequest request =
                UpdateOrderStatusRequest.builder()
                        .status(OrderStatus.SENT)
                        .build();

        InvalidOrderDataException exception = assertThrows(
                InvalidOrderDataException.class,
                () -> orderService.updateStatus(null, request)
        );

        assertEquals(
                "Order id is required",
                exception.getMessage()
        );

        verifyNoInteractions(
                cartItemRepository,
                orderRepository,
                orderItemRepository
        );
    }

    @Test
    void updateStatusShouldThrowExceptionWhenStatusIsNull() {
        UUID orderId = UUID.randomUUID();

        UpdateOrderStatusRequest request =
                UpdateOrderStatusRequest.builder()
                        .status(null)
                        .build();

        InvalidOrderDataException exception = assertThrows(
                InvalidOrderDataException.class,
                () -> orderService.updateStatus(orderId, request)
        );

        assertEquals(
                "Order status is required",
                exception.getMessage()
        );

        verifyNoInteractions(
                cartItemRepository,
                orderRepository,
                orderItemRepository
        );
    }
}

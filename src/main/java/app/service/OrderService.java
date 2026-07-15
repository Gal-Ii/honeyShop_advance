package app.service;

import app.exception.*;
import app.model.entity.cartitem.CartItem;
import app.model.entity.order.Order;
import app.model.entity.order.OrderStatus;
import app.model.entity.orderitem.OrderItem;
import app.model.entity.product.Product;
import app.model.entity.user.User;
import app.repository.cartitem.CartItemRepository;
import app.repository.order.OrderRepository;
import app.repository.orderitem.OrderItemRepository;
import app.web.dto.order.OrderItemResponse;
import app.web.dto.order.OrderResponse;
import app.web.dto.order.UpdateOrderStatusRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(CartItemRepository cartItemRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public Order createOrder(User user) {
        if (user == null) {
            throw new UnauthorizedActionException("No logged user");
        }

        List<CartItem> cartItems = cartItemRepository.findAllByUser(user);

        if (cartItems.isEmpty()) {
            throw new InvalidCartDataException("Cart is empty");
        }

        LocalDateTime now = LocalDateTime.now();

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if(cartItem.getQuantity()>product.getItems()){
                throw new NotEnoughQuantityException("Not enough product quantity: " + product.getName());
            }

            BigDecimal price = cartItem.getProduct().getPrice();
            BigDecimal quantity = BigDecimal.valueOf(cartItem.getQuantity());
            BigDecimal itemTotal = price.multiply(quantity);

            totalPrice = totalPrice.add(itemTotal);
        }

        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(OrderStatus.NEW)
                .createdOn(now)
                .updatedOn(now)
                .build();


        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            product.setItems(product.getItems() - cartItem.getQuantity());

            BigDecimal itemTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .productName(product.getName())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(itemTotal)
                    .build();

            orderItems.add(orderItem);
        }

        orderItemRepository.saveAll(orderItems);
        cartItemRepository.deleteAllByUser(user);
        log.info(
                "Order created: orderId={}, userId={}, totalPrice={}, itemCount={}",
                savedOrder.getId(),
                user.getId(),
                savedOrder.getTotalPrice(),
                orderItems.size()
        );
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(User user) {
        if (user == null) {
            throw new UnauthorizedActionException("No logged user");
        }

        return orderRepository.findAllByUserOrderByCreatedOnDesc(user)
                .stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> items = orderItemRepository.findAllByOrder(order)
                .stream()
                .map(this::mapToOrderItemResponse)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getUser().getName())
                .customerEmail(order.getUser().getEmail())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .createdOn(order.getCreatedOn())
                .items(items)
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .productName(orderItem.getProductName())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getTotalPrice())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedOnDesc()
                .stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, UpdateOrderStatusRequest request) {
        if (orderId == null) {
            throw new InvalidOrderDataException("Order id is required");
        }

        if (request == null || request.getStatus() == null) {
            throw new InvalidOrderDataException("Order status is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidOrderDataException("Order does not exist"));

        OrderStatus oldStatus = order.getStatus();

        order.setStatus(request.getStatus());
        order.setUpdatedOn(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        log.info(
                "Order status updated: orderId={}, oldStatus={}, newStatus={}",
                savedOrder.getId(),
                oldStatus,
                savedOrder.getStatus()
        );

        return mapToOrderResponse(savedOrder);
    }
}

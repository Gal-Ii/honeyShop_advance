package app.model.entity.order;


import app.model.entity.orderitem.OrderItem;
import app.model.entity.user.User;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Order customer is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Order total price is required.")
    @PositiveOrZero(message = "Order total price cannot be negative.")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @NotNull(message = "Order status is required.")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @NotNull(message = "Creation date is required.")
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @NotNull(message = "Update date is required.")
    @Column(nullable = false)
    private LocalDateTime updatedOn;

    @Valid
    @NotNull(message = "Order items collection is required.")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order")
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
}

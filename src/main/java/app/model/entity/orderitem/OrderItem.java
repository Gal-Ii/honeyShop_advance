package app.model.entity.orderitem;

import app.model.entity.order.Order;
import app.model.entity.product.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Order is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Product is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank(message = "Product name is required.")
    @Size(max = 100,
            message = "Product name must be up to 100 symbols.")
    @Column(nullable = false, length = 100)
    private String productName;

    @NotNull(message = "Order item quantity is required.")
    @Positive(message = "Order item quantity must be positive.")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Unit price is required.")
    @Positive(message = "Unit price must be positive.")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @NotNull(message = "Item total price is required.")
    @PositiveOrZero(message = "Item total price cannot be negative.")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

}

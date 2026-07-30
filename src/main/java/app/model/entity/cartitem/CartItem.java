package app.model.entity.cartitem;

import app.model.entity.product.Product;
import app.model.entity.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Cart owner is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Cart product is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Cart quantity is required.")
    @Positive(message = "Cart quantity must be positive.")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Creation date is required.")
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @NotNull(message = "Update date is required.")
    @Column(nullable = false)
    private LocalDateTime updatedOn;

}

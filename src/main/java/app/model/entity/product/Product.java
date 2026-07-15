package app.model.entity.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Product name is required.")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 symbols.")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 1000, message = "Description must be up to 1000 symbols.")
    @Column(length = 1000)
    private String description;


    @NotNull(message = "Product price is required.")
    @Positive(message = "Product price must be positive.")
    @Digits(integer = 8, fraction = 2, message = "Product price must have up to 8 whole digits and 2 decimal digits.")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;


    @Size(max = 500, message = "Image path must be up to 500 symbols.")
    @Column(length = 500)
    private String imageUrl;

    @NotNull(message = "Product quantity is required.")
    @PositiveOrZero(message = "Product quantity cannot be negative.")
    @Column(nullable = false)
    private Integer items;

    @NotNull(message = "Product active status is required.")
    @Column(nullable = false)
    private Boolean isActive;

    @NotNull(message = "Creation date is required.")
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @NotNull(message = "Update date is required.")
    @Column(nullable = false)
    private LocalDateTime updatedOn;
}

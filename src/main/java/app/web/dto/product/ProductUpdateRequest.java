package app.web.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductUpdateRequest {
    @NotBlank(message = "Product name is required.")
    @Size(min=2, max=100, message = "Product name must be between 2 and 100 symbols.")
    private String name;

    @Size(max=1000, message = "Description must be up to 1000 symbols.")
    private String description;

    @NotNull(message = "Product price is required.")
    @Positive(message = "Product price must be positive.")
    @Digits(integer = 8, fraction = 2, message = "Product price must have up to 8 whole digits and 2 decimal digits.")
    private BigDecimal price;

    @Size(max = 500, message = "Image path must be up to 500 symbols.")
    private String imageUrl;

    @NotNull(message = "Product quantity is required.")
    @PositiveOrZero(message = "Product quantity cannot be negative.")
    private Integer items;

    @NotNull(message = "Product active status is required.")
    private Boolean isActive;
}

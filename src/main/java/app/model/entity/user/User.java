package app.model.entity.user;

import app.model.entity.cartitem.CartItem;
import app.model.entity.order.Order;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "User name is required.")
    @Size(min = 3, max = 50,
            message = "User name must be between 3 and 50 symbols.")
    @Column(nullable = false, length = 50)
    private String name;

    @NotNull(message = "Gender is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Size(max = 500,
            message = "Profile picture URL must be up to 500 symbols.")
    @Column(length = 500)
    private String profilePicture;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 100,
            message = "Email must be up to 100 symbols.")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(max = 100,
            message = "Encoded password must be up to 100 symbols.")
    @Column(nullable = false, length = 100)
    private String password;

    @NotNull(message = "User role is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @NotNull(message = "Country is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Country country;

    @NotNull(message = "User active status is required.")
    @Column(nullable = false)
    private Boolean isActive;

    @NotNull(message = "Creation date is required.")
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @NotNull(message = "Update date is required.")
    @Column(nullable = false)
    private LocalDateTime updatedOn;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @OrderBy("createdOn DESC")
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @OrderBy("createdOn DESC")
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();
}

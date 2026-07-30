package app.model.entity.user;

import app.model.entity.cartitem.CartItem;
import app.model.entity.order.Order;
import jakarta.persistence.*;
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

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String profilePicture;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private Country country;

    private Boolean isActive;

    @Column(nullable = false)
    private LocalDateTime createdOn;

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

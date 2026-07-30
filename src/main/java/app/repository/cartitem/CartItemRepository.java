package app.repository.cartitem;

import app.model.entity.cartitem.CartItem;
import app.model.entity.product.Product;
import app.model.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findAllByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    void deleteAllByUser(User user);

    int deleteAllByCreatedOnBefore(LocalDateTime expirationDate);
}

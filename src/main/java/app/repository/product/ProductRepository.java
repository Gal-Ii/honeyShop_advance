package app.repository.product;

import app.model.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByIsActiveTrue();

    List<Product> findAllByIsActiveTrueAndItemsLessThanEqual(
            Integer items
    );

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

}

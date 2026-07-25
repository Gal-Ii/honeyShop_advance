package app.repository.user;

import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findUserByEmail(String email);

    boolean existsUserByEmail(String email);

    User getUsersByRole(UserRole role);

    boolean existsByEmailAndIdNot(String email, UUID id);
}

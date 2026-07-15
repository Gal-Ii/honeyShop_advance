package app.web.dto.user;

import app.model.entity.user.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotNull(message = "Моля, изберете роля.")
    private UserRole role;
}

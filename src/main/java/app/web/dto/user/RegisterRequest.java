package app.web.dto.user;

import app.model.entity.user.Country;
import app.model.entity.user.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Please write your first and last name.")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 symbols.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 100, message = "Email must be up to 100 symbols.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(
            min = 6,
            max = 100,
            message = "Password must be between 6 and 100 symbols."
    )
    private String password;

    @NotNull(message = "Country is required.")
    private Country country;

    @NotNull(message = "Gender is required.")
    private Gender gender;
}
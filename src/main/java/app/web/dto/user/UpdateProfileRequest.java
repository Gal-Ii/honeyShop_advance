package app.web.dto.user;

import app.model.entity.user.Country;
import app.model.entity.user.Gender;
import app.model.entity.user.User;
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
public class UpdateProfileRequest {
    @NotBlank(message = "Please write your first and last name.")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 symbols.")
    private String name;

    @NotBlank
    @Email(message = "Please enter a valid email address.")
    private String email;

    @NotNull(message = "Моля, изберете държава.")
    private Country country;

    @NotNull(message = "Моля, изберете пол.")
    private Gender gender;

    @Size(max = 500, message = "Адресът на снимката трябва да бъде до 500 символа.")
    private String profilePicture;

    public static UpdateProfileRequest from(User user) {
        return UpdateProfileRequest.builder()
                .name(user.getName())
                .email(user.getEmail())
                .country(user.getCountry())
                .gender(user.getGender())
                .profilePicture(user.getProfilePicture())
                .build();
    }
}

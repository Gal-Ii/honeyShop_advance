package app.model.entity.user;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "users")
public class UserProperties {
    private DefaultUser defaultUser;

    @Data
    public static class DefaultUser{
        private String name;
        private String password;
        private String email;
        private Country country;
    }
}

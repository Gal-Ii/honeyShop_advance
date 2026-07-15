package app.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Bean
    public SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(matchers -> matchers
                            .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/",
                                    "/register",
                                    "/index",
                                    "/products",
                                    "/login",
                                    "/logout").permitAll()
                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/register"
                            ).permitAll()
                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/products"
                            ).hasAuthority("PRODUCT_CREATE")
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/product-create"
                            ).hasAuthority("PRODUCT_CREATE")
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/products/*/update"
                            ).hasAuthority("PRODUCT_UPDATE")
                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/products/*/update"
                            ).hasAuthority("PRODUCT_UPDATE")
                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/products/*/delete"
                            ).hasAuthority("PRODUCT_DELETE")
                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/orders/*/status"
                            ).hasAuthority("ORDER_STATUS_UPDATE")
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/admin"
                            ).hasAuthority("ORDER_STATUS_UPDATE")
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/admin-products"
                            ).hasAnyAuthority(
                                    "PRODUCT_CREATE",
                                    "PRODUCT_UPDATE",
                                    "PRODUCT_DELETE"
                            )
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/admin-users"
                            ).hasAuthority("USER_VIEW")

                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/admin/users/*/activate"
                            ).hasAuthority("USER_ACTIVATE")

                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/admin/users/*/deactivate"
                            ).hasAuthority("USER_DEACTIVATE")
                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/admin/users/*/role"
                            ).hasAuthority("USER_ROLE_UPDATE")
                            .anyRequest().authenticated())
                    .formLogin(form -> form
                            .loginPage("/login")
                            .loginProcessingUrl("/login")
                            .usernameParameter("email")
                            .passwordParameter("password")
                            .defaultSuccessUrl("/profile", true)
                            .failureUrl("/login?error=true")
                            .permitAll())
                    .logout(logout -> logout
                            .logoutUrl("/perform-logout")
                            .logoutSuccessUrl("/logout")
                            .invalidateHttpSession(true)
                            .clearAuthentication(true)
                            .deleteCookies("JSESSIONID")
                            .permitAll()
                    );
            return http.build();
    }
}
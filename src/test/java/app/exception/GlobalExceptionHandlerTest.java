package app.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(
                new ExceptionThrowingController()
        )
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void userNotFoundShouldReturn404() throws Exception {
        mockMvc.perform(get("/test/user-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "User not found."
                ));
    }

    @Test
    void reviewNotFoundShouldReturn404() throws Exception {
        mockMvc.perform(get("/test/review-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "Review not found."
                ));
    }

    @Test
    void invalidOperationShouldReturn400() throws Exception {
        mockMvc.perform(get("/test/invalid-operation"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "Invalid product data."
                ));
    }

    @Test
    void duplicateShouldReturn409() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/409"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "Product already exists."
                ));
    }

    @Test
    void quantityConflictShouldReturn409() throws Exception {
        mockMvc.perform(get("/test/quantity-conflict"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/409"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "Not enough quantity."
                ));
    }

    @Test
    void unavailableReviewServiceShouldReturn503()
            throws Exception {

        mockMvc.perform(get("/test/review-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error/503"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "Review service is unavailable."
                ));
    }

    @Test
    void invalidReviewDataShouldReturn400() throws Exception {
        mockMvc.perform(get("/test/invalid-review-data"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "Invalid review data."
                ));
    }

    @Controller
    static class ExceptionThrowingController {

        @GetMapping("/test/user-not-found")
        String userNotFound() {
            throw new UserNotFoundException(
                    "User not found."
            );
        }

        @GetMapping("/test/review-not-found")
        String reviewNotFound() {
            throw new ReviewNotFoundException(
                    "Review not found.",
                    new RuntimeException()
            );
        }

        @GetMapping("/test/invalid-operation")
        String invalidOperation() {
            throw new InvalidProductDataException(
                    "Invalid product data."
            );
        }

        @GetMapping("/test/duplicate")
        String duplicate() {
            throw new ProductAlreadyExistsException(
                    "Product already exists."
            );
        }

        @GetMapping("/test/quantity-conflict")
        String quantityConflict() {
            throw new NotEnoughQuantityException(
                    "Not enough quantity."
            );
        }

        @GetMapping("/test/review-unavailable")
        String reviewUnavailable() {
            throw new ReviewServiceUnavailableException(
                    "Review service is unavailable.",
                    new RuntimeException()
            );
        }

        @GetMapping("/test/invalid-review-data")
        String invalidReviewData() {
            throw new InvalidReviewDataException(
                    "Invalid review data."
            );
        }
    }
}

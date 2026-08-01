package app.service;

import app.client.ReviewFeignClient;
import app.model.entity.user.User;
import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.ReviewResponse;
import app.exception.ReviewAlreadyExistsException;
import app.exception.ReviewNotFoundException;
import app.exception.ReviewServiceUnavailableException;
import app.exception.UnauthorizedReviewOperationException;
import app.exception.InvalidReviewDataException;
import app.exception.ProductNotFoundException;
import app.web.dto.review.UpdateReviewRequest;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewFeignClient reviewFeignClient;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                reviewFeignClient,
                userService,
                productService
        );
    }

    @Test
    void createReviewShouldUseCurrentUserData() {
        UUID productId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(currentUserId)
                .name("Honey Customer")
                .build();

        CreateReviewRequest formRequest =
                CreateReviewRequest.builder()
                        .userId(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .authorName("Incorrect author")
                        .rating(5)
                        .comment(" Excellent natural honey. ")
                        .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .userId(currentUserId)
                .build();

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        when(reviewFeignClient.createReview(any()))
                .thenReturn(response);

        reviewService.createReview(
                productId,
                formRequest
        );

        ArgumentCaptor<CreateReviewRequest> captor =
                ArgumentCaptor.forClass(
                        CreateReviewRequest.class
                );

        verify(reviewFeignClient)
                .createReview(captor.capture());

        verify(productService).getById(productId);

        CreateReviewRequest sentRequest =
                captor.getValue();

        assertEquals(productId, sentRequest.getProductId());
        assertEquals(currentUserId, sentRequest.getUserId());
        assertEquals(
                currentUser.getName(),
                sentRequest.getAuthorName()
        );
        assertEquals(5, sentRequest.getRating());
        assertEquals(
                "Excellent natural honey.",
                sentRequest.getComment()
        );
    }

    @Test
    void createReviewShouldTranslateConflictException() {
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(userId)
                .name("Honey Customer")
                .build();

        CreateReviewRequest request =
                CreateReviewRequest.builder()
                        .rating(5)
                        .comment("Excellent natural honey.")
                        .build();

        FeignException.Conflict conflictException =
                mock(FeignException.Conflict.class);

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        when(reviewFeignClient.createReview(
                any(CreateReviewRequest.class)
        )).thenThrow(conflictException);

        ReviewAlreadyExistsException result =
                assertThrows(
                        ReviewAlreadyExistsException.class,
                        () -> reviewService.createReview(
                                productId,
                                request
                        )
                );

        assertEquals(
                "You have already reviewed this product.",
                result.getMessage()
        );

        assertEquals(
                conflictException,
                result.getCause()
        );
    }

    @Test
    void updateReviewShouldTranslateForbiddenException() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(userId)
                .build();

        UpdateReviewRequest request =
                UpdateReviewRequest.builder()
                        .rating(4)
                        .comment("Updated review comment.")
                        .build();

        FeignException.Forbidden forbiddenException =
                mock(FeignException.Forbidden.class);

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        when(reviewFeignClient.updateReview(
                eq(reviewId),
                any(UpdateReviewRequest.class)
        )).thenThrow(forbiddenException);

        UnauthorizedReviewOperationException result =
                assertThrows(
                        UnauthorizedReviewOperationException.class,
                        () -> reviewService.updateReview(
                                reviewId,
                                request
                        )
                );

        assertEquals(
                "You cannot update another user's review.",
                result.getMessage()
        );

        assertEquals(
                forbiddenException,
                result.getCause()
        );
    }

    @Test
    void deleteReviewShouldTranslateForbiddenException() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(userId)
                .build();

        FeignException.Forbidden forbiddenException =
                mock(FeignException.Forbidden.class);

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        doThrow(forbiddenException)
                .when(reviewFeignClient)
                .deleteReview(reviewId, userId);

        UnauthorizedReviewOperationException result =
                assertThrows(
                        UnauthorizedReviewOperationException.class,
                        () -> reviewService.deleteReview(reviewId)
                );

        assertEquals(
                "You cannot delete another user's review.",
                result.getMessage()
        );

        assertEquals(
                forbiddenException,
                result.getCause()
        );
    }

    @Test
    void createReviewShouldRejectNullProductId() {
        CreateReviewRequest request =
                CreateReviewRequest.builder()
                        .rating(5)
                        .comment("Excellent natural honey.")
                        .build();

        InvalidReviewDataException exception =
                assertThrows(
                        InvalidReviewDataException.class,
                        () -> reviewService.createReview(
                                null,
                                request
                        )
                );

        assertEquals(
                "Product id is required.",
                exception.getMessage()
        );

        verify(productService, never()).getById(any());
        verify(reviewFeignClient, never()).createReview(any());
    }

    @Test
    void createReviewShouldRejectNullRequest() {
        InvalidReviewDataException exception =
                assertThrows(
                        InvalidReviewDataException.class,
                        () -> reviewService.createReview(
                                UUID.randomUUID(),
                                null
                        )
                );

        assertEquals(
                "Review request is required.",
                exception.getMessage()
        );

        verify(productService, never()).getById(any());
        verify(reviewFeignClient, never()).createReview(any());
    }

    @Test
    void createReviewShouldRejectMissingProduct() {
        UUID productId = UUID.randomUUID();

        CreateReviewRequest request =
                CreateReviewRequest.builder()
                        .rating(5)
                        .comment("Excellent natural honey.")
                        .build();

        when(productService.getById(productId))
                .thenThrow(new ProductNotFoundException(
                        "Product does not exist."
                ));

        ProductNotFoundException exception =
                assertThrows(
                        ProductNotFoundException.class,
                        () -> reviewService.createReview(
                                productId,
                                request
                        )
                );

        assertEquals(
                "Product does not exist.",
                exception.getMessage()
        );

        verify(productService).getById(productId);
        verify(userService, never()).getCurrentUser();
        verify(reviewFeignClient, never()).createReview(any());
    }

    @Test
    void updateReviewShouldTranslateNotFoundException() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(userId)
                .build();

        UpdateReviewRequest request =
                UpdateReviewRequest.builder()
                        .rating(4)
                        .comment("Updated review comment.")
                        .build();

        FeignException.NotFound notFoundException =
                mock(FeignException.NotFound.class);

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        when(reviewFeignClient.updateReview(
                eq(reviewId),
                any(UpdateReviewRequest.class)
        )).thenThrow(notFoundException);

        ReviewNotFoundException result =
                assertThrows(
                        ReviewNotFoundException.class,
                        () -> reviewService.updateReview(
                                reviewId,
                                request
                        )
                );

        assertEquals(
                "Review with ID "
                        + reviewId
                        + " was not found.",
                result.getMessage()
        );

        assertEquals(
                notFoundException,
                result.getCause()
        );
    }

    @Test
    void deleteReviewShouldTranslateNotFoundException() {
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(userId)
                .build();

        FeignException.NotFound notFoundException =
                mock(FeignException.NotFound.class);

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        doThrow(notFoundException)
                .when(reviewFeignClient)
                .deleteReview(reviewId, userId);

        ReviewNotFoundException result =
                assertThrows(
                        ReviewNotFoundException.class,
                        () -> reviewService.deleteReview(reviewId)
                );

        assertEquals(
                "Review with ID "
                        + reviewId
                        + " was not found.",
                result.getMessage()
        );

        assertEquals(
                notFoundException,
                result.getCause()
        );
    }

    @Test
    void getReviewsShouldTranslateRetryableException() {
        UUID productId = UUID.randomUUID();

        RetryableException retryableException =
                mock(RetryableException.class);

        when(reviewFeignClient.getReviewsByProductId(productId))
                .thenThrow(retryableException);

        ReviewServiceUnavailableException result =
                assertThrows(
                        ReviewServiceUnavailableException.class,
                        () -> reviewService
                                .getReviewsByProductId(productId)
                );

        assertEquals(
                "Review service is currently unavailable. "
                        + "Please try again later.",
                result.getMessage()
        );

        assertEquals(
                retryableException,
                result.getCause()
        );
    }

    @Test
    void createReviewShouldTranslateServerError() {
        UUID productId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Honey Customer")
                .build();

        CreateReviewRequest request =
                CreateReviewRequest.builder()
                        .rating(5)
                        .comment("Excellent natural honey.")
                        .build();

        FeignException serverException =
                mock(FeignException.class);

        when(serverException.status()).thenReturn(500);

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        when(reviewFeignClient.createReview(
                any(CreateReviewRequest.class)
        )).thenThrow(serverException);

        ReviewServiceUnavailableException result =
                assertThrows(
                        ReviewServiceUnavailableException.class,
                        () -> reviewService.createReview(
                                productId,
                                request
                        )
                );

        assertEquals(
                "Review service is currently unavailable. "
                        + "Please try again later.",
                result.getMessage()
        );

        assertEquals(
                serverException,
                result.getCause()
        );
    }

    @Test
    void createReviewShouldTranslateClientError() {
        UUID productId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Honey Customer")
                .build();

        CreateReviewRequest request =
                CreateReviewRequest.builder()
                        .rating(5)
                        .comment("Excellent natural honey.")
                        .build();

        FeignException clientException =
                mock(FeignException.class);

        when(clientException.status()).thenReturn(400);

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        when(reviewFeignClient.createReview(
                any(CreateReviewRequest.class)
        )).thenThrow(clientException);

        InvalidReviewDataException result =
                assertThrows(
                        InvalidReviewDataException.class,
                        () -> reviewService.createReview(
                                productId,
                                request
                        )
                );

        assertEquals(
                "The review service rejected the submitted data.",
                result.getMessage()
        );

        assertEquals(
                clientException,
                result.getCause()
        );
    }

    @ParameterizedTest
    @MethodSource("invalidReviewContent")
    void createReviewShouldRejectInvalidContent(
            Integer rating,
            String comment,
            String expectedMessage) {

        CreateReviewRequest request =
                CreateReviewRequest.builder()
                        .rating(rating)
                        .comment(comment)
                        .build();

        InvalidReviewDataException exception =
                assertThrows(
                        InvalidReviewDataException.class,
                        () -> reviewService.createReview(
                                UUID.randomUUID(),
                                request
                        )
                );

        assertEquals(
                expectedMessage,
                exception.getMessage()
        );

        verify(productService, never()).getById(any());
        verify(reviewFeignClient, never()).createReview(any());
    }

    private static Stream<Arguments> invalidReviewContent() {
        return Stream.of(
                Arguments.of(
                        (Integer) null,
                        "Excellent natural honey.",
                        "Rating is required."
                ),
                Arguments.of(
                        0,
                        "Excellent natural honey.",
                        "Rating must be between 1 and 5."
                ),
                Arguments.of(
                        6,
                        "Excellent natural honey.",
                        "Rating must be between 1 and 5."
                ),
                Arguments.of(
                        5,
                        (String) null,
                        "Comment is required."
                ),
                Arguments.of(
                        5,
                        "   ",
                        "Comment is required."
                ),
                Arguments.of(
                        5,
                        "short",
                        "Comment must be between 10 and 1000 symbols."
                ),
                Arguments.of(
                        5,
                        "a".repeat(1001),
                        "Comment must be between 10 and 1000 symbols."
                )
        );
    }

    @Test
    void getReviewsShouldRejectNullProductId() {
        InvalidReviewDataException exception =
                assertThrows(
                        InvalidReviewDataException.class,
                        () -> reviewService
                                .getReviewsByProductId(null)
                );

        assertEquals(
                "Product id is required.",
                exception.getMessage()
        );

        verify(reviewFeignClient, never())
                .getReviewsByProductId(any());
    }

    @Test
    void updateReviewShouldRejectNullReviewId() {
        UpdateReviewRequest request =
                UpdateReviewRequest.builder()
                        .rating(4)
                        .comment("Updated review comment.")
                        .build();

        InvalidReviewDataException exception =
                assertThrows(
                        InvalidReviewDataException.class,
                        () -> reviewService.updateReview(
                                null,
                                request
                        )
                );

        assertEquals(
                "Review id is required.",
                exception.getMessage()
        );

        verify(userService, never()).getCurrentUser();
        verify(reviewFeignClient, never())
                .updateReview(any(), any());
    }

    @Test
    void updateReviewShouldRejectNullRequest() {
        InvalidReviewDataException exception =
                assertThrows(
                        InvalidReviewDataException.class,
                        () -> reviewService.updateReview(
                                UUID.randomUUID(),
                                null
                        )
                );

        assertEquals(
                "Review update request is required.",
                exception.getMessage()
        );

        verify(userService, never()).getCurrentUser();
        verify(reviewFeignClient, never())
                .updateReview(any(), any());
    }

    @Test
    void deleteReviewShouldRejectNullReviewId() {
        InvalidReviewDataException exception =
                assertThrows(
                        InvalidReviewDataException.class,
                        () -> reviewService.deleteReview(null)
                );

        assertEquals(
                "Review id is required.",
                exception.getMessage()
        );

        verify(userService, never()).getCurrentUser();
        verify(reviewFeignClient, never())
                .deleteReview(any(), any());
    }
}

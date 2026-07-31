package app.service;

import app.client.ReviewFeignClient;
import app.model.entity.user.User;
import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.ReviewResponse;
import app.exception.ReviewAlreadyExistsException;
import app.exception.UnauthorizedReviewOperationException;
import app.web.dto.review.UpdateReviewRequest;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewFeignClient reviewFeignClient;

    @Mock
    private UserService userService;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                reviewFeignClient,
                userService
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
                        .comment("Excellent natural honey.")
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
}

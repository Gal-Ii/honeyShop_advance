package app.service;

import app.client.ReviewFeignClient;
import app.model.entity.user.User;
import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.ReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
}

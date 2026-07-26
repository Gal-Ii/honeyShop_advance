package app.service;

import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final RestClient reviewRestClient;

    public List<ReviewResponse> getReviewsByProductId(UUID productId) {

        return reviewRestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/reviews")
                        .queryParam("productId", productId)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ReviewResponse createReview(CreateReviewRequest request) {

        return reviewRestClient
                .post()
                .uri("/api/v1/reviews")
                .body(request)
                .retrieve()
                .body(ReviewResponse.class);
    }
}

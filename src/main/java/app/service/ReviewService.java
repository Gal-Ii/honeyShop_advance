package app.service;

import app.client.ReviewFeignClient;
import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewFeignClient reviewFeignClient;

    public List<ReviewResponse> getReviewsByProductId(UUID productId) {

        return reviewFeignClient.getReviewsByProductId(productId);
    }

    public ReviewResponse createReview(CreateReviewRequest request) {

        return reviewFeignClient.createReview(request);
    }
}
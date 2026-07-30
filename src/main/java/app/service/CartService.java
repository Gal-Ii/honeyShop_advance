package app.service;

import app.exception.InvalidCartDataException;
import app.exception.NotEnoughQuantityException;
import app.exception.ProductNotFoundException;
import app.exception.UnauthorizedActionException;
import app.model.entity.cartitem.CartItem;
import app.model.entity.product.Product;
import app.model.entity.user.User;
import app.repository.cartitem.CartItemRepository;
import app.repository.product.ProductRepository;
import app.web.dto.cart.AddToCartRequest;
import app.web.dto.cart.CartItemResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    public List<CartItemResponse> getCart(User user){

        if(user == null){
            throw new UnauthorizedActionException("User must be logged in.");
        }

        return cartItemRepository.findAllByUser(user)
                .stream()
                .map(this::mapToCartItemResponse)
                .toList();
    }

    public CartItemResponse addToCart(User user, AddToCartRequest request){
        if(user == null){
            throw new UnauthorizedActionException("User must be logged in.");
        }

        validateAddToCartRequest(request);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product does not exist"));

        if(!Boolean.TRUE.equals(product.getIsActive())){
            throw new InvalidCartDataException("Product is not active");
        }

        if(request.getQuantity()>product.getItems()){
            throw new NotEnoughQuantityException("Not enough product quantity.");
        }

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product).orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if(cartItem == null){
            cartItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .createdOn(now)
                    .updatedOn(now)
                    .build();
        }else {
            int newQuantity = cartItem.getQuantity() + request.getQuantity();

            if(newQuantity> product.getItems()){
                throw new NotEnoughQuantityException("Not enough product quantity.");
            }

            cartItem.setQuantity(newQuantity);
            cartItem.setUpdatedOn(now);
        }

        CartItem savedCartItem = cartItemRepository.save(cartItem);
        log.info(
                "Product added to cart: userId={}, productId={}, " +
                        "addedQuantity={}, totalQuantity={}",
                user.getId(),
                product.getId(),
                request.getQuantity(),
                savedCartItem.getQuantity()
        );
        return mapToCartItemResponse(savedCartItem);
    }

    public void removeFromCart(User user, UUID cartItemId){
        if(user == null){
            throw new UnauthorizedActionException("User must be logged in.");
        }

        if(cartItemId == null){
            throw new InvalidCartDataException(
                    "Cart item id is required"
            );
        }

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new InvalidCartDataException("Cart item does not exist"));

        if(!cartItem.getUser().getId().equals(user.getId())){
            throw new UnauthorizedActionException("Cart item does not belong to current user");
        }
        cartItemRepository.delete(cartItem);
        log.info(
                "Cart item removed: userId={}, cartItemId={}, productId={}",
                user.getId(),
                cartItem.getId(),
                cartItem.getProduct().getId()
        );
    }

    public void clearCart(User user){
        if(user == null){
            throw new UnauthorizedActionException("User must be logged in.");
        }
        cartItemRepository.deleteAllByUser(user);
        log.info(
                "Cart cleared: userId={}",
                user.getId()
        );
    }

    private CartItemResponse mapToCartItemResponse(CartItem cartItem){
        Product product = cartItem.getProduct();

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .imageUrl(product.getImageUrl())
                .price(product.getPrice())
                .quantity(cartItem.getQuantity())
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .build();
    }

    @Transactional
    public int removeExpiredCartItems(LocalDateTime expirationDate) {

        if (expirationDate == null) {
            throw new InvalidCartDataException(
                    "Cart expiration date is required."
            );
        }

        int removedItems =
                cartItemRepository.deleteAllByCreatedOnBefore(expirationDate);

        if (removedItems > 0) {
            log.info(
                    "Removed [{}] expired cart items created before [{}].",
                    removedItems,
                    expirationDate
            );
        }

        return removedItems;
    }

    private void validateAddToCartRequest(
            AddToCartRequest request) {

        if (request == null) {
            throw new InvalidCartDataException(
                    "Cart request is required."
            );
        }

        if (request.getProductId() == null) {
            throw new InvalidCartDataException(
                    "Product id is required."
            );
        }

        if (request.getQuantity() == null) {
            throw new InvalidCartDataException(
                    "Cart quantity is required."
            );
        }

        if (request.getQuantity() <= 0) {
            throw new InvalidCartDataException(
                    "Cart quantity must be positive."
            );
        }
    }
}

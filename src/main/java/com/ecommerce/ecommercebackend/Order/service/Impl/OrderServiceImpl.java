package com.ecommerce.ecommercebackend.Order.service.Impl;


import com.ecommerce.ecommercebackend.Cart.dto.CartItemResponse;
import com.ecommerce.ecommercebackend.Cart.service.CartService;
import com.ecommerce.ecommercebackend.Order.entity.*;
import com.ecommerce.ecommercebackend.Order.exception.OrderCancellationException;
import com.ecommerce.ecommercebackend.Order.exception.OrderNotFoundException;
import com.ecommerce.ecommercebackend.Order.repository.OrderRepository;
import com.ecommerce.ecommercebackend.Order.service.OrderService;
import com.ecommerce.ecommercebackend.Product.entity.Product;
import com.ecommerce.ecommercebackend.Product.exception.ProductNotFoundException;
import com.ecommerce.ecommercebackend.Product.exception.ProductOutOfStockException;
import com.ecommerce.ecommercebackend.Product.repository.ProductRepository;
import com.ecommerce.ecommercebackend.auth.exception.UserNotFoundException;
import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.repository.UsersRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final UsersRepo usersRepo;
    private final com.ecommerce.ecommercebackend.ai.repository.NegotiatedOfferRepository negotiatedOfferRepository;


    /**
     * Create an order from the user's cart and clear the cart.
     *
     * @param userEmail       email of the authenticated user
     * @param shippingAddress shipping address for the order
     * @return the created Order
     */
    @Override
    @Transactional
    public Order createOrderFromCart(String userEmail, String shippingAddress) {
       Users user = usersRepo.findByEmail(userEmail).orElseThrow(
               () -> new UserNotFoundException("No User found with this email : " + userEmail)
       );

        List<CartItemResponse> cartItems = cartService.getCart(userEmail).getItems();

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Order order = Order.builder()
                .user(user)
                .shippingAddress(shippingAddress)
                .status(OrderStatus.CREATED)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CartItemResponse ci : cartItems) {
            Product p = productRepository.findById(ci.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found with this id : " + ci.getProductId()));

            if (p.getStock() < ci.getQuantity()) {
                throw new ProductOutOfStockException("Insufficient stock for product : " + p.getName());
            }

            // Decrement stock during order creation (it will be restored if order is cancelled or expires)
            p.setStock(p.getStock() - ci.getQuantity());
            productRepository.save(p);

            // Use the price from the cart (which includes negotiated offers)
            BigDecimal effectivePrice = ci.getPrice();
            BigDecimal subtotal = effectivePrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
            total = total.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(p)
                    .quantity(ci.getQuantity())
                    .priceAtPurchase(effectivePrice)
                    .build();

            order.getItems().add(item);
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        // clear cart after order creation
        cartService.clearCart(userEmail);

        return saved;
    }



    /**
     * Create a direct order for a specific product and quantity.
     *
     * @param userEmail       email of the authenticated user
     * @param productId       UUID of the product
     * @param quantity        quantity to order
     * @param shippingAddress shipping address for the order
     * @return the created Order
     */
    @Override
    @Transactional
    public Order createDirectOrder(String userEmail, UUID productId, int quantity, String shippingAddress) {

        Users user = usersRepo.findByEmail(userEmail).orElseThrow(
                () -> new UserNotFoundException("User not found with this email : " + userEmail)
        );

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id : " + productId));

        if (product.getStock() < quantity) {
            throw new ProductOutOfStockException("Insufficient stock");
        }


        // decrement stock after creating order
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        // Check for active negotiated offer for direct order as well
        BigDecimal effectivePrice = product.getPrice();
        java.util.Optional<com.ecommerce.ecommercebackend.ai.entity.NegotiatedOffer> offer = 
            negotiatedOfferRepository.findByUserAndProductAndExpiryDateAfter(user, product, java.time.LocalDateTime.now());
        
        if (offer.isPresent()) {
            effectivePrice = offer.get().getNegotiatedPrice();
        }

        Order order = Order.builder()
                .user(user)
                .shippingAddress(shippingAddress)
                .status(OrderStatus.CREATED)
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .priceAtPurchase(effectivePrice)
                .build();

        order.getItems().add(item);

        order.setTotalAmount(
                effectivePrice.multiply(BigDecimal.valueOf(quantity))
        );

        return orderRepository.save(order);
    }



    /**
     * Retrieve a specific order by its ID for a user.
     *
     * @param id        order ID
     * @param userEmail email of the authenticated user
     * @return the found Order
     */
    @Override
    public Order getOrderById(Long id, String userEmail) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("No order found with this Id : ",id));
        if (order.getUser() == null || !userEmail.equals(order.getUser().getEmail())) {
            throw new OrderNotFoundException("NO order found with this Id :" ,id);
        }
        return order;
    }



    /**
     * Retrieve paginated orders for a specific user.
     *
     * @param userEmail email of the authenticated user
     * @param pageable  pagination information
     * @return paginated orders
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public Page<Order> getOrdersForUser(String userEmail, Pageable pageable) {
        Users user = usersRepo.findByEmail(userEmail).orElseThrow(
                () -> new UserNotFoundException("User not found with this email : " + userEmail)
        );

        return orderRepository.findAllByUser(user, pageable);
    }




    /**
     * Cancel an order if allowed by business rules and restore stock.
     *
     * @param orderId   ID of the order to cancel
     * @param userEmail email of the authenticated user
     */
    @Override
    @Transactional
    public void cancelOrder(Long orderId, String userEmail) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("No order found with this Id : ",orderId));

        // ownership check (security)
        if (!userEmail.equals(order.getUser().getEmail())) {
            throw new OrderNotFoundException("No order found with this Id : ",orderId);
        }

        // business rule check
        if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderCancellationException("Order cannot be cancelled at this stage");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return; // idempotent (cancel twice = no problem)
        }

        // restore stock here
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelOrderSystem(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("No order found with this Id : ", orderId));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.PAID) {
            return;
        }

        // restore stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}


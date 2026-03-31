# Ecommerce Backend API Testing Guide

This document explains how to test all APIs in this project with Postman, step by step.

## 1. Prerequisites

Before testing any API, make sure all of the following are ready:

1. MySQL is running.
2. The database configured in [application.properties](D:/JAVA/ecommerce-backend/src/main/resources/application.properties) exists or can be created.
3. The app starts successfully on `http://localhost:8080`.
4. `JWT_SECRET` is set to a secure value with at least 32 characters.
5. If you want to test email verification, password reset, or seller approval emails, Gmail SMTP config must be valid.
6. If you want to test image upload, Cloudinary config must be valid.
7. If you want to test payments and refunds, Stripe config must be valid.

## 2. Recommended Postman Setup

Create a Postman collection called `Ecommerce Backend`.

Create a Postman environment called `local-ecommerce` with these variables:

| Variable                      | Example Value             | Purpose                             |
| ----------------------------- | ------------------------- | ----------------------------------- |
| `baseUrl`                   | `http://localhost:8080` | Base API URL                        |
| `accessToken`               | empty                     | JWT access token                    |
| `refreshToken`              | empty                     | JWT refresh token                   |
| `userEmail`                 | `user1@example.com`     | Normal user email                   |
| `userPassword`              | `Password123!`          | Normal user password                |
| `adminEmail`                | `admin@example.com`     | Admin email                         |
| `adminPassword`             | `Password123!`          | Admin password                      |
| `sellerEmail`               | `seller@example.com`    | Seller email                        |
| `sellerPassword`            | `Password123!`          | Seller password                     |
| `categoryId`                | empty                     | Category ID created during testing  |
| `productId`                 | empty                     | Product UUID created during testing |
| `orderId`                   | empty                     | Order ID created during testing     |
| `paymentSessionId`          | empty                     | Stripe checkout session ID          |
| `emailVerificationToken`    | empty                     | Token from register response        |
| `newEmailVerificationToken` | empty                     | Token from update-email flow        |

## 3. Authorization Rule

Security is configured in [SecurityConfig.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/auth/security/SecurityConfig.java).

- All `/api/v1/auth/**` routes are public.
- Everything else under `/api/**` requires authentication.
- Some routes also require role checks:
  - `ADMIN`
  - `SELLER`

For protected routes, add this header:

```http
Authorization: Bearer {{accessToken}}
```

## 4. Suggested Test Order

Follow this order because later APIs depend on data created earlier:

1. Auth
2. Categories
3. Seller onboarding
4. Products
5. Wishlist
6. Cart
7. Orders
8. Payments
9. Account maintenance
10. Negative tests

## 5. Postman Folder Structure

Create these folders inside the collection:

- `01 Auth`
- `02 Category`
- `03 Seller`
- `04 Product`
- `05 Wishlist`
- `06 Cart`
- `07 Order`
- `08 Payment`
- `09 Account Maintenance`
- `10 Negative Tests`

## 6. Common Postman Scripts

### 6.1 Save login tokens

Add this in the `Tests` tab of login:

```javascript
const json = pm.response.json();
pm.environment.set("accessToken", json.accessToken);
pm.environment.set("refreshToken", json.refreshToken);
```

### 6.2 Save register verification token

Add this in the `Tests` tab of register:

```javascript
const json = pm.response.json();
if (json.verificationToken) {
  pm.environment.set("emailVerificationToken", json.verificationToken);
}
```

### 6.3 Save created category id

```javascript
const json = pm.response.json();
if (json.id) {
  pm.environment.set("categoryId", json.id);
}
```

### 6.4 Save created product id

```javascript
const json = pm.response.json();
if (json.id) {
  pm.environment.set("productId", json.id);
}
```

### 6.5 Save created order id

```javascript
const json = pm.response.json();
if (json.id) {
  pm.environment.set("orderId", json.id);
}
```

### 6.6 Save payment session id

```javascript
const json = pm.response.json();
if (json.sessionId) {
  pm.environment.set("paymentSessionId", json.sessionId);
}
```

## 7. Auth API Tests

Controller: [AuthController.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/auth/controller/AuthController.java)

### 7.1 Register user

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/auth/register`
- Auth: none
- Body type: `form-data`

Fields:

| Key           | Type | Required | Example              |
| ------------- | ---- | -------- | -------------------- |
| `firstName` | Text | Yes      | `John`             |
| `lastName`  | Text | Yes      | `Doe`              |
| `email`     | Text | Yes      | `{{userEmail}}`    |
| `password`  | Text | Yes      | `{{userPassword}}` |
| `role`      | Text | Yes      | `ROLE_USER`        |
| `file`      | File | No       | profile image        |

Expected:

- Status `201`
- Response contains `message`
- Response may contain `verificationToken`

### 7.2 Register admin

Repeat the same request with:

- `email = {{adminEmail}}`
- `password = {{adminPassword}}`
- `role = ROLE_ADMIN`

Expected:

- Status `201`

### 7.3 Register seller candidate

Repeat the same request with:

- `email = {{sellerEmail}}`
- `password = {{sellerPassword}}`
- `role = ROLE_USER`

Expected:

- Status `201`

Note:

- Seller onboarding later promotes a normal user to seller.

### 7.4 Verify email

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/auth/verify-email?token={{emailVerificationToken}}`
- Auth: none

Expected:

- Status `200`
- Message confirms verification

If SMTP is not configured:

- Use the `verificationToken` returned in register response.

### 7.5 Login

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/auth/login`
- Auth: none
- Body type: `raw -> JSON`

```json
{
  "email": "{{userEmail}}",
  "password": "{{userPassword}}"
}
```

Expected:

- Status `200`
- Response contains:
  - `accessToken`
  - `refreshToken`
  - `email`
  - `role`

### 7.6 Refresh token

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/auth/refresh-token`
- Auth: none
- Body:

```json
{
  "token": "{{refreshToken}}"
}
```

Expected:

- Status `200`
- New access token returned

### 7.7 Get current profile

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/auth/me`
- Auth: Bearer token

Expected:

- Status `200`
- Profile of logged-in user

### 7.8 Update profile

- Method: `PUT`
- URL: `{{baseUrl}}/api/v1/auth/update-profile`
- Auth: Bearer token
- Body type: `form-data`

Optional fields:

| Key           | Type | Example    |
| ------------- | ---- | ---------- |
| `firstName` | Text | `Johnny` |
| `lastName`  | Text | `Doer`   |
| `file`      | File | image      |

Expected:

- Status `200`
- Updated profile returned

### 7.9 Request email update

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/auth/update-email`
- Auth: Bearer token
- Body:

```json
{
  "message": "change email",
  "newEmail": "newuser@example.com",
  "verificationToken": "placeholder"
}
```

Expected:

- Status `202`

Important:

- `verificationToken` exists in the DTO, but the controller only uses `newEmail`.
- If service returns a token in response or email, save it for the next step.

### 7.10 Verify updated email

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/auth/update-email/verify?token={{newEmailVerificationToken}}`
- Auth: none

Expected:

- Status `200`

### 7.11 Update password

- Method: `PUT`
- URL: `{{baseUrl}}/api/v1/auth/update-password`
- Auth: Bearer token
- Body:

```json
{
  "oldPassword": "{{userPassword}}",
  "newPassword": "NewPassword123!"
}
```

Expected:

- Status `200`

### 7.12 Forgot password

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/auth/forgot-password`
- Auth: none
- Body:

```json
{
  "email": "{{userEmail}}"
}
```

Expected:

- Status `200`
- OTP or reset flow starts

### 7.13 Reset password

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/auth/reset-password`
- Auth: none
- Body:

```json
{
  "email": "{{userEmail}}",
  "otp": "123456",
  "newPassword": "ResetPassword123!"
}
```

Expected:

- Status `200`

### 7.14 Logout

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/auth/logout`
- Auth: Bearer token

Expected:

- Status `200`

### 7.15 Dev list users

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/auth/dev/users`
- Auth: none

Expected:

- Status `200`
- Returns all users

Note:

- This is a dev-only endpoint and should not exist in production.

## 8. Category API Tests

Controller: [CategoryController.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/Category/Controller/CategoryController.java)

Important:

- `POST`, `PUT`, `DELETE` require `ADMIN`.

### 8.1 Login as admin

Use the login endpoint with:

```json
{
  "email": "{{adminEmail}}",
  "password": "{{adminPassword}}"
}
```

This will replace `{{accessToken}}` with the admin token.

### 8.2 Create category

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/categories`
- Auth: Bearer token
- Body:

```json
{
  "name": "Electronics",
  "description": "Electronic devices and accessories",
  "parentId": null
}
```

Expected:

- Status `201`
- Response contains category details

### 8.3 Get category by id

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/categories/{{categoryId}}`
- Auth: Bearer token or no token if endpoint is not blocked by security role rules

Expected:

- Status `200`

### 8.4 Get all categories

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/categories`
- Auth: Bearer token

Expected:

- Status `200`

### 8.5 Update category

- Method: `PUT`
- URL: `{{baseUrl}}/api/v1/categories/{{categoryId}}`
- Auth: Bearer token
- Body:

```json
{
  "name": "Electronics Updated",
  "description": "Updated description",
  "parentId": null
}
```

Expected:

- Status `200`

### 8.6 Delete category

- Method: `DELETE`
- URL: `{{baseUrl}}/api/v1/categories/{{categoryId}}`
- Auth: Bearer token

Expected:

- Status `200`

## 9. Seller API Tests

Controller: [SellerController.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/seller/Controller/SellerController.java)

### 9.1 Login as seller candidate

Login using:

```json
{
  "email": "{{sellerEmail}}",
  "password": "{{sellerPassword}}"
}
```

### 9.2 Submit seller request

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/seller/request`
- Auth: Bearer token
- Body type: `form-data`

Fields:

| Key           | Type | Required | Example                        |
| ------------- | ---- | -------- | ------------------------------ |
| `storeName` | Text | Yes      | `My Gadget Store`            |
| `reason`    | Text | No       | `I want to sell electronics` |
| `document`  | File | Yes      | ID proof or test document      |

Expected:

- Status `200`
- Response contains request info

### 9.3 Login as admin

Login again as admin to get admin token in `{{accessToken}}`.

### 9.4 List pending seller requests

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/seller/seller-requests`
- Auth: Bearer token

Expected:

- Status `200`
- Response list includes pending request

### 9.5 Approve seller request

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/seller/approve/1`
- Auth: Bearer token

Expected:

- Status `200`
- User becomes `ROLE_SELLER`

Note:

- Replace `1` with the actual seller request id.

### 9.6 Reject seller request

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/seller/reject/1?reason=Incomplete%20document`
- Auth: Bearer token

Expected:

- Status `200`

Use either approve or reject, not both for the same request.

## 10. Product API Tests

Controller: [ProductController.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/Product/controller/ProductController.java)

Important:

- Create product requires `SELLER`.
- Update and delete require `SELLER` or `ADMIN`.

### 10.1 Login as seller

After seller approval, login again as `{{sellerEmail}}`.

### 10.2 Create product

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/products`
- Auth: Bearer token
- Body type: `form-data`

Fields:

| Key             | Type | Required | Example               |
| --------------- | ---- | -------- | --------------------- |
| `name`        | Text | Yes      | `iPhone 15`         |
| `description` | Text | Yes      | `Latest smartphone` |
| `price`       | Text | Yes      | `79999`             |
| `categoryId`  | Text | Yes      | `{{categoryId}}`    |
| `stock`       | Text | Yes      | `10`                |
| `file`        | File | No       | product image         |

Expected:

- Status `201`
- Response contains product UUID

### 10.3 Get all products

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/products`
- Auth: Bearer token

Expected:

- Status `200`

### 10.4 Get product by id

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/products/{{productId}}`
- Auth: Bearer token

Expected:

- Status `200`

### 10.5 Update product

- Method: `PUT`
- URL: `{{baseUrl}}/api/v1/products/{{productId}}`
- Auth: Bearer token
- Body type: `form-data`

Example fields:

| Key             | Type | Example                 |
| --------------- | ---- | ----------------------- |
| `name`        | Text | `iPhone 15 Pro`       |
| `description` | Text | `Updated description` |
| `price`       | Text | `89999`               |
| `categoryId`  | Text | `{{categoryId}}`      |
| `stock`       | Text | `8`                   |
| `file`        | File | new image               |

Expected:

- Status `200`

### 10.6 Delete product

- Method: `DELETE`
- URL: `{{baseUrl}}/api/v1/products/{{productId}}`
- Auth: Bearer token

Expected:

- Status `200`

Only run this after wishlist, cart, and order tests if you still need the product.

## 11. Wishlist API Tests

Controller: [WishlistController.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/Wishlist/Controller/WishlistController.java)

### 11.1 Login as normal user

Login as `{{userEmail}}`.

### 11.2 Add to wishlist

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/wishlist/add/{{productId}}`
- Auth: Bearer token

Expected:

- Status `200`

### 11.3 Get wishlist

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/wishlist`
- Auth: Bearer token

Expected:

- Status `200`

### 11.4 Remove from wishlist

- Method: `DELETE`
- URL: `{{baseUrl}}/api/v1/wishlist/remove/{{productId}}`
- Auth: Bearer token

Expected:

- Status `200`

### 11.5 Clear wishlist

- Method: `DELETE`
- URL: `{{baseUrl}}/api/v1/wishlist/clear`
- Auth: Bearer token

Expected:

- Status `200`

## 12. Cart API Tests

Controller: [CartController.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/Cart/Controller/CartController.java)

### 12.1 Add to cart

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/cart/add`
- Auth: Bearer token
- Body:

```json
{
  "productId": "{{productId}}",
  "quantity": 2
}
```

Expected:

- Status `200`
- Updated cart returned

### 12.2 Get cart

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/cart`
- Auth: Bearer token

Expected:

- Status `200`

### 12.3 Update cart quantity

- Method: `PUT`
- URL: `{{baseUrl}}/api/v1/cart/update`
- Auth: Bearer token
- Body:

```json
{
  "productId": "{{productId}}",
  "quantity": 5
}
```

Expected:

- Status `200`

### 12.4 Remove item from cart

- Method: `DELETE`
- URL: `{{baseUrl}}/api/v1/cart/remove/{{productId}}`
- Auth: Bearer token

Expected:

- Status `200`

### 12.5 Clear cart

- Method: `DELETE`
- URL: `{{baseUrl}}/api/v1/cart/clear`
- Auth: Bearer token

Expected:

- Status `200`

## 13. Order API Tests

Controller: [OrderController.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/Order/Controller/OrderController.java)

### 13.1 Create order from cart

First add at least one product into cart.

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/orders`
- Auth: Bearer token
- Body:

```json
{
  "shippingAddress": "221B Baker Street, London"
}
```

Expected:

- Status `201`
- Response contains order data

### 13.2 Create direct order

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/orders/direct`
- Auth: Bearer token
- Body:

```json
{
  "productId": "{{productId}}",
  "quantity": 1,
  "shippingAddress": "221B Baker Street, London"
}
```

Expected:

- Status `201`

### 13.3 List orders

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/orders?page=0&size=10&sort=id,desc`
- Auth: Bearer token

Expected:

- Status `200`
- Paginated response (Note: `OrderSummaryResponse` now includes `itemThumbnails` for product visual previews)

### 13.4 Get order by id

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/orders/{{orderId}}`
- Auth: Bearer token

Expected:

- Status `200` (Note: `OrderItemResponse` now includes `imageUrl` for each product item)

### 13.5 Cancel order

- Method: `PATCH`
- URL: `{{baseUrl}}/api/v1/orders/{{orderId}}/cancel`
- Auth: Bearer token

Expected:

- Status `200`
- Message `Order cancelled.`

Note:

- Cancellation may fail depending on order status.

## 14. Payment API Tests

Controller: [PaymentController.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/payment/Controller/PaymentController.java)

Prerequisites:

- Stripe secret key is valid.
- The order is eligible for payment.

### 14.1 Create Stripe checkout session

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/payments/create/{{orderId}}`
- Auth: Bearer token

Expected:

- Status `200`
- Response contains:
  - `sessionId`
  - `url`

Open the returned `url` in browser to complete payment if Stripe is configured.

### 14.2 Confirm payment

- Method: `GET`
- URL: `{{baseUrl}}/api/v1/payments/confirm?session_id={{paymentSessionId}}`
- Auth: Bearer token

Expected:

- Status `200`
- Response example:

```json
{
  "status": "OK",
  "message": "Payment confirmed successfully",
  "paid": true
}
```

Note: A `"status": "PENDING"` indicates the payment is still in progress.

### 14.3 Refund payment

- Method: `POST`
- URL: `{{baseUrl}}/api/v1/payments/refund`
- Auth: Bearer token
- Body:

Full refund:

```json
{
  "orderId": {{orderId}},
  "amount": null
}
```

Partial refund:

```json
{
  "orderId": {{orderId}},
  "amount": 100.00
}
```

Expected:

- Status `200`
- Response contains `refundId` and `status`

## 15. Utility Endpoints

### 15.1 Payment success page

Controller: [PaymentRedirctController.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/payment/Controller/PaymentRedirctController.java)

- Method: `GET`
- URL: `{{baseUrl}}/payment/success`
- Auth: none

Expected:

- Static success page or forwarded success resource

### 15.2 Test email

Controller: [TestEmailController.java](D:/JAVA/ecommerce-backend/src/main/java/com/ecommerce/ecommercebackend/auth/controller/TestEmailController.java)

- Method: `GET`
- URL: `{{baseUrl}}/test-email`
- Auth: none

Expected:

- Email sent if SMTP config is valid

Warning:

- This endpoint sends email to a hardcoded address.

## 16. Negative Tests You Should Run

These are necessary to properly validate the API.

### 16.1 Auth negative tests

- Login with wrong password -> expect `401` or business error
- Register with existing email -> expect validation/business error
- Refresh token with invalid token -> expect failure
- Access `/api/v1/auth/me` without token -> expect failure because controller requires authentication object even though path is public in security config

### 16.2 Role negative tests

- Create category with normal user token -> expect `403`
- Create product with normal user token -> expect `403`
- Approve seller request with non-admin token -> expect `403`

### 16.3 Resource negative tests

- Get category with bad id -> expect `404`
- Get product with bad UUID -> expect `404`
- Add invalid product to cart -> expect `404`
- Create direct order with quantity `0` -> expect validation error
- Refund non-paid order -> expect business error

### 16.4 Token negative tests

- Use expired token -> expect `401`
- Use malformed token -> expect `401`
- Use token of another user on protected business flow -> expect failure if ownership checks exist

## 17. Practical End-to-End Flow

If you want one clean end-to-end Postman run, use this exact sequence:

1. Register admin
2. Register normal user
3. Register seller candidate
4. Verify their emails
5. Login as admin
6. Create category
7. List pending seller requests after seller candidate submits request
8. Approve seller request
9. Login as seller
10. Create product
11. Login as normal user
12. Add product to wishlist
13. Add product to cart
14. Create order
15. Create payment session
16. Confirm payment
17. Refund payment if needed
18. Update profile
19. Update password
20. Logout

## 18. Known Testing Notes For This Codebase

- Role values during registration should be one of:
  - `ROLE_USER`
  - `ROLE_ADMIN`
  - `ROLE_SELLER`
- Some public auth endpoints internally still rely on authenticated context for actual success, so treat `/me`, `/logout`, `/update-profile`, `/update-email`, and `/update-password` as token-required during testing.
- Multipart routes require Postman `form-data`, not raw JSON.
- Product IDs are UUIDs.
- Order IDs and category IDs are numeric.
- Payment confirmation requires a real Stripe session if you want to validate the full payment flow.
- Valid Order Statuses: `CREATED`, `PENDING_PAYMENT`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `FAILED`, `REFUNDED`.

## 19. What To Verify For Every Request

For each request, verify all of these:

1. Correct status code
2. Correct response body shape
3. Correct database change
4. Correct role restriction behavior
5. Correct ownership behavior
6. Correct validation behavior for invalid input

## 20. Optional Next Improvement

After manual Postman testing, the next step should be:

1. Export the Postman collection
2. Add collection tests
3. Run the collection with Newman
4. Add automated integration tests in Spring Boot

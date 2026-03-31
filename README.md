# Ec-Kart Backend

A robust, enterprise-ready Spring Boot backend for the Ec-Kart e-commerce platform.

## 🚀 Key Features
- **User Management**: Secure registration and login using **JWT (JSON Web Tokens)**.
- **Order System**: Complete order lifecycle with **Stripe** payment integration.
- **Product Management**: Category-based product organization with **Cloudinary** image hosting.
- **Inventory & Tracking**: Real-time stock management and order tracking status.
- **Security**: Role-based access control (Admin, Seller, Buyer) powered by **Spring Security**.

## 🛠️ Technology Stack
- **Language**: Java 21+
- **Framework**: Spring Boot 4.0.5
- **Database**: MySQL 8.0+
- **Persistence**: Spring Data JPA / Hibernate
- **Security**: Spring Security & JJWT
- **Payment Gateway**: Stripe Java SDK
- **Media Storage**: Cloudinary
- **Build Tool**: Maven

## 📋 Prerequisites
- **JDK 21** or later
- **MySQL Server**
- **Maven**
- Accounts for **Stripe** and **Cloudinary** (for API keys)

## ⚙️ Setup & Installation

1. **Clone the repository**
2. **Configure Environment**: Update `src/main/resources/application.properties` with your credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
   spring.datasource.username=YOUR_USERNAME
   spring.datasource.password=YOUR_PASSWORD
   
   stripe.api.key=YOUR_STRIPE_SECRET_KEY
   cloudinary.cloud_name=YOUR_CLOUD_NAME
   cloudinary.api_key=YOUR_API_KEY
   cloudinary.api_secret=YOUR_API_SECRET
   ```
3. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

## 🛣️ API Endpoints (Quick Reference)
- `POST /api/v1/auth/register` - New user registration
- `POST /api/v1/auth/login` - Authenticate and get JWT
- `GET /api/v1/product/all` - List all products
- `POST /api/v1/orders/create` - Create a new order
- `POST /api/v1/payments/confirm` - Verify Stripe session status

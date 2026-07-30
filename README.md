# Honey Shop Application

Honey Shop is a Spring Boot MVC web application for an online honey store. Visitors can browse the product catalog, while registered users can manage a shopping cart, create orders, and review their order history. The application also provides permission-based administration for products, orders, users, and user roles.

## Technology Stack

- Java 17
- Spring Boot 3.4.0
- Spring MVC
- Spring Security
- Spring Data JPA
- Spring Cloud OpenFeign
- Spring Cache with Caffeine
- Spring Scheduling
- Thymeleaf
- Bean Validation
- MySQL
- H2 for automated tests
- Maven
- Lombok and SLF4J
- JUnit, Mockito, MockMvc, and JaCoCo
- HTML and CSS

## Main Features

### Public functionality

- Browse the home page and product catalog
- Register a new account
- Log in with an email and password

### Authenticated user functionality

- View a personal profile
- Add products to the shopping cart
- Remove products from the shopping cart
- Create an order from the current cart
- View personal order history
- Create, update, and delete product reviews
- Log out securely

### Administrative functionality

- Create, update, and deactivate products
- View all orders and update their status
- View, activate, and deactivate users
- Assign specialized roles to users
- Prevent modification or deactivation of the main administrator

The application records important state-changing operations through SLF4J, including product management, shopping-cart operations, order creation and status changes, user registration, account activation, account deactivation, and role changes.

## Security

Authentication and authorization are implemented with Spring Security. Passwords are stored as BCrypt hashes. CSRF protection remains enabled, and logout is performed through a POST request that invalidates the current HTTP session.

The application defines open, authenticated, and permission-protected endpoints.

### Roles and permissions

| Role | Permissions |
| --- | --- |
| `USER` | Standard customer functionality |
| `PRODUCT_ADMIN` | `PRODUCT_CREATE`, `PRODUCT_UPDATE`, `PRODUCT_DELETE` |
| `ORDER_ADMIN` | `ORDER_STATUS_UPDATE` |
| `USER_ADMIN` | `USER_VIEW`, `USER_ACTIVATE`, `USER_DEACTIVATE` |
| `ADMIN` | All available permissions, including `USER_ROLE_UPDATE` |

Only the main `ADMIN` can assign roles. The `ADMIN` role itself cannot be assigned through the web administration panel.

### Administrative pages

| URL | Purpose | Required access |
| --- | --- | --- |
| `/admin` | Order administration | `ORDER_STATUS_UPDATE` |
| `/admin-products` | Product administration | Product permission |
| `/admin-users` | User administration | `USER_VIEW` |

The navigation bar displays only the administrative sections available to the currently authenticated user.

## Domain Model

The main domain entities are:

- `User`
- `Product`
- `CartItem`
- `Order`
- `OrderItem`

All entities use UUID primary keys. The model contains JPA relationships between users, carts, products, orders, and order items.

## Project Structure

```text
src/main/java/app
|-- config       Security and application configuration
|-- exception    Custom application exceptions
|-- model        JPA entities, enums, and configuration properties
|-- repository   Spring Data JPA repositories
|-- service      Business logic and authorization-aware operations
`-- web
    |-- controller
    `-- dto
```

Thymeleaf templates are located in:

```text
src/main/resources/templates
```

Static CSS and image resources are located in:

```text
src/main/resources/static
```

## REST Microservice Integration

The application communicates with the independent Honey Review Service through
Spring Cloud OpenFeign. The microservice runs on port `8081`, owns a separate
MySQL database, and provides the product review REST API.

The main application invokes the microservice to:

- list reviews for a product
- create a review
- update a review belonging to the current user
- delete a review belonging to the current user

The service address is configurable through:

```properties
review.service.base-url=${REVIEW_SERVICE_BASE_URL:http://localhost:8081}
```

REST microservice repository:

https://github.com/Gal-Ii/honey-review-service

## Scheduling and Caching

The application uses Spring's caching mechanism with Caffeine for active
products, all products, and product details. Product mutations evict the
affected caches.

Two scheduled maintenance jobs are configured:

- a cron-based job that deactivates out-of-stock products
- a fixed-delay job that removes expired shopping-cart entries

The schedules and time zone can be configured through environment variables.

## Configuration

The active Spring profile is configured through:

```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

Supported profiles:

- `dev` - local MySQL instance
- `prod` - production-oriented configuration

The application reads sensitive configuration from environment variables:

```text
SPRING_PROFILES_ACTIVE=dev
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
DEFAULT_ADMIN_PASSWORD=your_secure_admin_password
```

An example is available in:

```text
src/main/resources/application-example.properties
```

### Development database

The development profile connects to:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/honney-shop-application?createDatabaseIfNotExist=true
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

MySQL creates the database automatically when the configured database user has the required permission.

## Default Administrator

The application initializes a main administrator during startup. Its non-sensitive profile settings are configured in the Spring properties, while the password must be supplied through the `DEFAULT_ADMIN_PASSWORD` environment variable.

```properties
users.default-user.password=${DEFAULT_ADMIN_PASSWORD}
```

If the configured administrator already exists but is inactive or does not have the `ADMIN` role, the startup initialization restores the account.

## Running the Application

Prerequisites:

- Java 17 or newer
- Maven
- MySQL Server

Steps:

1. Start MySQL.
2. Configure `DB_USERNAME`, `DB_PASSWORD`, and `DEFAULT_ADMIN_PASSWORD`.
3. Optionally set `SPRING_PROFILES_ACTIVE`; it defaults to `dev`.
4. Reload the Maven project.
5. Run `app.Application` from IntelliJ IDEA or execute:

```bash
mvn spring-boot:run
```

Open the application at:

```text
http://localhost:8080
```

## Build and Test

Compile the project:

```bash
mvn clean compile
```

Run the test suite:

```bash
mvn test
```

Run the complete test and coverage verification:

```bash
mvn verify
```

The project contains unit, integration, MVC controller, and API tests. JaCoCo
enforces a minimum of 70% line coverage. At the latest verification, all 72
tests passed and the measured line coverage was 70.27%.

## Repository

Public repository:

https://github.com/Gal-Ii/honeyShop_advance

## Author

Galina Georgieva

# Class Booking System REST API

A complete REST API for managing class bookings with JWT authentication, built using Spring Boot, H2 database, and comprehensive security features.

## Features

- **User Authentication**: JWT-based secure authentication with role-based access control (USER, ADMIN, INSTRUCTOR)
- **User Management**: User registration, login, and profile management
- **Class Scheduling**: Create, update, view, and cancel classes with instructor assignment
- **Booking System**:
  - Book classes with automatic capacity management
  - Prevent double bookings
  - Cancel bookings with automatic capacity updates
  - Concurrency-safe booking operations
- **API Documentation**: Interactive Swagger UI for testing endpoints
- **Database**: H2 in-memory database with Flyway migrations

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.2**
- **Spring Security** with JWT
- **Spring Data JPA**
- **H2 Database**
- **Flyway** for database migrations
- **Lombok** for reducing boilerplate
- **SpringDoc OpenAPI** for API documentation
- **Maven** for build management

## Prerequisites

- Java 17 or higher
- Maven 3.8 or higher

## Getting Started

### 1. Clone the repository

```bash
cd class-booking-system
```

### 2. Build the project

```bash
mvn clean install
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access the API Documentation

Open your browser and navigate to:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:bookingdb`
  - Username: `sa`
  - Password: (leave empty)

## API Endpoints

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/register` | Register new user | No |
| POST | `/api/v1/auth/login` | Login user | No |

### User Management Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/users/me` | Get current user profile | Yes |
| GET | `/api/v1/users/{id}` | Get user by ID | Admin only |
| GET | `/api/v1/users` | Get all users | Admin only |

### Class Schedule Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/classes` | List all classes | No |
| GET | `/api/v1/classes?availableOnly=true` | List available classes | No |
| GET | `/api/v1/classes?status=SCHEDULED` | Filter by status | No |
| GET | `/api/v1/classes/{id}` | Get class by ID | No |
| POST | `/api/v1/classes` | Create new class | Admin/Instructor |
| PUT | `/api/v1/classes/{id}` | Update class | Admin/Instructor |
| DELETE | `/api/v1/classes/{id}` | Cancel class | Admin/Instructor |

### Booking Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/bookings` | Book a class | Yes |
| DELETE | `/api/v1/bookings/{id}` | Cancel booking | Yes |
| GET | `/api/v1/bookings/my-bookings` | Get user's bookings | Yes |
| GET | `/api/v1/bookings/{id}` | Get booking by ID | Yes |
| GET | `/api/v1/bookings` | Get all bookings | Admin only |

## Usage Examples

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "john@example.com",
  "username": "johndoe",
  "role": "ROLE_USER"
}
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

### 3. Create a Class (requires Admin/Instructor role)

First, you need to manually update a user's role to ADMIN in the H2 console:

```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'john@example.com';
```

Then create a class:

```bash
curl -X POST http://localhost:8080/api/v1/classes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "Yoga Basics",
    "description": "Introduction to Yoga for beginners",
    "startTime": "2026-03-15T10:00:00",
    "endTime": "2026-03-15T11:00:00",
    "capacity": 20,
    "location": "Studio A"
  }'
```

### 4. View Available Classes

```bash
curl -X GET "http://localhost:8080/api/v1/classes?availableOnly=true"
```

### 5. Book a Class

```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "classScheduleId": 1,
    "notes": "Looking forward to this class!"
  }'
```

### 6. View My Bookings

```bash
curl -X GET http://localhost:8080/api/v1/bookings/my-bookings \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 7. Cancel a Booking

```bash
curl -X DELETE http://localhost:8080/api/v1/bookings/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Database Schema

The application uses 4 main tables:

- **users**: User accounts with authentication details
- **instructors**: Instructor profiles linked to users
- **class_schedules**: Class schedules with capacity management
- **bookings**: Booking records linking users to classes

All tables include timestamps and proper foreign key relationships.

## Security Features

- **JWT Authentication**: Stateless token-based authentication
- **Password Encryption**: BCrypt password hashing
- **Role-Based Access Control**: Different permissions for USER, ADMIN, and INSTRUCTOR roles
- **Input Validation**: Bean validation on all request DTOs
- **SQL Injection Prevention**: JPA with parameterized queries
- **Concurrency Control**: Pessimistic locking on booking operations

## Testing

Run unit tests:

```bash
mvn test
```

Run with coverage:

```bash
mvn clean test jacoco:report
```

## Project Structure

```
src/
├── main/
│   ├── java/com/booking/system/
│   │   ├── ClassBookingSystemApplication.java
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Request/Response DTOs
│   │   ├── entity/         # JPA entities
│   │   ├── exception/      # Custom exceptions
│   │   ├── repository/     # Data repositories
│   │   ├── security/       # JWT security components
│   │   └── service/        # Business logic
│   └── resources/
│       ├── application.yml
│       └── db/migration/   # Flyway migration scripts
└── test/
    └── java/com/booking/system/
```

## Key Implementation Details

### Concurrency Handling

The booking system uses pessimistic locking to prevent race conditions:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<ClassSchedule> findByIdWithLock(Long id);
```

This ensures that concurrent booking requests are handled safely without overbooking.

### JWT Configuration

- Token expiration: 24 hours
- Algorithm: HS256
- Token format: Bearer {token}

### Default Roles

- `ROLE_USER`: Can book classes, view own bookings
- `ROLE_ADMIN`: Full access to all endpoints
- `ROLE_INSTRUCTOR`: Can manage classes

## Future Enhancements

- Add email notifications for bookings
- Implement waitlist functionality
- Add class ratings and reviews
- Support recurring classes
- Payment integration
- Calendar integration (iCal/Google Calendar)
- Admin dashboard with analytics

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues or questions, please open an issue in the repository.

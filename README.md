# Register & Login REST API

A Spring Boot RESTful API for registering an account and logging in with email and password.
Accounts are stored in an in-memory H2 database, and passwords are hashed with BCrypt.

## Features

- `POST /api/register` and `POST /api/login` endpoints
- Requests use `application/x-www-form-urlencoded`; responses are plain text
- In-memory H2 storage (data is reset on every restart)
- Passwords hashed with BCrypt — plain text is never stored
- Input validation for email format and password
- Business error handling for duplicate email and invalid login
- Emails are trimmed and lowercased, so `Test@Example.com` and `test@example.com` are the same account

## Tech Stack

- Java 17
- Spring Boot 3.5 (Web, Data JPA, Validation)
- H2 Database (in-memory)
- Spring Security Crypto (BCrypt)
- JUnit 5 + MockMvc
- Maven

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+

### Run

```bash
git clone https://github.com/yoedi/taiwan-mobile-tech-test.git
cd taiwan-mobile-tech-test
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

### Test

```bash
mvn test
```

### Build

```bash
mvn clean package
java -jar target/auth-0.0.1-SNAPSHOT.jar
```

## API

Both endpoints accept `Content-Type: application/x-www-form-urlencoded` with the fields `email` and `password`.

| Field      | Rules                                  |
|------------|----------------------------------------|
| `email`    | required, valid email format           |
| `password` | required, at least 6 characters        |

### Register — `POST /api/register`

```bash
curl -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=test@example.com&password=mypassword"
```

| Status            | Response body                     | When                         |
|-------------------|-----------------------------------|------------------------------|
| `201 Created`     | `User registered successfully.`   | Account created              |
| `400 Bad Request` | Validation message                | Invalid or missing input     |
| `409 Conflict`    | `Email already registered`        | Email is already registered  |

### Login — `POST /api/login`

```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=test@example.com&password=mypassword"
```

| Status             | Response body                 | When                               |
|--------------------|-------------------------------|------------------------------------|
| `200 OK`           | `Login successful`            | Email and password match           |
| `400 Bad Request`  | Validation message            | Invalid or missing input           |
| `401 Unauthorized` | `Invalid email or password`   | Unknown email or wrong password    |

Unknown email and wrong password return the same message, so the API does not reveal which emails are registered.

### Validation messages

| Case                     | Message                                    |
|--------------------------|--------------------------------------------|
| Email missing            | `Email is required`                        |
| Email format invalid     | `Invalid email format`                     |
| Password missing         | `Password is required`                     |
| Password too short       | `Password must be at least 6 characters`   |

### Testing with Postman

1. Set the method to `POST` and the URL to one of the endpoints above.
2. In the **Body** tab, choose **x-www-form-urlencoded** (not `raw` JSON).
3. Add the keys `email` and `password`.

Register first, then log in. Sending JSON instead of form data returns `415 Unsupported Media Type`.

## H2 Console

Open `http://localhost:8080/h2-console` while the app is running:

- JDBC URL: `jdbc:h2:mem:authdb`
- User: `sa`
- Password: *(empty)*

Run `SELECT * FROM USERS;` to see stored accounts. The `PASSWORD` column holds the BCrypt hash.

## Project Structure

```
src/main/java/com/example/auth/
├── AuthApplication.java
├── config/PasswordConfig.java              # BCrypt PasswordEncoder bean
├── controller/AuthController.java          # /api/register, /api/login
├── dto/AuthRequest.java                    # email, password + validation rules
├── entity/User.java                        # users table
├── repository/UserRepository.java
├── service/AuthService.java                # register and login logic
└── exception/
    ├── DuplicateEmailException.java
    ├── InvalidCredentialsException.java
    └── GlobalExceptionHandler.java         # maps errors to HTTP status + message
src/test/java/com/example/auth/
├── AuthApplicationTests.java
├── controller/AuthControllerTest.java      # endpoint tests with MockMvc
└── service/AuthServiceTest.java            # business logic tests
```

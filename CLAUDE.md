# CLAUDE.md

This file guides Claude Code when working in this repository.

## Project Overview

A Spring Boot RESTful API that provides **register** and **login** endpoints using email and password.
Account data is stored in an in-memory **H2** database. Passwords are hashed with **BCrypt**.

## Tech Stack

- Java 17
- Spring Boot 3.x (Maven)
- Spring Web
- Spring Data JPA
- H2 Database (in-memory)
- Spring Boot Validation (Jakarta Bean Validation)
- `spring-security-crypto` (BCrypt only — do **not** add full `spring-boot-starter-security`, it would lock down the endpoints with default auth)
- Lombok (optional)
- JUnit 5 + MockMvc for tests

## Requirements

1. Provide endpoints for **register** and **login**.
2. Register an account with **email** and **password**.
3. Store account data in memory using **H2**.
4. Passwords must be **securely encrypted** (BCrypt hash; never store or log plain text).
5. **Validate** email and password on login (and on register).
6. Handle basic business errors: **invalid login** and **duplicate email**.

## API Contract

Both endpoints accept `Content-Type: application/x-www-form-urlencoded` and return **plain text** (`text/plain`).

### POST `/api/register`

Request (form fields):

| Field      | Type   | Rules                                  |
|------------|--------|----------------------------------------|
| `email`    | string | required, valid email format           |
| `password` | string | required, not blank (min 6 characters) |

Example:

```bash
curl -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=test@example.com&password=mypassword"
```

Responses:

| Status            | Body                                  | When                      |
|-------------------|---------------------------------------|---------------------------|
| `201 Created`     | `User registered successfully.`       | Success                   |
| `400 Bad Request` | validation message (e.g. `Invalid email format`) | Invalid / missing input |
| `409 Conflict`    | `Email already registered`            | Duplicate email           |

### POST `/api/login`

Request (form fields): `email`, `password` — same validation as register.

Example:

```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=test@example.com&password=mypassword"
```

Responses:

| Status             | Body                          | When                                   |
|--------------------|-------------------------------|----------------------------------------|
| `200 OK`           | `Login successful`            | Email exists and password matches      |
| `400 Bad Request`  | validation message            | Invalid / missing input                |
| `401 Unauthorized` | `Invalid email or password`   | Unknown email **or** wrong password    |

Use the same message for unknown email and wrong password so the API does not reveal which emails are registered.

## Project Structure

```
src/main/java/com/example/auth/
├── AuthApplication.java
├── config/
│   └── PasswordConfig.java          # @Bean PasswordEncoder -> BCryptPasswordEncoder
├── controller/
│   └── AuthController.java          # /api/register, /api/login
├── dto/
│   └── AuthRequest.java             # email, password + validation annotations
├── entity/
│   └── User.java                    # id, email (unique), password (hash), createdAt
├── repository/
│   └── UserRepository.java          # existsByEmail, findByEmail
├── service/
│   └── AuthService.java             # register / login business logic
└── exception/
    ├── DuplicateEmailException.java
    ├── InvalidCredentialsException.java
    └── GlobalExceptionHandler.java  # @RestControllerAdvice -> status + plain text
src/main/resources/application.properties
src/test/java/com/example/auth/      # service unit tests + MockMvc controller tests
```

## Implementation Guidelines

- **Controller**: bind form data with `@Valid @ModelAttribute AuthRequest` (not `@RequestBody`, since the body is form-urlencoded). Use `consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE` and `produces = MediaType.TEXT_PLAIN_VALUE`.
- **Controller stays thin**: all logic lives in `AuthService`.
- **Email normalization**: trim and lowercase the email before saving and looking up, so `Test@Example.com` and `test@example.com` are treated as the same account.
- **Register**: normalize email → check `existsByEmail` → throw `DuplicateEmailException` → hash with `passwordEncoder.encode()` → save. Also add a unique constraint on the `email` column and map `DataIntegrityViolationException` to `409` as a safety net.
- **Login**: normalize email → `findByEmail` → if missing or `!passwordEncoder.matches(raw, hash)` throw `InvalidCredentialsException`.
- **Error handling**: `GlobalExceptionHandler` maps
  - `MethodArgumentNotValidException` / `BindException` → `400` with the first field error message
  - `DuplicateEmailException` → `409`
  - `InvalidCredentialsException` → `401`
- Never return or log the password or its hash.

## Configuration (`application.properties`)

```properties
spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Data is lost when the application stops (in-memory by design).

## Commands

```bash
mvn spring-boot:run      # run the app on http://localhost:8080
mvn test                 # run all tests
mvn clean package        # build the jar
```

H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:authdb`, user `sa`, empty password).

## Testing Checklist

Write tests covering at least:

- Register success → `201` + `User registered successfully.`
- Register duplicate email (including different letter case) → `409`
- Register with invalid email / blank password → `400`
- Login success → `200` + `Login successful`
- Login with wrong password → `401`
- Login with unregistered email → `401`
- Stored password is a BCrypt hash, not the plain text

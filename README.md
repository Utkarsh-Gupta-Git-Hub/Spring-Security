<div align="center">

# 🔐 Spring Security — JWT & Role-Based Access Control

**A focused Spring Boot service demonstrating stateless JWT authentication and fine-grained, permission-based authorization**
*Login → get a token → access is enforced by role and permission, not just "logged in or not."*

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT%20%2B%20RBAC-6DB33F?logo=springsecurity)
![JJWT](https://img.shields.io/badge/JJWT-0.11.5-blueviolet)
![H2](https://img.shields.io/badge/H2-In--Memory%20DB-lightblue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

</div>

---

## 📌 Overview

This project is a **Spring Boot Security reference implementation** built around a small "weather service" domain, focused entirely on getting authentication and authorization right: **stateless JWT-based login**, a **custom `UserDetailsService`**, **BCrypt password hashing**, and a **role-to-permission mapping model** enforced both at the security-filter-chain level and via method-level `@PreAuthorize` checks.

Rather than a feature-heavy app, this is a deliberately scoped, deep dive into how production-grade Spring Security actually works under the hood — the kind of implementation most developers configure once from a tutorial but rarely build from scratch themselves.

> **Why this project matters:** Almost every backend role requires secure authentication and authorization, but many developers can only wire up Spring Security via boilerplate they don't fully understand. This project demonstrates hand-rolled JWT generation/validation, a custom security filter, and a genuine **role → permission** authority model — not just `@PreAuthorize("hasRole(...)")` copied from docs.

---

## ✨ Features

- 🔑 **Stateless JWT Authentication** — Login issues a signed JWT (HS256); no server-side session state
- 🧩 **Custom Security Filter Chain** — A `JwtAuthFilter` (`OncePerRequestFilter`) intercepts every request, validates the bearer token, and populates the Spring Security context
- 👥 **Role-Based Access Control (RBAC)** — `Role` enum (`ADMIN`, `USER`) maps to a set of granular `Permissions` (`WEATHER_READ`, `WEATHER_WRITE`, `WEATHER_DELETE`)
- 🛡️ **Method-Level Security** — `@PreAuthorize("hasRole('ADMIN')")` protects admin-only endpoints, in addition to URL-level rules
- 🔒 **Password Hashing** — All credentials are stored using `BCryptPasswordEncoder`, never in plain text
- 🌱 **Automatic Admin/User Seeding** — A `CommandLineRunner` bootstraps default `admin` and `user` accounts on startup for easy testing
- 🗄️ **Custom `UserDetailsService`** — Loads users from the database via Spring Data JPA and adapts them into Spring Security's `UserDetails` contract
- ⚡ **Zero External Dependencies to Run** — Uses an in-memory H2 database, so the whole security flow can be tested with no setup

---

## 🏗️ Architecture

```
 Client
   │  POST /authenticate {username, password}
   ▼
 AuthController ──► AuthenticationManager ──► DaoAuthenticationProvider
   │                                                 │
   │                                     CustomUserDetailsService (loads Users from DB)
   │                                                 │
   │                                     BCryptPasswordEncoder (verifies password)
   ▼
 JWTUtil.generateToken() → signed JWT returned to client
   │
   │  (client stores token, sends on every subsequent request)
   ▼
 Authorization: Bearer <token>
   │
   ▼
 JwtAuthFilter (OncePerRequestFilter)
   │  validates token → loads UserDetails → sets SecurityContext
   ▼
 SecurityFilterChain (URL rules)  +  @PreAuthorize (method rules)
   │
   ▼
 Protected Controller Endpoint
```

- **`Users`** implements Spring Security's `UserDetails` directly, deriving `GrantedAuthority`s from both the user's role (`ROLE_ADMIN`/`ROLE_USER`) and that role's individual permissions.
- **`JWTUtil`** handles token generation, subject extraction, and expiration validation using the `jjwt` library.
- **`SecurityConfig`** wires the filter chain, exposes `/authenticate` and `/api/users/register` publicly, and requires authentication for everything else.

---

## 🚀 Tech Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.2 |
| Security | Spring Security 6, method security (`@EnableMethodSecurity`) |
| Authentication | JWT (`io.jsonwebtoken` / JJWT 0.11.5), `DaoAuthenticationProvider` |
| Persistence | Spring Data JPA |
| Database | H2 (in-memory, for easy local testing) |
| Password Hashing | BCrypt |
| Build Tool | Maven (with Maven Wrapper) |

---

## 📂 Project Structure

```
weather-service/
├── config/
│   └── SecurityConfig.java          # Filter chain, encoders, AuthenticationManager
├── controller/
│   ├── AuthController.java           # POST /authenticate — issues JWT
│   └── UserController.java           # User registration (self-serve + admin-created)
├── entity/
│   ├── Users.java                    # JPA entity implementing UserDetails
│   ├── Role.java                     # ADMIN / USER → set of Permissions
│   ├── Permissions.java              # WEATHER_READ / WRITE / DELETE
│   ├── AuthRequest.java, RegisterUserRequest.java, UserResponse.java
├── filters/
│   └── JwtAuthFilter.java            # Per-request JWT validation & context population
├── repository/
│   └── UserDetailsRepository.java    # Spring Data JPA repository
├── service/
│   ├── CustomUserDetailsService.java # Loads Users for Spring Security
│   ├── UserService.java              # Registration logic
│   └── AdminUserInitializer.java     # Seeds default admin/user on startup
└── util/
    └── JWTUtil.java                  # Token generation, parsing, validation
```

---

## ⚙️ Getting Started

### Prerequisites

- JDK 21
- Maven 3.9+ (or the included Maven Wrapper)

### 1. Run the Application

```bash
./mvnw spring-boot:run
```

On startup, default accounts are seeded automatically:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin1234` | `ADMIN` |
| `user` | `user1234` | `USER` |

### 2. Authenticate & Get a Token

```bash
curl -X POST http://localhost:8080/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin1234"}'
```

This returns a signed JWT string.

### 3. Call a Protected Endpoint

```bash
curl -X POST http://localhost:8080/api/users/admin/create \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"username": "newuser", "password": "pass1234"}'
```

Only a user with the `ADMIN` role can successfully call this endpoint — a `USER` token will be rejected by `@PreAuthorize("hasRole('ADMIN')")`.

### 4. Register a New (Self-Serve) User

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "someone", "password": "pass1234"}'
```

New self-registered users are always assigned the `USER` role.

---

## 🛣️ Roadmap

- [ ] Move the JWT signing secret out of source code into an environment variable
- [ ] Enable the commented-out `WEATHER_READ`/`WRITE`/`DELETE` permission checks on actual weather endpoints (currently defined but not yet wired to routes)
- [ ] Add refresh-token support so clients aren't forced to re-authenticate every hour
- [ ] Replace H2 with a persistent database (PostgreSQL/MySQL) for non-demo use
- [ ] Add integration tests covering authentication, authorization, and token expiry paths
- [ ] Add global exception handling for cleaner error responses (currently some exceptions bubble up raw)

---

## 🎯 What This Project Demonstrates

- Implementing **stateless JWT authentication** from scratch — token generation, validation, and a custom filter — rather than relying on a pre-built starter
- Designing a genuine **role-to-permission authorization model**, not just role checks
- Understanding the **Spring Security filter chain** and where custom filters plug in relative to built-in ones
- Applying **both URL-level and method-level security** (`@PreAuthorize`) in the same application
- Proper **credential hygiene**: BCrypt hashing, no plaintext passwords anywhere in the persistence layer

---

## 👤 Author

**Utkarsh Gupta**
Backend Developer | Java & Spring Boot | Spring Security

---

## 📄 License

This project is available under the [MIT License](LICENSE).

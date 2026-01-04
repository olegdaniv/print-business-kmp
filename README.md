# PrintBusinessKmp

A **Kotlin Multiplatform (KMP)** project for managing a print business, targeting Web and Server platforms.

## Project Structure

This project consists of three main modules:

- **backend**: Ktor server application (JVM target, runs on port 8080)
- **webApp**: Compose Multiplatform web application (JS and Wasm targets, runs on port 8081)
- **shared**: Shared business logic, models, and constants (JVM, JS, and Wasm targets)

```
backend/       - Ktor server with REST API
webApp/        - Compose Multiplatform UI (JS/Wasm browsers)
shared/        - Shared code including models, constants, and utilities
```

## Technologies

- **Kotlin**: 2.3.0
- **Ktor**: 3.3.3 (server framework)
- **Compose Multiplatform**: 1.9.3 (web UI)
- **kotlinx.serialization**: 1.10.0-RC (JSON serialization)
- **Exposed**: 0.58.0 (database ORM)
- **H2**: 2.3.232 (embedded database)

## Quick Start

### Prerequisites

- JDK 17 or higher
- Node.js (for web app development)

### Run the Application

1. **Start the Backend Server** (port 8080):
   ```bash
   ./gradlew :backend:run
   ```

2. **Start the Web Application** (port 8081):

   For Wasm target (faster, modern browsers):
   ```bash
   ./gradlew :webApp:wasmJsBrowserDevelopmentRun
   ```

   For JS target (broader browser support):
   ```bash
   ./gradlew :webApp:jsBrowserDevelopmentRun
   ```

3. Open your browser and navigate to http://localhost:8081/

## Build Commands

### Backend (Ktor Server)

```bash
# Run development server
./gradlew :backend:run

# Build backend
./gradlew :backend:build

# Run backend tests
./gradlew :backend:test
```

### Web Application

```bash
# Run Wasm target (faster, modern browsers only)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Run JS target (broader browser support)
./gradlew :webApp:jsBrowserDevelopmentRun

# Build for production
./gradlew :webApp:wasmJsBrowserDistribution  # Wasm
./gradlew :webApp:jsBrowserDistribution       # JS

# Run web app tests
./gradlew :webApp:wasmJsTest  # or :webApp:jsTest
```

### Shared Module

```bash
# Run shared module tests (all platforms)
./gradlew :shared:allTests

# Run platform-specific tests
./gradlew :shared:jvmTest
./gradlew :shared:jsTest
./gradlew :shared:wasmJsTest

# Build shared module
./gradlew :shared:build
```

### Project-Wide

```bash
# Build all modules
./gradlew build

# Run all tests across all modules
./gradlew test

# Clean build artifacts
./gradlew clean
```

## Features

- Client management (CRUD operations)
- Order management with items
- Cost and profit calculations
- Invoice generation tracking
- REST API with CORS support
- Responsive web UI using Compose Multiplatform

## API Endpoints

The backend exposes the following REST API endpoints:

- `GET /` - Server status
- `GET /health` - Health check
- `GET /api/clients` - List all clients
- `GET /api/clients/{id}` - Get client by ID
- `POST /api/clients` - Create new client
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Delete client
- `GET /api/orders` - List all orders
- `GET /api/orders/{id}` - Get order by ID
- `POST /api/orders` - Create new order
- `PUT /api/orders/{id}/status` - Update order status

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Ktor](https://ktor.io/)
- [Kotlin/Wasm](https://kotl.in/wasm/)
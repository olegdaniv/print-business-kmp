# PrintBusinessKmp

A comprehensive **Kotlin Multiplatform (KMP)** project for managing a print business, targeting Web, Desktop, and Server platforms.

## Project Structure

This project consists of four main modules:

- **`backend`**: Ktor server application (JVM target, runs on port 8080) serving the REST API.
- **`webApp`**: Compose Multiplatform web application (JS and Wasm targets, runs on port 8081).
- **`desktopApp`**: Compose Multiplatform desktop application (JVM target, natively packaged for Windows).
- **`shared`**: Shared core business logic, models, networking, and constants utilized across all platforms.

```text
.
├── backend/            # Ktor server with REST API
├── webApp/             # Compose Multiplatform UI (JS/Wasm browsers)
├── desktopApp/         # Compose Multiplatform UI (Desktop/Windows)
├── shared/             # Shared code including models, constants, and utilities
├── Dockerfile          # Production Dockerfile for the backend
└── docker-compose.yml  # Local deployment environment (Backend + Postgres)
```

## Technologies Used

- **Kotlin**: 2.3.0
- **Ktor**: 3.3.3 (Backend server framework)
- **Compose Multiplatform**: 1.9.3 (Web & Desktop UI)
- **kotlinx.serialization**: 1.10.0-RC (JSON serialization)
- **Exposed**: 0.58.0 (Database ORM)
- **Database**: H2 (Embedded for dev/testing) & PostgreSQL (Production/Docker)

---

## 🚀 Quick Start (Local Development)

### Prerequisites
- **JDK 17** or higher
- **Node.js** (required for web app development)
- **Docker & Docker Compose** (optional, but recommended for running the database)

### 1. Start the Backend Server
You can run the backend server either natively via Gradle or through Docker.

**Option A: Run via Gradle (Port 8080)**
```bash
./gradlew :backend:run
```

**Option B: Run via Docker Compose (Backend + PostgreSQL)**
This will start both the PostgreSQL database and the Ktor application.
```bash
docker-compose up -d
```

### 2. Start the Web Application
The web app runs on port `8081`. You can run it using the modern Wasm target or the broader Javascript target.

```bash
# For Wasm target (faster, requires modern browsers)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# For JS target (broader browser compatibility)
./gradlew :webApp:jsBrowserDevelopmentRun
```
Access the web app at [http://localhost:8081/](http://localhost:8081/).

### Google Sign-In Configuration (Web)
The web app resolves the Google client ID in this order:
1. `window.__PRINTBUSINESS_GOOGLE_CLIENT_ID` in `index.html` (runtime override)
2. Backend runtime config from `GET /auth/google/client-id` (uses backend `GOOGLE_CLIENT_ID`)
3. Build-time fallback from `-Pprintbusiness.google.clientId=...`
4. Build-time fallback from `GOOGLE_CLIENT_ID` in environment or root `.env`

For local development, setting backend `GOOGLE_CLIENT_ID` in `.env` is usually enough.

### Google Sign-In Configuration (Desktop + Backend)
If you use both web and desktop sign-in, keep separate OAuth client IDs:

- `GOOGLE_CLIENT_ID`: **Web application** OAuth client ID (used by web UI and backend default audience)
- `GOOGLE_DESKTOP_CLIENT_ID`: **Desktop app** OAuth client ID (used by desktop PKCE flow; backend also accepts it)
- `desktopGoogleClientId` in `gradle.properties`: desktop client ID passed to `:desktopApp:run`

This allows backend token verification for both app types while keeping web and desktop credentials independent.

### 3. Start the Desktop Application
To run the Windows/Desktop Compose application locally:

```bash
./gradlew :desktopApp:run
```

---

## ⚙️ API Configuration (Local vs Production)

To allow the frontend (`webApp`, `desktopApp`) to dynamically connect to either your local backend or a deployed production endpoint, the project uses **BuildKonfig** to generate API endpoints at compile-time.

By default, the endpoints bind to the properties inside your `gradle.properties`:
```properties
printbusiness.api.host=localhost
printbusiness.api.port=8080
printbusiness.api.scheme=http
```

### Overriding for Production Builds

When you build your application for production (e.g. via GitHub Actions or Render), you should override these properties using standard Gradle command-line flags so the UI targets your real backend domain:

```bash
# Example: Building the Web App for production
./gradlew :webApp:wasmJsBrowserDistribution \
  -Pprintbusiness.api.scheme=https \
  -Pprintbusiness.api.host=print-business-kmp.onrender.com \
  -Pprintbusiness.api.port=443
```

---

## 📦 Build & Test Commands

### Project-Wide
```bash
./gradlew build    # Build all modules
./gradlew test     # Run all tests across all modules
./gradlew clean    # Clean build artifacts
```

### Backend (Ktor Server)
```bash
./gradlew :backend:build
./gradlew :backend:test
```

### Web Application
```bash
./gradlew :webApp:wasmJsBrowserDistribution  # Build for production (Wasm)
./gradlew :webApp:jsBrowserDistribution       # Build for production (JS)
./gradlew :webApp:wasmJsTest                  # Run web app tests
```

### Desktop Application
```bash
./gradlew :desktopApp:packageMsi  # Package as an MSI installer (Windows)
```

### Shared Module
```bash
./gradlew :shared:build
./gradlew :shared:allTests       # Run shared module tests (all platforms)
./gradlew :shared:jvmTest        # Run JVM-specific tests
./gradlew :shared:jsTest         # Run JS-specific tests
./gradlew :shared:wasmJsTest     # Run Wasm-specific tests
```

---

## 💻 Features

- **Client Management**: Full CRUD operations for clients.
- **Order Management**: Track orders, line items, and their statuses.
- **Financials**: Cost, profit calculations, and invoice tracking.
- **Cross-Platform UI**: Beautiful, responsive UI built completely in Compose Multiplatform.
- **REST API**: Robust Ktor backend with CORS support and database integration.

---

## 🔄 Desktop Windows CI/CD and Updates

The project is fully configured to automatically build and release updates for the Windows Desktop App. 

### GitHub Actions Workflows

1. **CI Workflow (`ci.yml`)**: 
   - Triggers on PRs and pushes to `main`.
   - Validates the Gradle wrapper, runs tests, and assembles the desktop app.
2. **Release Workflow (`release-windows.yml`)**: 
   - Triggers on version tags (`vX.Y.Z`).
   - Builds the Windows MSI on `windows-latest`.
   - Packages the app with update feed configurations bound to GitHub Pages.
   - Publishes the MSI to GitHub Releases.
   - Publishes the updater JSON feed strictly to `gh-pages` so the app can auto-update.

### How To Release a New Version

1. Bump the `desktopAppVersion` property in `gradle.properties`.
2. Commit and push the version bump to `main`.
3. Create and push a release tag matching the version:
   ```bash
   git tag -a vX.Y.Z -m "Release notes for X.Y.Z"
   git push origin vX.Y.Z
   ```
4. GitHub Actions will automatically take care of building the `.msi`, creating the GitHub release, and updating the update feed on your GitHub Pages deployment.

### How The Updater Works
The installed desktop app reads its target feed URL (e.g., `https://<owner>.github.io/<repo>/updates/latest.json`) on startup. If a newer version exists in the feed, it prompts the user, downloads the new MSI, and seamlessly executes the upgrade flow.

---

## 🌐 API Endpoints

The backend exposes the following REST API endpoints:

- `GET /` & `GET /health` - Server status & health check
- `GET /api/clients` - List all clients
- `GET /api/clients/{id}` - Get client by ID
- `POST /api/clients` - Create new client
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Delete client
- `GET /api/orders` - List all orders
- `GET /api/orders/{id}` - Get order by ID
- `POST /api/orders` - Create new order
- `PUT /api/orders/{id}/status` - Update order status

---

## 📚 Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Ktor](https://ktor.io/)
- [Kotlin/Wasm](https://kotl.in/wasm/)

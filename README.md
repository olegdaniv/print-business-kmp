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
├── deploy/             # Production Dockerfiles and docker-compose for deployment
├── Dockerfile          # Production Dockerfile for the backend
└── docker-compose.yml  # Local deployment environment (Backend + Postgres)
```

## Technologies Used

- **Kotlin**: 2.3.0
- **Ktor**: 3.3.3 (Backend server framework)
- **Compose Multiplatform**: 1.9.3 (Web & Desktop UI)
- **kotlinx.serialization**: 1.10.0-RC (JSON serialization)
- **Exposed**: 0.58.0 (Database ORM)
- **Flyway**: Database migrations (PostgreSQL/Docker)
- **Database**: H2 (Embedded for local dev) & PostgreSQL (Production/Docker)
- **Auth**: Google OAuth 2.0 + JWT

---

## Quick Start (Local Development)

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

### 3. Start the Desktop Application

To run the Desktop Compose application locally:

```bash
./gradlew :desktopApp:run
```

---

## Authentication

The project uses **Google OAuth 2.0** for sign-in with **JWT** tokens for API authorization.

### Environment Variables

| Variable | Description |
|---|---|
| `JWT_SECRET` | Secret key for signing JWT tokens |
| `JWT_ISSUER` | JWT issuer claim |
| `JWT_AUDIENCE` | JWT audience claim |
| `GOOGLE_CLIENT_ID` | Web application OAuth client ID (used by web UI and backend) |
| `GOOGLE_DESKTOP_CLIENT_ID` | Desktop app OAuth client ID (used by desktop PKCE flow) |
| `GOOGLE_DESKTOP_CLIENT_SECRET` | (Optional) Desktop app OAuth client secret |
| `GOOGLE_DESKTOP_REDIRECT_PORT` | Port for local desktop OAuth redirect loopback server |
| `GOOGLE_DESKTOP_REDIRECT_HOST` | Override redirect host (default: `localhost`) |
| `ALLOWED_EMAILS` | CSV list of allowed email addresses (fallback) |
| `CORS_ALLOWED_ORIGINS` | CSV list of allowed CORS origins |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection (Docker/production) |

### Google Sign-In (Web)

The web app resolves the Google client ID in this order:

1. `window.__PRINTBUSINESS_GOOGLE_CLIENT_ID` in `index.html` (runtime override)
2. Backend runtime config from `GET /auth/google/client-id` (uses backend `GOOGLE_CLIENT_ID`)
3. Build-time fallback from `-Pprintbusiness.google.clientId=...`
4. Build-time fallback from `GOOGLE_CLIENT_ID` in environment or root `.env`

For local development, setting backend `GOOGLE_CLIENT_ID` in `.env` is usually enough.

### Google Sign-In (Desktop)

If you use both web and desktop sign-in, keep separate OAuth client IDs:

- `GOOGLE_CLIENT_ID`: **Web application** OAuth client ID
- `GOOGLE_DESKTOP_CLIENT_ID`: **Desktop app** OAuth client ID (used by desktop PKCE flow; backend also accepts it)

If desktop sign-in shows `Error 400: redirect_uri_mismatch`, use a Google **Desktop app** OAuth client ID for `GOOGLE_DESKTOP_CLIENT_ID`. If you must use a Web OAuth client, set a fixed `GOOGLE_DESKTOP_REDIRECT_PORT` and register the exact loopback redirect URI.

---

## API Configuration (Local vs Production)

To allow the frontend (`webApp`, `desktopApp`) to dynamically connect to either your local backend or a deployed production endpoint, the project uses **BuildKonfig** to generate API endpoints at compile-time.

By default, the endpoints bind to the properties inside your `gradle.properties`:

```properties
printbusiness.api.host=localhost
printbusiness.api.port=8080
printbusiness.api.scheme=http
```

### Overriding for Production Builds

When you build your application for production (e.g. via GitHub Actions or Render), override these properties using Gradle command-line flags:

```bash
./gradlew :webApp:wasmJsBrowserDistribution \
  -Pprintbusiness.api.scheme=https \
  -Pprintbusiness.api.host=print-business-kmp.onrender.com \
  -Pprintbusiness.api.port=443
```

---

## Build & Test Commands

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
./gradlew :desktopApp:run          # Run locally
./gradlew :desktopApp:packageMsi   # Package as an MSI installer (Windows)
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

## API Endpoints

### Public

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Server status |
| `GET` | `/health` | Health check |
| `GET` | `/auth/google/client-id` | Get Google client ID |
| `POST` | `/auth/google` | Exchange Google ID token for JWT |

### Authenticated (requires JWT)

**Clients**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/clients` | List all clients |
| `GET` | `/api/clients/{id}` | Get client by ID |
| `POST` | `/api/clients` | Create new client |
| `PUT` | `/api/clients/{id}` | Update client |
| `DELETE` | `/api/clients/{id}` | Delete client |

**Orders**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/orders` | List all orders |
| `GET` | `/api/orders/{id}` | Get order by ID |
| `POST` | `/api/orders` | Create new order |
| `PUT` | `/api/orders/{id}` | Update order |
| `PATCH` | `/api/orders/{id}/state` | Update order state |
| `DELETE` | `/api/orders/{id}` | Delete order |

**Partners**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/partners` | List all partners |
| `GET` | `/api/partners/{id}` | Get partner by ID |
| `POST` | `/api/partners` | Create new partner |
| `PUT` | `/api/partners/{id}` | Update partner |
| `DELETE` | `/api/partners/{id}` | Delete partner |

**Outsource Jobs**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/outsource-jobs` | List all outsource jobs |
| `GET` | `/api/outsource-jobs/{id}` | Get outsource job by ID |
| `POST` | `/api/outsource-jobs` | Create new outsource job |
| `PUT` | `/api/outsource-jobs/{id}` | Update outsource job |
| `DELETE` | `/api/outsource-jobs/{id}` | Delete outsource job |

**Invoices**

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/invoices/generate/{orderId}` | Generate invoice for order |
| `GET` | `/api/invoices/{id}` | Get invoice by ID |
| `GET` | `/api/invoices/order/{orderId}` | Get invoices for order |
| `GET` | `/api/invoices/download/{id}` | Download invoice file |
| `DELETE` | `/api/invoices/{id}` | Delete invoice |

**Layouts**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/layouts` | List all layouts |
| `GET` | `/api/layouts/{id}` | Get layout by ID |
| `POST` | `/api/layouts` | Create new layout |
| `PUT` | `/api/layouts/{id}` | Update layout |
| `DELETE` | `/api/layouts/{id}` | Delete layout |

**Business Profile**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/business-profile` | Get business profile |
| `PUT` | `/api/business-profile` | Create or update business profile |

**Pricing**

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/pricing/calculate` | Calculate pricing |

**Admin**

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/allowed-emails` | List allowed emails |
| `POST` | `/admin/allowed-emails` | Add allowed email |
| `DELETE` | `/admin/allowed-emails/{email}` | Remove allowed email |

---

## CI/CD

### GitHub Actions Workflows

1. **CI (`ci.yml`)**: Triggers on PRs and pushes to `main`. Validates the Gradle wrapper, runs tests, and assembles the desktop app.
2. **Release Windows (`release-windows.yml`)**: Triggers on version tags (`vX.Y.Z`). Builds the Windows MSI, publishes to GitHub Releases, and updates the updater JSON feed on `gh-pages`.
3. **Build & Push Backend (`build-push-backend.yml`)**: Builds and pushes the backend Docker image.
4. **Build & Push Web (`build-push-web.yml`)**: Builds and pushes the web app Docker image.

### How To Release a New Desktop Version

1. Bump the `desktopAppVersion` property in `gradle.properties`.
2. Commit and push the version bump to `main`.
3. Create and push a release tag:

   ```bash
   git tag -a vX.Y.Z -m "Release notes for X.Y.Z"
   git push origin vX.Y.Z
   ```

4. GitHub Actions will automatically build the `.msi`, create the GitHub release, and update the update feed.

### Desktop Auto-Updater

The installed desktop app reads its target feed URL (e.g., `https://<owner>.github.io/<repo>/updates/latest.json`) on startup. If a newer version exists, it prompts the user, downloads the new MSI, and executes the upgrade.

---

## Features

- **Client Management**: Full CRUD operations for clients.
- **Order Management**: Track orders, line items, statuses, and state transitions.
- **Partner & Outsource Tracking**: Manage partners and outsourced jobs.
- **Invoice Generation**: Generate and download invoices for orders.
- **Layout Management**: Store and manage print layouts.
- **Pricing Calculator**: Calculate pricing for print jobs.
- **Business Profile**: Configure business details for invoices and branding.
- **Email Allowlist**: Admin-controlled access via email allowlisting.
- **Cross-Platform UI**: Compose Multiplatform UI for Web and Desktop.
- **Desktop Auto-Updates**: Built-in update mechanism for Windows desktop app.
- **Google OAuth**: Secure authentication with Google Sign-In.

---

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Ktor](https://ktor.io/)
- [Kotlin/Wasm](https://kotl.in/wasm/)
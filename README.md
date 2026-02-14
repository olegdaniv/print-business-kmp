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

## Desktop Windows CI/CD and Updates

### GitHub Actions Workflows

- CI workflow: `/Users/oleh/kmp/print-business-kmp/.github/workflows/ci.yml`
  - triggers on pull requests and pushes to `main`
  - runs `./gradlew test`
  - runs `./gradlew :desktopApp:assemble`

- Release workflow: `/Users/oleh/kmp/print-business-kmp/.github/workflows/release-windows.yml`
  - triggers on tags `vX.Y.Z`
  - builds Windows MSI on `windows-latest`
  - auto-detects MSI packaging task (`:desktopApp:packageMsi` or fallback)
  - packages with:
    - `desktopUpdateFeedUrl=https://<USER>.github.io/<REPO>/updates/latest.json`
    - `desktopUpdateAllowedHosts=<USER>.github.io`
    - `desktopAllowUpdatesWithoutChecksum=false`
  - publishes MSI to GitHub Release
  - publishes `/updates/latest.json` and `/updates/PrintBusiness-X.Y.Z.msi` to `gh-pages`
  - uses only `GITHUB_TOKEN` (no extra secrets required)

### Update Feed URL

Enable GitHub Pages to serve from branch `gh-pages` (root).

Stable feed URL format:

- project pages: `https://<owner>.github.io/<repo>/updates/latest.json`
- user/org pages repo (`<owner>.github.io`): `https://<owner>.github.io/updates/latest.json`

### How To Release

1. Bump desktop app version (`desktopAppVersion`) in `/Users/oleh/kmp/print-business-kmp/gradle.properties`.
2. Commit and push to `main`.
3. Create a release tag:
   ```bash
   git tag -a vX.Y.Z -m "Release notes for X.Y.Z"
   git push origin vX.Y.Z
   ```
4. GitHub Actions will:
   - build `PrintBusiness-X.Y.Z.msi`
   - compute SHA256
   - publish GitHub Release with MSI asset
   - update GitHub Pages feed at `/updates/latest.json`
5. Verify feed and artifact URLs:
   - `https://<owner>.github.io/<repo>/updates/latest.json`
   - `https://<owner>.github.io/<repo>/updates/PrintBusiness-X.Y.Z.msi`

### Local Feed Generation (Optional)

Generate `latest.json` locally:

```powershell
pwsh ./scripts/generate-latest-json.ps1 `
  -Version "1.2.3" `
  -Notes "Bug fixes" `
  -MsiUrl "https://<owner>.github.io/<repo>/updates/PrintBusiness-1.2.3.msi" `
  -Sha256 "<SHA256>" `
  -OutputPath "./latest.json"
```

### Test Updates Locally

1. Build an older MSI and install it.
2. Build a newer MSI with production-like feed settings:
   ```bash
   ./gradlew :desktopApp:packageMsi \
     -PdesktopAppVersion=1.2.3 \
     -PdesktopUpdateFeedUrl=https://<owner>.github.io/<repo>/updates/latest.json \
     -PdesktopUpdateAllowedHosts=<owner>.github.io \
     -PdesktopAllowUpdatesWithoutChecksum=false
   ```
3. Upload `updates/latest.json` and `updates/PrintBusiness-1.2.3.msi` to `gh-pages` (or use the tag-based release workflow).
4. Open installed older app, go to Updates screen, check for updates, and run upgrade flow.

### How App Checks Updates

The desktop app reads the feed URL from `desktopUpdateFeedUrl` (Gradle property at build/package time), checks `latest.json` on startup and from the Updates screen, and if a newer version exists it downloads the MSI and launches installer-based upgrade.

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

# Windows MSI Packaging + In-App Updates

## Build prerequisites
- Build MSI on a Windows machine (or Windows CI runner).
- Keep `desktopAppVersion` in `gradle.properties` in SemVer format (`MAJOR.MINOR.PATCH`).

## Build commands
- Run desktop app (dev): `./gradlew :desktopApp:run`
- Build MSI: `./gradlew :desktopApp:packageMsi`
- Print SHA-256 for feed: `./gradlew :desktopApp:printMsiSha256`

## Update feed format (`latest.json`)
```json
{
  "version": "1.2.3",
  "notes": "Bug fixes, invoice improvements",
  "windows": {
    "url": "https://updates.example.com/printbusiness/updates/PrintBusiness-1.2.3.msi",
    "sha256": "4fd8d4a3356eb7aa8d42f1b4069f99f8a55f83c58e5f56f5c3df2bf42a5c8f66"
  }
}
```

## Hosting layout
- `/printbusiness/updates/latest.json`
- `/printbusiness/updates/PrintBusiness-1.2.3.msi`

## Release checklist
1. Bump `desktopAppVersion` in `gradle.properties`.
2. Build MSI on Windows: `./gradlew :desktopApp:packageMsi`.
3. Rename/upload MSI to `PrintBusiness-<version>.msi`.
4. Generate SHA-256 via `./gradlew :desktopApp:printMsiSha256`.
5. Update/upload `latest.json` with version, notes, URL, and SHA-256.
6. Verify on clean Windows machine:
   - install older MSI
   - open app and check Updates screen
   - download and install update
   - confirm new version is shown after relaunch

## Runtime settings (optional)
The launcher passes these JVM properties from Gradle:
- `printbusiness.app.name`
- `printbusiness.app.version`
- `printbusiness.update.feedUrl`
- `printbusiness.update.allowedHosts`
- `printbusiness.update.allowWithoutChecksum`

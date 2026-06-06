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

## Automated releases (GitHub Actions)
Releases are produced automatically by `.github/workflows/release-windows.yml`.

- **Trigger:** any push to `main` that touches `desktopApp/**`, `shared/**`, or the
  Gradle build files. (Can also be run manually via *Actions → Release Windows MSI →
  Run workflow*.)
- **Versioning:** `MAJOR.MINOR` come from `desktopAppVersion` in `gradle.properties`;
  the **patch is auto-incremented** from the highest existing `vMAJOR.MINOR.*` tag.
  Example: last tag `v1.0.9` → next release `v1.0.10`.
- **What the workflow does:** builds the MSI on Windows, computes SHA-256, creates the
  Git tag + GitHub Release (MSI attached), and publishes `latest.json` to the
  `gh-pages` branch — the feed the app polls.
- **Release notes:** taken from the triggering commit message.

### To cut a normal release
Just push your desktop changes to `main`. A new patch version ships automatically.

### To bump minor/major
Edit `desktopAppVersion` in `gradle.properties` (e.g. `1.0.0` → `1.1.0`) and push.
The next release becomes `v1.1.0` (patch resets to the lowest unused value).

### Verify (on a clean Windows machine)
- install an older MSI
- open app → Updates screen
- download and install the update
- confirm the new version is shown after relaunch

## Runtime settings (optional)
The launcher passes these JVM properties from Gradle:
- `printbusiness.app.name`
- `printbusiness.app.version`
- `printbusiness.update.feedUrl`
- `printbusiness.update.allowedHosts`
- `printbusiness.update.allowWithoutChecksum`

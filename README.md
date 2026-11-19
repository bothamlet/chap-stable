# Chap

Chap is an offline Android app for envelope budgeting. Set a spending limit for each envelope, log expenses, and see what's left for the current budget period. No account or network access is required.

![Chap budgeting screens](assets/chap-card.webp)

## Run

Use JDK 17 and Android SDK 36. Open the project in Android Studio, or set `ANDROID_HOME` to your SDK directory.

```sh
./gradlew assembleDebug
./gradlew installDebug   # connected phone or emulator
```

The app runs on Android 8.0 or newer. To try an import without using a bank statement, use the [sample CSV](test-data/small-budget-import.csv).

## How it works

- **Budgeting:** Fixed and variable spending envelopes, split expenses, and balance tracking by budget period. CSV imports validate the whole file before saving transactions.
- **Data:** Room and SQLCipher store the budget on the device. An Android Keystore key protects encrypted backups. A backup can restore an in-app wipe, but not an uninstall or a move to another phone.
- **Interface:** Kotlin and Jetpack Compose, with screen state in view models. The repository handles data access, and amounts are stored as integer minor units (such as cents).

Chap tracks spending only. It skips income rows on import and does not sync with banks or the cloud. See [import and recovery details](docs/operations.md).

## Checks

```sh
./gradlew testDebugUnitTest lintDebug assembleRelease
./gradlew connectedDebugAndroidTest   # connected phone or emulator
```

Release APKs are unsigned. Release builds require device authentication and block screen capture; debug builds allow screen capture and can skip authentication on emulators.

The bundled [Space Grotesk font](app/src/main/assets/licenses/space-grotesk.txt) is licensed under the SIL Open Font License.

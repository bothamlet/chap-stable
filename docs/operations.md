# Data, imports, and recovery

## Budget periods and timestamps

Transaction timestamps represent calendar date/time encoded in UTC. Changing the device timezone
does not move transactions between budget periods. Database version 5 converts legacy timestamps
using the device timezone at upgrade.

Amounts are stored and passed as `Long` minor units. `ProfileFormatters.formatMoney` handles
display formatting.

## CSV import

Imports support comma, semicolon, and tab separators; quoted multiline fields; strict date and
money parsing; and repeat-import detection independent of row order. Ambiguous slash dates use
month/day/year. A malformed required field rejects the entire import without retaining partial data.

Positive income/credit rows are ignored and counted in the import result. Manual entry records
expenses only; previously stored income is hidden from the inbox.

Reimporting a statement first imported with an older version can create duplicates.
Without a bank reference, identical rows are matched by occurrence within the file.
Check overlapping statements for duplicate transactions.

## Backup and recovery

Encrypted backup files require the original installation's Android Keystore key. They can restore
data after an in-app wipe but not after uninstalling the app or replacing the phone. These files
are not portable recovery backups. Cloud backup and restore are unavailable.

Legacy goal records remain in storage for backup compatibility. Goal creation and goal sections
are hidden in the current version.

## Debug and release behavior

Debug builds set `ALLOW_EMULATOR_BIOMETRIC_BYPASS=true` and `ALLOW_SCREEN_CAPTURE=true`.
Authentication bypass applies only to detected emulators; screen capture is available for debugging.
Release builds disable both flags, require device authentication, and retain `FLAG_SECURE`.
No release signing configuration is provided.

For device tests, start an Android emulator from Android Studio or connect a phone.

# AGENTS.md

Nickel: a single-module Android expense tracker (Kotlin, Jetpack Compose, Material 3 Expressive). Two Gradle modules: `:app` and `:baselineprofile` (an instrumentation module that generates the baseline profile).

## Commands

```sh
./gradlew installDebug        # build and install the debug app on a connected device
./gradlew testDebugUnitTest   # run the JVM unit tests (the only tests)
./gradlew assembleRelease     # release build (R8-minified; signed with the debug key)
./gradlew :app:generateReleaseBaselineProfile   # regenerates app/src/release/generated/baselineProfiles/ (the task lives on :app, not :baselineprofile)
```

Environment requirements (not checked in):
- Android SDK with platform 37.2 (Android 17 QPR2; `sdk.dir` in `local.properties` or `ANDROID_HOME`)
- A connected device or emulator for installDebug and profile generation; profile generation uses a physical device only (`useConnectedDevices = true` in `baselineprofile/build.gradle.kts`), no emulator images
- The Gradle daemon JVM (JDK 25, pinned in `gradle/gradle-daemon-jvm.properties`) is auto-downloaded via the foojay resolver, so no local JDK setup beyond that

Version catalog is `gradle/libs.versions.toml`; all plugins/dependencies go through it.

## Architecture

No DI framework, no Room, no navigation library. Data flows one way through `StateFlow`s.

- **`MainActivity.kt`** is the only activity and handles all navigation: two screens (`HOME`, `SETTINGS`) switched via a `SeekableTransitionState` that also powers the predictive-back gesture. Add a screen by adding an enum entry and a `when` branch here, not by adding a navigation dependency.
- **`ui/NickelViewModel.kt`** owns all app state. It builds one combined `StateFlow<HomeState>` from ledger + filter + category filter (computed on `Dispatchers.Default` so filtering a large ledger doesn't drop frames). Side effects out of the ViewModel go through `Channel`s: `messages` (snackbar strings) and `shareExports` (CSV files). Import is a two-phase state machine (`TransferState`: Idle → Reading → Preview → Importing) so nothing is written until the user confirms.
- **`data/`** is plain Kotlin (no Android framework in the pure logic):
  - `Models.kt` — `Transaction`, `Category`, `Kind`, `Tone`, the built-in category list, and string-keyed registries (`CategoryIcons`, `CategoryShapes`)
  - `TransactionRepository.kt` — the whole ledger lives in two JSON files (`transactions.json`, `categories.json`), rewritten in full on every mutation under a `Mutex`. There is deliberately no database; don't introduce one without cause.
  - `Csv.kt` — a hand-written RFC 4180 parser plus `LedgerCsv.plan()` (import planning) and `LedgerCsv.export()`. `plan()` is pure and takes the existing ledger as arguments, which is what makes it unit-testable on the JVM.
  - `Money.kt` — a global singleton (`Money.chosenCurrency` is Compose snapshot state) applied by `SettingsRepository`'s `init`, so app and widget format identically.
  - `SettingsRepository.kt` — SharedPreferences: dynamic color and currency code.
- **`backup/`** — automatic CSV backups to a user-picked folder through a persisted Storage Access Framework tree grant (no storage permission). `LedgerBackup` writes one `nickel-backup-YYYY-MM-DD.csv` per day, keeps the newest 7 and never writes an empty ledger. `BackupWorker` is the periodic WorkManager job. Naming and pruning rules live in `data/Backups.kt` so they're unit-testable. The first-run prompt is on Home, and the folder and interval rows are in Settings → Your data.
- **`widget/`** — two Glance home-screen widgets (`NickelWidget` for the overall balance, `TodayWidget` for today) sharing one layout (`WidgetContent`), both reading the ledger files directly. Every ledger change after first load and every currency change redraws them through `updateNickelWidgets()`. Add new widgets there, not at call sites. Each widget constructs a `SettingsRepository` in `provideGlance` to apply the currency. The Today receiver also redraws on date, time and time-zone changes, and its provider info asks for a 30-minute refresh so the new day still shows up if Android doesn't deliver DATE_CHANGED.

## Data model gotchas

- **Money is always `amountCents: Long`, always positive.** Sign lives in `Kind` (`INCOME`/`EXPENSE`); `Transaction.signedCents` derives it. Never store negative amounts.
- **Keypad input is a digit string filling from the cents up**: `"449"` = 4.49, `"18945528"` = 189,455.28 (`Money.parseInput`). Formatting always shows two decimals even for currencies with other minor units.
- **Built-in categories keep stable string ids** (e.g. `PAYCHECK`, `GROCERIES`) so old ledgers keep loading. Custom categories get `custom:income:label-slug` ids, with icon/shape/tone derived deterministically from the label hash. Icons and shapes are stored as registry key strings, never as objects, so `Category` stays plain data and saveable across configuration changes. Unknown keys fall back to a default rather than crashing.
- **Ledger JSON corruption is tolerated**: reads are wrapped in `runCatching` and fall back to empty. Writes are temp-file-then-rename, and categories are written before transactions so a crash can't leave transactions pointing at unsaved categories.
- **Frosted glass needs QPR2 (API 37.2)**, but `minSdk` can't name a minor level, so it's 37 and everything glass is gated on `GlassSupported` (`ui/components/Glass.kt`), falling back to solid tonal fills. `Modifier.glass` blurs only what's drawn in the same window. The entry sheet is its own window, so Home blurs *itself* behind it (driven by `EntrySheet`'s `onFrost`). Never use `FLAG_BLUR_BEHIND` for it: the system adds a dim layer it owns, and Android drops touches through it as untrusted, so the list can't scroll while the sheet closes.
- **No network permission** — the app is privacy-first by design. Don't add internet access, analytics, or dependencies that pull in network code.

## Build gotchas

- **Release is signed with the debug keystore** (no release keystore exists) so it installs straight from Android Studio. The debug build has `applicationIdSuffix = ".debug"` and installs alongside release as a separate app (wrench launcher icon from `app/src/debug/`).
- **Baseline-profile and benchmark variants are rebranded to `xyz.draumaz.nickel.profiling`** (see `androidComponents` block in `app/build.gradle.kts`). This is load-bearing: the profiler tooling uninstalls its app when done, and under the real id that would wipe the user's ledger. Profile rules match classes, not the package.
- **WorkManager is pinned explicitly** in `libs.versions.toml` because Glance's transitive (ancient) WorkManager version has R8 keep-rule gaps that crash release builds at startup. Don't remove that pin.
- **Experimental Material 3 / Expressive APIs are compiler opt-ins** in `app/build.gradle.kts` (`kotlin { compilerOptions { optIn.addAll(...) } }`), not per-file annotations. Uses Material 3 `1.5.0-beta02`; Expressive APIs (MaterialShapes, motionScheme, etc.) are in active use.

## Testing

Only JVM unit tests exist, in `app/src/test/java/.../data/` (JUnit 4): `MoneyTest` and `LedgerCsvTest`. They test the pure `data/` layer directly — no Robolectric, no instrumentation tests. Keep new pure logic in `data/` testable this way; anything needing an Android context is untested. `LedgerCsvTest.realExportIfPresent` sanity-checks against `../export.csv` if you drop a real Dime/Nickel export there; that file is never committed.

## Style

- `kotlin.code.style=official`. Comments are KDoc explaining *why* (tradeoffs, crash safety, frame budget), not *what* — follow that voice when editing.
- UI code leans on Compose snapshot state (`Money.chosenCurrency`, `MutableStateFlow` in the ViewModel) rather than passing parameters down; composables read flows via `collectAsStateWithLifecycle`.

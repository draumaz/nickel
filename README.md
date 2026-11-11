# Nickel

A cozy little expense tracker for Android, built with Jetpack Compose and
[Material 3 Expressive](https://m3.material.io/).
Log what comes in and what goes out, sort it into categories, and keep an eye on
the running total — with plenty of springy, shape-shifting motion along the way.

<p align="center">
  <img src="docs/screenshots/home.png" width="260" alt="Home screen: net total card, income and expense filters, and today's transactions grouped by day">
  <img src="docs/screenshots/entry.png" width="260" alt="Entry sheet over a frosted home screen: expense amount, category picker, note, date and number pad">
  <img src="docs/screenshots/settings.png" width="260" alt="Settings: dynamic color, currency, notification auto-add, and CSV import and export">
</p>

## Features

- **Running total at a glance.** The net total sits at the top of the home screen
  and rolls like an odometer when it changes, with income and spending shown
  underneath.
- **Fast entry.** Tap the spinning scalloped **+** and choose Income or Expense. A
  custom number pad, swipeable category picker, optional note, and date picker
  (for logging things after the fact) all fit in one sheet.
- **Categories with character.** Each category has its own icon and Material
  shape — Paycheck, Salary, Groceries, Rent, Medical, Subscriptions, and more.
  Categories from imported files are recreated with fitting icons.
- **A tidy history.** Transactions are grouped by day with daily totals, and can
  be filtered to just income or just expenses. Tap one to edit it, or swipe it
  away to delete (with undo).
- **Dynamic color.** Follows your wallpaper's Material You palette, or switch it
  off for Nickel's own sage-green theme. Light and dark mode are both supported.
- **CSV import and export.** Bring in your history from
  Dime or a spreadsheet, and export everything back
  out at any time. See [CSV format](#csv-format).
- **Expressive by design.** Spring-based motion, shape morphing, connected button
  groups, a predictive back gesture, loading indicators, and custom haptics.
- **Frosted glass.** On Android 17 QPR2, the add button and header buttons are
  lightly tinted glass that blurs whatever scrolls beneath them, and the entry
  sheet frosts the screen behind it as it grows out of the button. Earlier
  Android 17 builds get the same controls as solid tonal surfaces.

  <img src="docs/screenshots/glass.png" width="540" alt="Close-up of the glass add button blurring a transaction row scrolling underneath it">

## Privacy

Nickel has no network access — the app doesn't request the internet permission.
Your ledger lives only in the app's private storage on your device, and only
leaves it when you export a CSV yourself.

## CSV format

Exports use the same layout as Dime, so files move cleanly between the two apps:

```csv
Date,Note,Amount,Category,Type
2026-10-31 14:12:45 +0000,Groceries run,42.18,Groceries,Expense
2026-10-29 17:23:00 +0000,Paycheck,1500.00,Paycheck,Income
```

Imports are more flexible. Columns are matched by header name, so most exports
work as long as they have a **date** and an **amount** column:

| Field    | Accepted headers                                          |
| -------- | --------------------------------------------------------- |
| Date     | `Date`, `Time`, `Timestamp`, `Datetime`, `Created`        |
| Amount   | `Amount`, `Value`, `Sum`, `Price`                         |
| Note     | `Note`, `Notes`, `Title`, `Description`, `Memo`, `Name`, `Payee` |
| Category | `Category`, `Categories`, `Tag`                           |
| Type     | `Type`, `Kind`, `Transaction type`                        |

- **Dates** can include a timezone offset (as Dime's do), be ISO 8601, or be
  plain `yyyy-MM-dd` / `M/d/yyyy` dates. Past dates are kept exactly as written.
- **Amounts** may include currency symbols, thousands separators, a minus sign,
  or accounting-style parentheses.
- **Type** accepts values like `Income`/`Expense` or `Credit`/`Debit`. Without a
  type column, positive amounts are treated as income and negative as expenses.
- **Categories** that match a built-in one are reused; anything else becomes a
  new custom category.
- **Re-importing is safe.** Transactions already in Nickel are recognized and
  skipped, and you'll see a preview of what will be added before anything is
  saved.

## Building

Requirements:

- Android SDK with platform 37.2 (Android 17 QPR2; set `sdk.dir` in
  `local.properties`, or `ANDROID_HOME`)
- A JDK supported by Gradle 9.8 (the wrapper handles Gradle itself)
- A device or emulator running Android 17 (API 37) or newer; frosted glass
  needs QPR2 (API 37.2)

```sh
./gradlew installDebug      # build and install on a connected device
./gradlew testDebugUnitTest # run the unit tests
```

## Project structure

```
app/src/main/java/io/github/draumaz/nickel/
├── MainActivity.kt            # entry point and screen navigation
├── data/
│   ├── Models.kt              # transactions, categories, icon and shape registries
│   ├── TransactionRepository.kt  # JSON-backed ledger storage
│   ├── SettingsRepository.kt  # preferences (dynamic color)
│   ├── Csv.kt                 # CSV parsing, import planning, and export
│   └── Money.kt               # currency formatting and keypad input
└── ui/
    ├── HomeScreen.kt          # total, filters, transaction list, add button
    ├── EntrySheet.kt          # add/edit sheet
    ├── Keypad.kt              # number pad
    ├── SettingsScreen.kt      # settings, import/export flow
    ├── NickelViewModel.kt     # app state
    ├── Haptics.kt             # composed vibration effects
    ├── components/            # shared composables (header, badges, rolling text, glass)
    └── theme/                 # color schemes and theme
```

## Tech

Kotlin · Jetpack Compose · Material 3 (1.5 beta, Expressive APIs) ·
Android Gradle Plugin 9 · min SDK 37, target SDK 37, compile SDK 37.2

## License

Nickel is released under the [MIT License](LICENSE).

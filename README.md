# Nickel

A cozy little expense tracker for Android, built with Jetpack Compose and
[Material 3 Expressive](https://m3.material.io/).

<p align="center">
  <img src="docs/screenshots/home.png" width="260" alt="Home screen: net total card, income and expense filters, and today's transactions grouped by day">
  <img src="docs/screenshots/entry.png" width="260" alt="Entry sheet over a frosted home screen: expense amount, category picker, note, date and number pad">
  <img src="docs/screenshots/settings.png" width="260" alt="Settings: dynamic color, currency, notification auto-add, and CSV import and export">
</p>

## Features

- A running total that rolls like an odometer, with history grouped by day
- Quick entry from the spinning **+**: number pad, category dial, note and date in one sheet
- Swipe a transaction right to duplicate it, left to delete it
- Frosted glass on Android 17 QPR2, with springy, shape-morphing motion throughout
- Dynamic color, or Nickel's own sage-green theme
- CSV import and export, automatic backups to a folder you pick, and home-screen widgets (overall balance and today)

<p align="center">
  <img src="docs/screenshots/glass.png" width="540" alt="Close-up of the glass add button blurring a transaction row scrolling underneath it">
</p>

## Privacy

No internet permission. Your ledger stays on your device unless you export it.

## CSV

Exports use the same layout as [Dime](https://github.com/rafsoh/dimeApp), so files move between the two apps:

```csv
Date,Note,Amount,Category,Type
2026-10-31 14:12:45 +0000,Groceries run,42.18,Groceries,Expense
```

Imports match columns by header name, so most spreadsheets work as long as they
have a date and an amount. You get a preview before anything is saved, and
transactions you already have are skipped.

## Building

Needs the Android SDK with platform 37.2 and a device on Android 17 or newer.

```sh
./gradlew installDebug      # build and install on a connected device
./gradlew testDebugUnitTest # run the unit tests
```

## License

[MIT](LICENSE)

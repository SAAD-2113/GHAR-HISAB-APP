# GharHisab

GharHisab is a household finance and expense manager for individuals and families in Pakistan, built by Sindan Technologies.

## Overview

This application helps users track their daily income and expenses. It features a clean, premium user interface built with modern Android development practices, ensuring a smooth and responsive experience. The app is localized to support English, Urdu, and Punjabi, and includes robust theming capabilities.

## Tech Stack

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose (Material Design 3)
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Navigation:** Navigation Compose
*   **Data Persistence:** Jetpack DataStore (Preferences)
*   **Minimum SDK:** 29 (Android 10)
*   **Target SDK:** 35

## Architecture & Structure

The project follows a clean, modular architecture separating concerns into distinct layers:

*   **UI Layer (`ui`)**: Contains Composables, ViewModels, and Navigation. Divided into components, screens, and theme packages.
*   **Data Layer (`data`)**: Manages data sources. Currently implements DataStore for user preferences.
*   **Domain Layer (`domain`)**: Houses business models like `Language`, `ThemeMode`, and `StorageOption`.

## Setup Instructions

1.  Clone the repository.
2.  Open the project in Android Studio (Ladybug or newer recommended).
3.  Ensure you have the required Android SDKs installed (API 35).
4.  Sync the project with Gradle files.
5.  Run the `app` configuration on an emulator or physical device running Android 10 or higher.

## Theming

GharHisab supports three theme modes:
*   **Follow System** (Default): Adapts to the device's system theme.
*   **Classic**: A light theme with Deep Red and Royal Gold accents.
*   **Midnight**: A dark theme designed for low-light environments.

Themes are applied dynamically and can be changed in the initial onboarding flow or from the settings screen.

## Localization

The app is scaffolded for localization:
*   **English**: Fully supported (`values/strings.xml`).
*   **Urdu**: Placeholder ready (`values-ur/strings.xml`), with RTL layout support enabled.
*   **Punjabi**: Placeholder ready (`values-pa/strings.xml`).

To add translations, simply populate the corresponding `strings.xml` files with the translated values.

## Roadmap

Future iterations of GharHisab will include:

*   **Core Financial Tracking**: Implement adding, editing, and deleting transactions.
*   **Budgets & Categories**: Allow users to set budgets and categorize expenses.
*   **Room Database Integration**: Transition from placeholder storage to a robust local SQL database for financial records.
*   **Encrypted Storage**: Secure sensitive financial data locally.
*   **Biometric Security**: App PIN, Fingerprint, and Face Unlock integration.
*   **Google Drive Backup**: Enable cloud sync for data safety.
*   **Voice Input**: Add support for logging transactions via voice in supported languages.
*   **Reports & Charts**: Visual breakdown of income and expenses.

## License

Proprietary — Sindan Technologies (2026). All rights reserved.

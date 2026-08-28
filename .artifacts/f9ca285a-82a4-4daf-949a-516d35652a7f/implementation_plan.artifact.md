# Implementation Plan - Dynamic Themeing and UI/UX Improvements

This plan outlines the steps to implement dynamic themeing (Material 3 Dynamic Color) and polish the overall UI/UX of the PulseBreak app.

## Proposed Changes

### Dynamic Themeing

#### [MODIFY] [UserPreferencesRepository.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/data/preferences/UserPreferencesRepository.kt)
- Add `dynamicColorEnabled: Boolean` to `UserSettings` (default `true` on Android 12+).
- Update `PreferencesKeys` and `userSettingsFlow` to handle the new key.
- Update `saveUserSettings` to persist the new setting.

#### [MODIFY] [SettingsViewModel.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/settings/SettingsViewModel.kt)
- Add functions to toggle dynamic color and change theme mode (Light/Dark/System).

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/settings/SettingsScreen.kt)
- Add a "Appearance" section with:
    - Theme Mode selector (System, Light, Dark).
    - Dynamic Color toggle (only shown on Android 12+).

#### [MODIFY] [Theme.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/theme/Theme.kt)
- Refactor `PulseBreakTheme` to take `themeMode` and `dynamicColorEnabled`.
- Ensure it correctly applies the selected mode and dynamic colors.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/MainActivity.kt)
- Observe `userSettings` from `SettingsViewModel` (or a dedicated `MainViewModel`).
- Pass the observed settings to `PulseBreakTheme`.

---

### UI/UX Improvements

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/home/HomeScreen.kt)
- Re-enable `AnimatedVisibility` with smoother transitions.
- Fix the reported "blank screen" issue by ensuring `uiState` handling is robust.
- Polish the layout with better spacing and Material 3 cards.
- Replace any hardcoded colors with theme-aware colors.

#### [MODIFY] [AppNavigation.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/navigation/AppNavigation.kt)
- Improve `NavigationBar` styling (e.g., use `tonalElevation` or custom container color).
- Ensure consistent icon usage.

#### [MODIFY] [Typography.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/theme/Type.kt)
- Audit and refine typography to follow Material 3 guidelines for all text styles.

#### [MODIFY] General Cleanup
- Audit other screens (`WorkoutSetupScreen`, `ActiveWorkoutScreen`, etc.) for hardcoded values and UI consistency.
- Ensure proper inset handling across all screens (Edge-to-Edge).

## Verification Plan

### Automated Tests
- Run existing UI tests (if any) to ensure no regressions.
- I will check for existing tests before proceeding.

### Manual Verification
- Deploy to an Android 12+ device/emulator to verify Dynamic Color.
- Verify Theme Mode switching (Light/Dark/System) in Settings.
- Verify UI responsiveness and smooth animations on `HomeScreen`.
- Check Edge-to-Edge behavior (system bars transparency and padding).

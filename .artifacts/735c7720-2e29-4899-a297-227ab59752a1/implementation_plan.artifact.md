# Implementation Plan - App Enhancements

Enhance PulseBreak with voice guidance, workout history, and visual progress tracking.

## User Review Required

> [!IMPORTANT]
> The app will now require `TextToSpeech` engine on the device for voice guidance. This is standard on most Android devices.

## Proposed Changes

### [Audio & Feedback]
Add Voice Guidance (TTS) to announce workout phases.

#### [MODIFY] [AudioHapticFeedback.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/domain/timer/AudioHapticFeedback.kt)
- Initialize `TextToSpeech`.
- Implement `speak(text: String)` method.
- Clean up resources in a new `release()` method.

#### [MODIFY] [WorkoutTimerEngine.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/domain/timer/WorkoutTimerEngine.kt)
- Hook into phase changes to trigger voice announcements (e.g., "Work!", "Rest!", "Final Round!").

#### [MODIFY] [WorkoutForegroundService.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/service/WorkoutForegroundService.kt)
- Ensure `AudioHapticFeedback.release()` is called when the service is destroyed.

---

### [UI & Features]
Add Workout History screen and Weekly Progress chart.

#### [NEW] [HistoryScreen.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/workout/HistoryScreen.kt)
- Create a list-based UI to view past `WorkoutRecord` entries.
- Include ability to delete history items.

#### [MODIFY] [AppNavigation.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/navigation/AppNavigation.kt)
- Add `Screen.History` route.
- Add "History" to the bottom navigation bar (replacing or adding to existing items).

#### [NEW] [WeeklyProgressChart.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/components/WeeklyProgressChart.kt)
- A simple, stylized bar chart using Compose `Canvas`.

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/home/HomeScreen.kt)
- Integrate `WeeklyProgressChart` into the Home screen.
- Add navigation to the new History screen.

---

### [UI Polish]
#### [MODIFY] [ActiveWorkoutScreen.kt](file:///C:/Users/artho/AndroidStudioProjects/pulsebreak/app/src/main/java/com/example/ui/workout/ActiveWorkoutScreen.kt)
- Use `safeDrawing` or `navigationBars` insets for bottom controls to ensure they are fully visible on all devices.

## Verification Plan

### Automated Tests
- Build and run the app to ensure no regressions.
- Verify `WorkoutRecord` is still correctly saved after a session.

### Manual Verification
- Start a workout and verify Voice Guidance announces "Work" and "Rest".
- Navigate to the new History screen and verify past workouts are listed.
- Check the Weekly Progress chart on the Home screen.
- Verify bottom controls on `ActiveWorkoutScreen` are correctly positioned with edge-to-edge.

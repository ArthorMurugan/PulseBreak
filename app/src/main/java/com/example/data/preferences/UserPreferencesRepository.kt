package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ReminderConfig
import com.example.domain.model.ReminderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pulsebreak_preferences")

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

data class UserSettings(
    // Workout Defaults
    val defaultWorkSec: Int = 30,
    val defaultRestSec: Int = 15,
    val defaultRounds: Int = 8,
    val workoutSoundEnabled: Boolean = true,
    val workoutVibrationEnabled: Boolean = true,
    val workoutCountdownWarningSec: Int = 3,
    
    // Reminders
    val waterIntervalMin: Int = 45,
    val standIntervalMin: Int = 60,
    val stretchIntervalMin: Int = 120,
    val reminderSoundEnabled: Boolean = true,
    val reminderVibrationEnabled: Boolean = true,

    // Water Tracker
    val waterDailyTarget: Int = 8,
    val waterAmountPerDrinkMl: Int = 250,

    // Movement Tracker
    val moveDailyTarget: Int = 8,

    // Appearance
    val themeMode: ThemeMode = ThemeMode.DARK
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val WORKOUT_WORK_SEC = intPreferencesKey("workout_work_sec")
        val WORKOUT_REST_SEC = intPreferencesKey("workout_rest_sec")
        val WORKOUT_ROUNDS = intPreferencesKey("workout_rounds")
        val WORKOUT_SOUND = booleanPreferencesKey("workout_sound")
        val WORKOUT_VIBRATION = booleanPreferencesKey("workout_vibration")
        val WORKOUT_COUNTDOWN_WARNING = intPreferencesKey("workout_countdown_warning")

        val REMINDER_WATER_INTERVAL = intPreferencesKey("reminder_water_interval")
        val REMINDER_STAND_INTERVAL = intPreferencesKey("reminder_stand_interval")
        val REMINDER_STRETCH_INTERVAL = intPreferencesKey("reminder_stretch_interval")
        val REMINDER_SOUND = booleanPreferencesKey("reminder_sound")
        val REMINDER_VIBRATION = booleanPreferencesKey("reminder_vibration")
        val REMINDERS_CONFIG_JSON = stringPreferencesKey("reminders_config_json")

        val WATER_DAILY_TARGET = intPreferencesKey("water_daily_target")
        val WATER_AMOUNT_PER_DRINK = intPreferencesKey("water_amount_per_drink")
        val MOVE_DAILY_TARGET = intPreferencesKey("move_daily_target")

        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            defaultWorkSec = prefs[PreferencesKeys.WORKOUT_WORK_SEC] ?: 30,
            defaultRestSec = prefs[PreferencesKeys.WORKOUT_REST_SEC] ?: 15,
            defaultRounds = prefs[PreferencesKeys.WORKOUT_ROUNDS] ?: 8,
            workoutSoundEnabled = prefs[PreferencesKeys.WORKOUT_SOUND] ?: true,
            workoutVibrationEnabled = prefs[PreferencesKeys.WORKOUT_VIBRATION] ?: true,
            workoutCountdownWarningSec = prefs[PreferencesKeys.WORKOUT_COUNTDOWN_WARNING] ?: 3,

            waterIntervalMin = prefs[PreferencesKeys.REMINDER_WATER_INTERVAL] ?: 45,
            standIntervalMin = prefs[PreferencesKeys.REMINDER_STAND_INTERVAL] ?: 60,
            stretchIntervalMin = prefs[PreferencesKeys.REMINDER_STRETCH_INTERVAL] ?: 120,
            reminderSoundEnabled = prefs[PreferencesKeys.REMINDER_SOUND] ?: true,
            reminderVibrationEnabled = prefs[PreferencesKeys.REMINDER_VIBRATION] ?: true,

            waterDailyTarget = prefs[PreferencesKeys.WATER_DAILY_TARGET] ?: 8,
            waterAmountPerDrinkMl = prefs[PreferencesKeys.WATER_AMOUNT_PER_DRINK] ?: 250,
            moveDailyTarget = prefs[PreferencesKeys.MOVE_DAILY_TARGET] ?: 8,

            themeMode = when (prefs[PreferencesKeys.THEME_MODE]) {
                ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                ThemeMode.SYSTEM.name -> ThemeMode.SYSTEM
                else -> ThemeMode.DARK
            }
        )
    }

    val remindersFlow: Flow<List<ReminderConfig>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[PreferencesKeys.REMINDERS_CONFIG_JSON]
        if (jsonStr.isNullOrEmpty()) {
            ReminderConfig.createDefaults()
        } else {
            try {
                deserializeReminders(jsonStr)
            } catch (e: Exception) {
                ReminderConfig.createDefaults()
            }
        }
    }

    suspend fun saveUserSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.WORKOUT_WORK_SEC] = settings.defaultWorkSec
            prefs[PreferencesKeys.WORKOUT_REST_SEC] = settings.defaultRestSec
            prefs[PreferencesKeys.WORKOUT_ROUNDS] = settings.defaultRounds
            prefs[PreferencesKeys.WORKOUT_SOUND] = settings.workoutSoundEnabled
            prefs[PreferencesKeys.WORKOUT_VIBRATION] = settings.workoutVibrationEnabled
            prefs[PreferencesKeys.WORKOUT_COUNTDOWN_WARNING] = settings.workoutCountdownWarningSec

            prefs[PreferencesKeys.REMINDER_WATER_INTERVAL] = settings.waterIntervalMin
            prefs[PreferencesKeys.REMINDER_STAND_INTERVAL] = settings.standIntervalMin
            prefs[PreferencesKeys.REMINDER_STRETCH_INTERVAL] = settings.stretchIntervalMin
            prefs[PreferencesKeys.REMINDER_SOUND] = settings.reminderSoundEnabled
            prefs[PreferencesKeys.REMINDER_VIBRATION] = settings.reminderVibrationEnabled

            prefs[PreferencesKeys.WATER_DAILY_TARGET] = settings.waterDailyTarget
            prefs[PreferencesKeys.WATER_AMOUNT_PER_DRINK] = settings.waterAmountPerDrinkMl
            prefs[PreferencesKeys.MOVE_DAILY_TARGET] = settings.moveDailyTarget
            prefs[PreferencesKeys.THEME_MODE] = settings.themeMode.name
        }
    }

    suspend fun updateWorkoutSettings(
        workSec: Int,
        restSec: Int,
        rounds: Int,
        sound: Boolean,
        vibration: Boolean,
        countdownWarning: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.WORKOUT_WORK_SEC] = workSec
            prefs[PreferencesKeys.WORKOUT_REST_SEC] = restSec
            prefs[PreferencesKeys.WORKOUT_ROUNDS] = rounds
            prefs[PreferencesKeys.WORKOUT_SOUND] = sound
            prefs[PreferencesKeys.WORKOUT_VIBRATION] = vibration
            prefs[PreferencesKeys.WORKOUT_COUNTDOWN_WARNING] = countdownWarning
        }
    }

    suspend fun updateReminderSettings(
        waterInterval: Int,
        standInterval: Int,
        sound: Boolean,
        vibration: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REMINDER_WATER_INTERVAL] = waterInterval
            prefs[PreferencesKeys.REMINDER_STAND_INTERVAL] = standInterval
            prefs[PreferencesKeys.REMINDER_SOUND] = sound
            prefs[PreferencesKeys.REMINDER_VIBRATION] = vibration
        }
    }

    suspend fun saveReminders(reminders: List<ReminderConfig>) {
        val json = serializeReminders(reminders)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REMINDERS_CONFIG_JSON] = json
        }
    }

    suspend fun updateWaterDailyTarget(target: Int, amountPerDrink: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.WATER_DAILY_TARGET] = target
            prefs[PreferencesKeys.WATER_AMOUNT_PER_DRINK] = amountPerDrink
        }
    }

    suspend fun updateMoveDailyTarget(target: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MOVE_DAILY_TARGET] = target
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun resetAllData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun serializeReminders(reminders: List<ReminderConfig>): String {
        val array = JSONArray()
        for (rem in reminders) {
            val obj = JSONObject()
            obj.put("id", rem.id)
            obj.put("type", rem.type.name)
            obj.put("isEnabled", rem.isEnabled)
            obj.put("intervalMinutes", rem.intervalMinutes)
            obj.put("startHour", rem.startHour)
            obj.put("startMinute", rem.startMinute)
            obj.put("endHour", rem.endHour)
            obj.put("endMinute", rem.endMinute)
            obj.put("soundEnabled", rem.soundEnabled)
            obj.put("vibrationEnabled", rem.vibrationEnabled)
            obj.put("lastTriggeredEpochMs", rem.lastTriggeredEpochMs)
            obj.put("nextScheduledEpochMs", rem.nextScheduledEpochMs)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeReminders(jsonStr: String): List<ReminderConfig> {
        val list = mutableListOf<ReminderConfig>()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ReminderConfig(
                    id = obj.getString("id"),
                    type = ReminderType.valueOf(obj.getString("type")),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    intervalMinutes = obj.optInt("intervalMinutes", 45),
                    startHour = obj.optInt("startHour", 8),
                    startMinute = obj.optInt("startMinute", 0),
                    endHour = obj.optInt("endHour", 20),
                    endMinute = obj.optInt("endMinute", 0),
                    soundEnabled = obj.optBoolean("soundEnabled", true),
                    vibrationEnabled = obj.optBoolean("vibrationEnabled", true),
                    lastTriggeredEpochMs = obj.optLong("lastTriggeredEpochMs", 0L),
                    nextScheduledEpochMs = obj.optLong("nextScheduledEpochMs", 0L)
                )
            )
        }
        return list
    }
}

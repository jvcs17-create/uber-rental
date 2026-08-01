package com.juanka.uberrental

import android.content.Context

/**
 * Guarda las preferencias del usuario (umbral y modo aprendizaje).
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var thresholdPerHour: Int
        get() = prefs.getInt(KEY_THRESHOLD, DEFAULT_THRESHOLD)
        set(value) = prefs.edit().putInt(KEY_THRESHOLD, value).apply()

    /** Modo aprendizaje: muestra el texto crudo leido para calibrar el parser. */
    var learnMode: Boolean
        get() = prefs.getBoolean(KEY_LEARN, false)
        set(value) = prefs.edit().putBoolean(KEY_LEARN, value).apply()

    companion object {
        private const val PREFS = "uber_rental_prefs"
        private const val KEY_THRESHOLD = "threshold_per_hour"
        private const val KEY_LEARN = "learn_mode"
        const val DEFAULT_THRESHOLD = 25000
    }
}

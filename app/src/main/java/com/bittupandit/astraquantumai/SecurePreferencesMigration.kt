package com.bittupandit.astraquantumai

import android.content.SharedPreferences

/**
 * Idempotently moves legacy plaintext preferences into encrypted preferences.
 * The completion marker is written only after the encrypted copy commits and
 * the legacy plaintext store has been cleared.
 */
internal fun migrateLegacyPreferences(secure: SharedPreferences, legacy: SharedPreferences) {
    val marker = "_secure_migration_complete"
    if (!secure.getBoolean(marker, false)) {
        val editor = secure.edit()
        legacy.all.forEach { (key, value) ->
            if (key == marker) return@forEach
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toMutableSet())
            }
        }
        check(editor.commit()) { "Secure migration failed" }
        check(legacy.edit().clear().commit()) { "Legacy storage cleanup failed" }
        check(secure.edit().putBoolean(marker, true).commit()) {
            "Secure migration completion marker failed"
        }
    } else if (legacy.all.isNotEmpty()) {
        // Clean legacy data left by an interrupted migration from an older app version.
        check(legacy.edit().clear().commit()) { "Legacy storage cleanup failed" }
    }
}

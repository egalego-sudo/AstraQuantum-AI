package com.bittupandit.astraquantumai

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurePreferencesMigrationTest {
    @Test
    fun copiesSupportedValuesThenClearsLegacyAndMarksComplete() {
        val secure = FakePreferences()
        val legacy = FakePreferences(
            mapOf(
                "name" to "Bittu",
                "enabled" to true,
                "count" to 7,
                "timestamp" to 99L,
                "ratio" to 1.5f,
                "labels" to setOf("one", "two"),
                "_secure_migration_complete" to true
            )
        )

        migrateLegacyPreferences(secure, legacy)

        assertEquals("Bittu", secure.getString("name", null))
        assertTrue(secure.getBoolean("enabled", false))
        assertEquals(7, secure.getInt("count", 0))
        assertEquals(99L, secure.getLong("timestamp", 0L))
        assertEquals(1.5f, secure.getFloat("ratio", 0f))
        assertEquals(setOf("one", "two"), secure.getStringSet("labels", emptySet()))
        assertTrue(secure.getBoolean("_secure_migration_complete", false))
        assertTrue(legacy.all.isEmpty())
    }

    @Test
    fun failedEncryptedCopyLeavesLegacyDataUntouched() {
        val secure = FakePreferences(failCommitNumbers = setOf(1))
        val legacy = FakePreferences(mapOf("private_note" to "keep me"))

        val error = runCatching { migrateLegacyPreferences(secure, legacy) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("keep me", legacy.getString("private_note", null))
        assertFalse(secure.getBoolean("_secure_migration_complete", false))
    }

    @Test
    fun failedLegacyCleanupRetriesWithoutLosingEncryptedCopy() {
        val secure = FakePreferences()
        val legacy = FakePreferences(mapOf("private_note" to "keep me"), failCommitNumbers = setOf(1))

        assertTrue(runCatching { migrateLegacyPreferences(secure, legacy) }.isFailure)
        assertEquals("keep me", secure.getString("private_note", null))
        assertEquals("keep me", legacy.getString("private_note", null))
        assertFalse(secure.getBoolean("_secure_migration_complete", false))

        migrateLegacyPreferences(secure, legacy)

        assertEquals("keep me", secure.getString("private_note", null))
        assertTrue(legacy.all.isEmpty())
        assertTrue(secure.getBoolean("_secure_migration_complete", false))
    }

    @Test
    fun failedCompletionMarkerRetriesAfterLegacyWasCleared() {
        val secure = FakePreferences(failCommitNumbers = setOf(2))
        val legacy = FakePreferences(mapOf("private_note" to "keep me"))

        assertTrue(runCatching { migrateLegacyPreferences(secure, legacy) }.isFailure)
        assertTrue(legacy.all.isEmpty())
        assertEquals("keep me", secure.getString("private_note", null))
        assertFalse(secure.getBoolean("_secure_migration_complete", false))

        migrateLegacyPreferences(secure, legacy)

        assertEquals("keep me", secure.getString("private_note", null))
        assertTrue(secure.getBoolean("_secure_migration_complete", false))
    }

    @Test
    fun completedMigrationStillClearsStaleLegacyData() {
        val secure = FakePreferences(mapOf("_secure_migration_complete" to true))
        val legacy = FakePreferences(mapOf("stale" to "plaintext"))

        migrateLegacyPreferences(secure, legacy)

        assertTrue(legacy.all.isEmpty())
        assertTrue(secure.getBoolean("_secure_migration_complete", false))
    }

    private class FakePreferences(
        initial: Map<String, Any?> = emptyMap(),
        private val failCommitNumbers: Set<Int> = emptySet()
    ) : SharedPreferences {
        private val values = initial.toMutableMap()
        private var commits = 0

        override fun getAll(): MutableMap<String, *> = values.mapValues { (_, value) ->
            if (value is Set<*>) value.toMutableSet() else value
        }.toMutableMap()

        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            (values[key] as? Set<String>)?.toMutableSet() ?: defValues?.toMutableSet()
        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor()

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private inner class FakeEditor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()
            private var clearRequested = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
                if (value == null) remove(key) else pending[key] = value
            }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                if (values == null) remove(key) else pending[key] = values.toMutableSet()
            }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { pending[key] = value }
            override fun remove(key: String): SharedPreferences.Editor = apply { removed += key }
            override fun clear(): SharedPreferences.Editor = apply { clearRequested = true }

            override fun commit(): Boolean {
                commits += 1
                if (commits in failCommitNumbers) return false
                if (clearRequested) values.clear()
                removed.forEach(values::remove)
                pending.forEach { (key, value) -> values[key] = if (value is Set<*>) value.toMutableSet() else value }
                return true
            }
            override fun apply() { commit() }
        }
    }
}

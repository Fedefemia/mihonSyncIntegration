package eu.kanade.tachiyomi.data.sync

import tachiyomi.core.common.preference.PreferenceStore

class SyncPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun syncHost() = preferenceStore.getString("pref_sync_host", "")

    fun syncEnabled() = preferenceStore.getBoolean("pref_sync_enabled", false)

    fun syncInterval() = preferenceStore.getInt("pref_sync_interval", 15)

    fun lastSyncDate() = preferenceStore.getLong("pref_sync_last_run", 0L)
}

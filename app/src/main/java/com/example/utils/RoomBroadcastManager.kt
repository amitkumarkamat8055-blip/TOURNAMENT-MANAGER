package com.example.utils

import android.content.Context
import java.util.Locale
import java.util.concurrent.TimeUnit

object RoomBroadcastManager {
    private const val PREFS_NAME = "room_broadcast_expiration_prefs"
    private const val KEY_BROADCAST_TIME = "broadcast_time_"
    private const val KEY_BROADCAST_DELETED = "broadcast_deleted_"

    // 30 minutes duration in milliseconds
    const val EXPIRATION_DURATION_MS = 30 * 60 * 1000L // 30 minutes

    /**
     * Records the broadcast timestamp for a match.
     * If [force] is false and a broadcast time was already set, it will keep the original time.
     */
    fun recordBroadcastTime(
        context: Context,
        matchId: Long,
        timestamp: Long = System.currentTimeMillis(),
        force: Boolean = false
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getLong("$KEY_BROADCAST_TIME$matchId", 0L)
        if (existing == 0L || force) {
            prefs.edit()
                .putLong("$KEY_BROADCAST_TIME$matchId", timestamp)
                .putBoolean("$KEY_BROADCAST_DELETED$matchId", false)
                .apply()
        }
    }

    /**
     * Gets the broadcast timestamp for a match.
     */
    fun getBroadcastTime(context: Context, matchId: Long): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong("$KEY_BROADCAST_TIME$matchId", 0L)
    }

    /**
     * Checks if the broadcast message has been manually deleted or marked deleted.
     */
    fun isBroadcastDeleted(context: Context, matchId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("$KEY_BROADCAST_DELETED$matchId", false)
    }

    /**
     * Marks the broadcast message as deleted.
     */
    fun markBroadcastDeleted(context: Context, matchId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("$KEY_BROADCAST_DELETED$matchId", true)
            .apply()
    }

    /**
     * Checks if 30 minutes have passed since the broadcast time, or if it was marked deleted.
     */
    fun isBroadcastExpired(
        context: Context,
        matchId: Long,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (isBroadcastDeleted(context, matchId)) return true
        val broadcastTime = getBroadcastTime(context, matchId)
        if (broadcastTime == 0L) return false
        return (currentTimeMillis - broadcastTime) >= EXPIRATION_DURATION_MS
    }

    /**
     * Returns the remaining time in milliseconds before the 30-minute window expires.
     */
    fun getRemainingMillis(
        context: Context,
        matchId: Long,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        if (isBroadcastDeleted(context, matchId)) return 0L
        val broadcastTime = getBroadcastTime(context, matchId)
        if (broadcastTime == 0L) return EXPIRATION_DURATION_MS
        val elapsed = currentTimeMillis - broadcastTime
        return (EXPIRATION_DURATION_MS - elapsed).coerceAtLeast(0L)
    }

    /**
     * Formats remaining milliseconds to readable string, e.g. "28m 42s" or "Expired".
     */
    fun formatRemainingTime(millis: Long): String {
        if (millis <= 0L) return "Expired"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02dm %02ds", minutes, seconds)
    }
}

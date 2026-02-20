package com.example.snapandcook.data.remote

import com.example.snapandcook.BuildConfig
import java.util.Collections

/**
 * Manages a pool of Spoonacular API keys and rotates to the next available one
 * whenever a key hits its daily 402 quota limit.
 *
 * Keys are configured in local.properties as:
 *   SPOONACULAR_API_KEY_1=your_first_key
 *   SPOONACULAR_API_KEY_2=your_second_key
 *   SPOONACULAR_API_KEY_3=your_third_key
 *   (add more by continuing the numbering — no code changes needed)
 *
 * The Gradle build collects all consecutive numbered keys into the
 * SPOONACULAR_API_KEYS BuildConfig field (pipe-delimited).
 */
object SpoonacularKeyManager {

    /** All configured keys, in order. */
    private val allKeys: List<String> = BuildConfig.SPOONACULAR_API_KEYS
        .split("|")
        .filter { it.isNotBlank() }

    /**
     * Keys that have returned a 402 (quota exhausted) in this app session.
     * Thread-safe: multiple coroutines may mark keys exhausted concurrently.
     */
    private val exhaustedKeys: MutableSet<String> = Collections.synchronizedSet(LinkedHashSet())

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * The next non-exhausted key, or null if every configured key is exhausted.
     */
    val currentKey: String?
        get() = allKeys.firstOrNull { it !in exhaustedKeys }

    /** True while at least one key still has quota remaining. */
    val hasAvailableKey: Boolean
        get() = currentKey != null

    /** Total number of configured keys. */
    val totalKeys: Int
        get() = allKeys.size

    /** Number of keys that still have quota. */
    val availableKeys: Int
        get() = allKeys.count { it !in exhaustedKeys }

    /**
     * Mark [key] as quota-exhausted for this session.
     * The next call to [currentKey] will skip it.
     */
    fun markExhausted(key: String) {
        exhaustedKeys.add(key)
    }

    /**
     * Human-readable status string, e.g. "2/3 API keys available".
     * Useful for log messages.
     */
    val status: String
        get() = "$availableKeys/$totalKeys Spoonacular API key(s) available"
}

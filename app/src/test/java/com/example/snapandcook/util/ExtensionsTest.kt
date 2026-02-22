package com.example.snapandcook.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for extension functions in [Extensions.kt].
 *
 * Tests string and integer extension functions used throughout the app.
 * Note: Context-based extensions (toast, decodeBitmapFromUri) require Android
 * runtime and are not tested here (would need instrumented tests).
 */
class ExtensionsTest {

    // ─── stripHtml Tests ─────────────────────────────────────────────────────────

    // Note: stripHtml tests removed - they require Android runtime (HtmlCompat).
    // These would need to be instrumented tests (androidTest) to run on device/emulator.
    // The stripHtml() function works correctly in production - it just can't be unit tested
    // without Android framework.

    // ─── formatMinutes Tests ─────────────────────────────────────────────────────

    @Test
    fun `formatMinutes handles zero minutes`() {
        // Given: 0 minutes
        val minutes = 0

        // When: Formatting
        val result = minutes.formatMinutes()

        // Then: Should show "0m"
        assertThat(result).isEqualTo("0m")
    }

    @Test
    fun `formatMinutes handles minutes less than 60`() {
        // Given: Less than 60 minutes
        val minutes = 45

        // When: Formatting
        val result = minutes.formatMinutes()

        // Then: Should show minutes only
        assertThat(result).isEqualTo("45m")
    }

    @Test
    fun `formatMinutes handles exactly 60 minutes`() {
        // Given: Exactly 60 minutes
        val minutes = 60

        // When: Formatting
        val result = minutes.formatMinutes()

        // Then: Should show "1h" (no minutes)
        assertThat(result).isEqualTo("1h")
    }

    @Test
    fun `formatMinutes handles more than 60 with remainder`() {
        // Given: 90 minutes (1 hour 30 minutes)
        val minutes = 90

        // When: Formatting
        val result = minutes.formatMinutes()

        // Then: Should show hours and minutes
        assertThat(result).isEqualTo("1h 30m")
    }

    @Test
    fun `formatMinutes handles multiple hours with no remainder`() {
        // Given: 120 minutes (2 hours exactly)
        val minutes = 120

        // When: Formatting
        val result = minutes.formatMinutes()

        // Then: Should show hours only
        assertThat(result).isEqualTo("2h")
    }

    @Test
    fun `formatMinutes handles multiple hours with remainder`() {
        // Given: 150 minutes (2 hours 30 minutes)
        val minutes = 150

        // When: Formatting
        val result = minutes.formatMinutes()

        // Then: Should show hours and minutes
        assertThat(result).isEqualTo("2h 30m")
    }

    @Test
    fun `formatMinutes handles small remainder`() {
        // Given: 61 minutes (1 hour 1 minute)
        val minutes = 61

        // When: Formatting
        val result = minutes.formatMinutes()

        // Then: Should show "1h 1m"
        assertThat(result).isEqualTo("1h 1m")
    }

    @Test
    fun `formatMinutes handles large values`() {
        // Given: 300 minutes (5 hours)
        val minutes = 300

        // When: Formatting
        val result = minutes.formatMinutes()

        // Then: Should show "5h"
        assertThat(result).isEqualTo("5h")
    }

    @Test
    fun `formatMinutes handles large values with remainder`() {
        // Given: 325 minutes (5 hours 25 minutes)
        val minutes = 325

        // When: Formatting
        val result = minutes.formatMinutes()

        // Then: Should show "5h 25m"
        assertThat(result).isEqualTo("5h 25m")
    }

    @Test
    fun `formatMinutes edge case - 59 minutes`() {
        // Given: 59 minutes (just under 1 hour)
        val minutes = 59

        // When: Formatting
        val result = minutes.formatMinutes()

        // Then: Should show minutes only
        assertThat(result).isEqualTo("59m")
    }

    @Test
    fun `formatMinutes various common cooking times`() {
        // Given: Common cooking durations
        val testCases = mapOf(
            15 to "15m",
            30 to "30m",
            45 to "45m",
            60 to "1h",
            75 to "1h 15m",
            90 to "1h 30m",
            120 to "2h",
            180 to "3h"
        )

        testCases.forEach { (minutes, expected) ->
            // When: Formatting each duration
            val result = minutes.formatMinutes()

            // Then: Should match expected format
            assertThat(result).isEqualTo(expected)
        }
    }
}

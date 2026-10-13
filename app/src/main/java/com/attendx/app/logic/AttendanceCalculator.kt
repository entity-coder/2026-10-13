package com.attendx.app.logic

import kotlin.math.floor

/**
 * Pure-Kotlin attendance math — no Android dependencies, so it is trivially
 * unit-testable on the JVM.
 */
object AttendanceCalculator {

    enum class Status { BELOW, AT_RISK, SAFE }

    /** Attendance percentage. A subject with no recorded classes reads 100%. */
    fun percentage(present: Int, absent: Int): Double {
        val total = present + absent
        if (total == 0) return 100.0
        return present * 100.0 / total
    }

    /**
     * BELOW if pct < target,
     * AT_RISK if pct is within 5 points of the target (but not below),
     * SAFE otherwise.
     */
    fun status(percent: Double, target: Double): Status = when {
        percent < target -> Status.BELOW
        percent < target + 5.0 -> Status.AT_RISK
        else -> Status.SAFE
    }

    /**
     * Maximum number of consecutive classes that can still be missed before
     * the percentage drops below [target].
     *
     * From present / (present + absent + x) >= target/100,
     * solving for x gives x <= present * 100/target - present - absent.
     * Never negative: if already below target, the answer is 0.
     */
    fun classesCanMiss(present: Int, absent: Int, target: Double): Int {
        if (target <= 0.0) return Int.MAX_VALUE
        val maxMisses = floor(present * 100.0 / target - present - absent).toInt()
        return maxMisses.coerceAtLeast(0)
    }
}

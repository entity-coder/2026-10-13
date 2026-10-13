package com.attendx.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single tracked subject.
 *
 * Attendance is stored as two simple counters ([presentCount], [absentCount])
 * instead of per-day records — deliberately simple for a daily-size project.
 */
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    /** Short display code, e.g. "DM" for Discrete Mathematics. */
    val code: String,
    /** Classes held per week; informational, shown on the list. */
    val classesPerWeek: Int,
    /** Target attendance percentage, default 75 (the common college rule). */
    val targetPercent: Double = 75.0,
    val presentCount: Int = 0,
    val absentCount: Int = 0
)

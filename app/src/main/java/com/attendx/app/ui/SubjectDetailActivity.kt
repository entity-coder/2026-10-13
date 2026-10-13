package com.attendx.app.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.attendx.app.R
import com.attendx.app.data.AppDatabase
import com.attendx.app.data.Subject
import com.attendx.app.databinding.ActivitySubjectDetailBinding
import com.attendx.app.logic.AttendanceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Per-subject screen: big MARK PRESENT / MARK ABSENT buttons, live stats,
 * a progress bar, the "you can still miss N classes" line, and a menu with
 * Edit and Delete actions.
 */
class SubjectDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectDetailBinding
    private var subjectId: Long = -1L
    private var current: Subject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L)
        if (subjectId == -1L) {
            finish()
            return
        }

        binding.buttonPresent.setOnClickListener { markAttendance(present = true) }
        binding.buttonAbsent.setOnClickListener { markAttendance(present = false) }

        // Keep the screen in sync with the DB (counters change here).
        val dao = AppDatabase.getInstance(this).subjectDao()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.getAll().collect { subjects ->
                    val subject = subjects.firstOrNull { it.id == subjectId }
                    if (subject == null) {
                        // Deleted elsewhere — close.
                        finish()
                    } else {
                        current = subject
                        render(subject)
                    }
                }
            }
        }
    }

    /** Bump the present or absent counter and save. */
    private fun markAttendance(present: Boolean) {
        val subject = current ?: return
        val updated = if (present) {
            subject.copy(presentCount = subject.presentCount + 1)
        } else {
            subject.copy(absentCount = subject.absentCount + 1)
        }
        val dao = AppDatabase.getInstance(this).subjectDao()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { dao.update(updated) }
        }
    }

    private fun render(subject: Subject) {
        val percent = AttendanceCalculator.percentage(
            subject.presentCount, subject.absentCount
        )
        val status = AttendanceCalculator.status(percent, subject.targetPercent)
        val canMiss = AttendanceCalculator.classesCanMiss(
            subject.presentCount, subject.absentCount, subject.targetPercent
        )

        supportActionBar?.title = subject.name
        binding.textCode.text = getString(R.string.code_format, subject.code)
        binding.textPercentage.text = String.format(Locale.US, "%.1f%%", percent)
        binding.textCounts.text = getString(
            R.string.counts_format, subject.presentCount, subject.absentCount
        )
        binding.textTarget.text = getString(
            R.string.target_format, trimTarget(subject.targetPercent)
        )
        binding.progressBar.progress = percent.toInt().coerceIn(0, 100)

        val (statusText, statusColorRes) = when (status) {
            AttendanceCalculator.Status.SAFE ->
                getString(R.string.status_safe) to R.color.status_safe
            AttendanceCalculator.Status.AT_RISK ->
                getString(R.string.status_at_risk) to R.color.status_at_risk
            AttendanceCalculator.Status.BELOW ->
                getString(R.string.status_below) to R.color.status_below
        }
        binding.textStatus.text = statusText
        val statusColor = ContextCompat.getColor(this, statusColorRes)
        binding.textStatus.setTextColor(statusColor)
        binding.progressBar.setProgressTintList(
            android.content.res.ColorStateList.valueOf(statusColor)
        )

        binding.textCanMiss.text = getString(R.string.can_miss_format, canMiss)
    }

    private fun trimTarget(target: Double): String =
        if (target == target.toLong().toDouble()) target.toLong().toString()
        else target.toString()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_subject_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_edit -> {
                AddEditSubjectDialogFragment.newInstance(subjectId)
                    .show(supportFragmentManager, "edit_subject")
                true
            }
            R.id.action_delete -> {
                confirmDelete()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmDelete() {
        val subject = current ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, subject.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                val dao = AppDatabase.getInstance(this).subjectDao()
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { dao.delete(subject) }
                    finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_SUBJECT_ID = "subject_id"
    }
}

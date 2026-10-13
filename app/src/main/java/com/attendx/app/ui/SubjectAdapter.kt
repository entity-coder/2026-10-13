package com.attendx.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.attendx.app.R
import com.attendx.app.data.Subject
import com.attendx.app.databinding.ItemSubjectBinding
import com.attendx.app.logic.AttendanceCalculator
import java.util.Locale

/**
 * RecyclerView adapter for the subject list. Each row shows the subject name
 * and code, current percentage, present/absent counts, and a status-colored
 * bar + badge (green = safe, amber = at risk, red = below target).
 */
class SubjectAdapter(
    private val onItemClick: (Subject) -> Unit
) : ListAdapter<Subject, SubjectAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSubjectBinding,
        private val onItemClick: (Subject) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: Subject) {
            val percent = AttendanceCalculator.percentage(
                subject.presentCount, subject.absentCount
            )
            val status = AttendanceCalculator.status(percent, subject.targetPercent)

            binding.textName.text = subject.name
            binding.textCode.text = subject.code
            binding.textPercentage.text = String.format(Locale.US, "%.1f%%", percent)
            binding.textCounts.text = binding.root.context.getString(
                R.string.counts_format, subject.presentCount, subject.absentCount
            )
            binding.textClassesPerWeek.text = binding.root.context.getString(
                R.string.classes_per_week_format, subject.classesPerWeek
            )

            val context = binding.root.context
            val (badgeText, badgeColorRes, barColorRes) = when (status) {
                AttendanceCalculator.Status.SAFE -> Triple(
                    context.getString(R.string.status_safe),
                    R.color.status_safe, R.color.status_safe
                )
                AttendanceCalculator.Status.AT_RISK -> Triple(
                    context.getString(R.string.status_at_risk),
                    R.color.status_at_risk, R.color.status_at_risk
                )
                AttendanceCalculator.Status.BELOW -> Triple(
                    context.getString(R.string.status_below),
                    R.color.status_below, R.color.status_below
                )
            }
            binding.textBadge.text = badgeText
            val badgeColor = ContextCompat.getColor(context, badgeColorRes)
            binding.textBadge.setTextColor(badgeColor)
            binding.statusBar.setBackgroundColor(
                ContextCompat.getColor(context, barColorRes)
            )

            binding.root.setOnClickListener { onItemClick(subject) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Subject>() {
            override fun areItemsTheSame(oldItem: Subject, newItem: Subject) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Subject, newItem: Subject) =
                oldItem == newItem
        }
    }
}

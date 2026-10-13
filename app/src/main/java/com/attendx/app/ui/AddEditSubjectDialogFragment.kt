package com.attendx.app.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.attendx.app.R
import com.attendx.app.data.AppDatabase
import com.attendx.app.data.Subject
import com.attendx.app.databinding.DialogAddSubjectBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Add/edit dialog with fields: name, code, classes per week, target percent.
 * Pass an existing [Subject] id to [newInstance] for edit mode; pass null to
 * add a brand-new subject.
 */
class AddEditSubjectDialogFragment : DialogFragment() {

    private var _binding: DialogAddSubjectBinding? = null
    private val binding get() = _binding!!

    /** Subject id being edited, or -1 for "add" mode. */
    private var editingId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingId = arguments?.getLong(ARG_SUBJECT_ID, -1L) ?: -1L
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddSubjectBinding.inflate(layoutInflater)
        val title = if (editingId == -1L) R.string.add_subject else R.string.edit_subject

        // Prefill fields in edit mode.
        if (editingId != -1L) {
            val dao = AppDatabase.getInstance(requireContext()).subjectDao()
            lifecycleScope.launch {
                val subject = withContext(Dispatchers.IO) { dao.getById(editingId) }
                subject?.let {
                    binding.editName.setText(it.name)
                    binding.editCode.setText(it.code)
                    binding.editClassesPerWeek.setText(it.classesPerWeek.toString())
                    binding.editTarget.setText(trimTarget(it.targetPercent))
                }
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(R.string.save) { _, _ -> saveSubject() }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    private fun saveSubject() {
        val name = binding.editName.text?.toString()?.trim().orEmpty()
        val code = binding.editCode.text?.toString()?.trim().orEmpty()
        val classesPerWeek = binding.editClassesPerWeek.text?.toString()?.toIntOrNull() ?: 0
        val target = binding.editTarget.text?.toString()?.toDoubleOrNull()

        // Basic validation before touching the DB.
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_name_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (code.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_code_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (classesPerWeek <= 0) {
            Toast.makeText(requireContext(), R.string.error_classes_per_week, Toast.LENGTH_SHORT).show()
            return
        }
        if (target == null || target <= 0.0 || target > 100.0) {
            Toast.makeText(requireContext(), R.string.error_target_range, Toast.LENGTH_SHORT).show()
            return
        }

        val dao = AppDatabase.getInstance(requireContext()).subjectDao()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (editingId == -1L) {
                    dao.insert(
                        Subject(
                            name = name,
                            code = code.uppercase(),
                            classesPerWeek = classesPerWeek,
                            targetPercent = target
                        )
                    )
                } else {
                    val existing = dao.getById(editingId)
                    existing?.let {
                        dao.update(
                            it.copy(
                                name = name,
                                code = code.uppercase(),
                                classesPerWeek = classesPerWeek,
                                targetPercent = target
                            )
                        )
                    }
                }
            }
        }
    }

    /** Display whole targets ("75") without a trailing ".0". */
    private fun trimTarget(target: Double): String =
        if (target == target.toLong().toDouble()) target.toLong().toString()
        else target.toString()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SUBJECT_ID = "subject_id"

        /** [subjectId] = null for add mode, or an existing id for edit mode. */
        fun newInstance(subjectId: Long?): AddEditSubjectDialogFragment =
            AddEditSubjectDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SUBJECT_ID, subjectId ?: -1L)
                }
            }
    }
}

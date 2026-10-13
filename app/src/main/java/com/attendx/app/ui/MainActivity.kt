package com.attendx.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.attendx.app.data.AppDatabase
import com.attendx.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Home screen: the subject list with a FAB to add new subjects.
 * Tapping a subject opens [SubjectDetailActivity] where attendance is marked.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SubjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = SubjectAdapter { subject ->
            // Open the detail screen for the tapped subject.
            val intent = Intent(this, SubjectDetailActivity::class.java)
                .putExtra(SubjectDetailActivity.EXTRA_SUBJECT_ID, subject.id)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener {
            AddEditSubjectDialogFragment.newInstance(null)
                .show(supportFragmentManager, "add_subject")
        }

        // Collect the Flow; the list refreshes itself on every DB change.
        val dao = AppDatabase.getInstance(this).subjectDao()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.getAll().collect { subjects ->
                    adapter.submitList(subjects)
                    binding.emptyView.visibility =
                        if (subjects.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}

package com.SPMG.ERPDS

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set personalized greeting from a variable
        val userName = "Max Mustermann" // Replace this with your actual variable/logic
        val userNameTextView = findViewById<android.widget.TextView>(R.id.userNameTextView)
        
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        val greetingResId = if (hour in 0..10) R.string.greeting_morning else R.string.greeting_day
        userNameTextView.text = getString(greetingResId, userName)

        // Setup RecyclerView for Assignments
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.assignmentsRecyclerView)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
        // Example: List from an API (represented as an mutable list for removal)
        val assignmentsList = mutableListOf(
            Assignment("Einsatz Zentrale", "Unterstützung bei IT-Infrastruktur", isCurrent = true),
            Assignment("Kunde SPMG", "Wartung der ERP-Systeme", isCurrent = false),
            Assignment("Projekt Website", "Anpassung der mobilen Ansicht", isCurrent = false)
        )
        
        val adapter = AssignmentsAdapter(
            assignmentsList,
            onOpenClick = { assignment ->
                val intent = android.content.Intent(this, AssignmentDetailActivity::class.java).apply {
                    putExtra("title", assignment.title)
                    putExtra("details", assignment.details)
                }
                startActivity(intent)
            },
            onDismissClick = { assignment ->
                // 1. Send API Call (Preparation)
                // api.cancelAssignment(id = assignment.id, status = "Unable to Respond")
                android.widget.Toast.makeText(this, "API: Status 'Unable to Respond' gesendet", android.widget.Toast.LENGTH_SHORT).show()

                // 2. Remove from UI
                val position = assignmentsList.indexOf(assignment)
                if (position != -1) {
                    assignmentsList.removeAt(position)
                    recyclerView.adapter?.notifyItemRemoved(position)
                }
            },
            onReportClick = { assignment ->
                val intent = android.content.Intent(this, AssignmentReportActivity::class.java).apply {
                    putExtra("title", assignment.title)
                }
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter
    }
}
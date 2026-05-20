package com.SPMG.ERPDS

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DispatchActivity : AppCompatActivity() {
    
    private lateinit var allAssignments: MutableList<Assignment>
    private lateinit var adapter: AssignmentsAdapter

    private val detailActivityLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val id = data?.getStringExtra("assignmentId")
            val newStatusStr = data?.getStringExtra("newStatus")
            
            if (id != null && newStatusStr != null) {
                val newStatus = AssignmentStatus.valueOf(newStatusStr)
                val assignment = allAssignments.find { it.id == id }
                assignment?.let {
                    it.status = newStatus
                    if (newStatus == AssignmentStatus.ONGOING) {
                        it.acceptanceTime = data.getStringExtra("acceptanceTime") ?: ""
                    } else if (newStatus == AssignmentStatus.COMPLETED) {
                        it.completionTime = data.getStringExtra("completionTime") ?: ""
                    }
                    adapter.setData(buildAdapterItems(allAssignments))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dispatch)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val recyclerView = findViewById<RecyclerView>(R.id.assignmentsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        allAssignments = mutableListOf(
            Assignment("1", "Einsatz Zentrale", "Unterstützung bei IT-Infrastruktur", AssignmentStatus.NEW),
            Assignment("2", "Kunde SPMG", "Wartung der ERP-Systeme", AssignmentStatus.ONGOING),
            Assignment("3", "Projekt Website", "Anpassung der mobilen Ansicht", AssignmentStatus.COMPLETED)
        )

        adapter = AssignmentsAdapter(
            buildAdapterItems(allAssignments),
            onOpenClick = { assignment ->
                val intent = Intent(this, AssignmentDetailActivity::class.java).apply {
                    putExtra("assignmentId", assignment.id)
                    putExtra("title", assignment.title)
                    putExtra("details", assignment.details)
                    putExtra("status", assignment.status.name)
                }
                detailActivityLauncher.launch(intent)
            },
            onDismissClick = { assignment ->
                Toast.makeText(this, "API: Status 'Unable to Respond' gesendet", Toast.LENGTH_SHORT).show()
                allAssignments.remove(assignment)
                adapter.setData(buildAdapterItems(allAssignments))
            },
            onReportClick = { assignment ->
                val intent = Intent(this, AssignmentReportActivity::class.java).apply {
                    putExtra("title", assignment.title)
                }
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun buildAdapterItems(assignments: List<Assignment>): List<AdapterItem> {
        val items = mutableListOf<AdapterItem>()
        items.add(AdapterItem.Header("Neueste Einsätze", AssignmentStatus.NEW))
        items.addAll(assignments.filter { it.status == AssignmentStatus.NEW }.map { AdapterItem.AssignmentItem(it) })
        items.add(AdapterItem.Header("Laufende Einsätze", AssignmentStatus.ONGOING))
        items.addAll(assignments.filter { it.status == AssignmentStatus.ONGOING }.map { AdapterItem.AssignmentItem(it) })
        items.add(AdapterItem.Header("Abgeschlossene Einsätze", AssignmentStatus.COMPLETED))
        items.addAll(assignments.filter { it.status == AssignmentStatus.COMPLETED }.map { AdapterItem.AssignmentItem(it) })
        return items
    }
}
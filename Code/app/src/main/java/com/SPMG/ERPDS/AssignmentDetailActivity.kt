package com.SPMG.ERPDS

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AssignmentDetailActivity : AppCompatActivity() {

    data class UnitInfo(val callsign: String, val organization: String, val status: String)
    private var isAccepted = false
    private var creationTime: String = ""
    private var acceptanceTime: String = ""
    private var assignmentId: String = ""

    private fun getCurrentTimestamp(pattern: String = "dd.MM.yyyy HH:mm"): String {
        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("Europe/Vienna")
        return sdf.format(java.util.Date())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_assignment_detail)
        
        assignmentId = intent.getStringExtra("assignmentId") ?: ""
        val initialStatus = intent.getStringExtra("status") ?: "NEW"
        isAccepted = initialStatus == "ONGOING"
        
        // Use current time in Vienna TimeZone as creation/received time
        creationTime = intent.getStringExtra("creationTime") ?: getCurrentTimestamp()
        acceptanceTime = intent.getStringExtra("acceptanceTime") ?: ""

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val title = intent.getStringExtra("title") ?: "Unbekannter Einsatz"
        val details = intent.getStringExtra("details") ?: ""

        findViewById<TextView>(R.id.detailStichwort).text = title
        findViewById<TextView>(R.id.detailNummer).text = getString(R.string.assignment_number_prefix, "2024-001")
        findViewById<TextView>(R.id.detailUhrzeit).text = getString(R.string.time_label_format, getString(R.string.label_uhrzeit), getCurrentTimestamp("HH:mm"))
        findViewById<TextView>(R.id.detailOrt).text = getString(R.string.placeholder_location)
        findViewById<TextView>(R.id.detailBeschreibung).text = details

        val unitsContainer = findViewById<LinearLayout>(R.id.unitsContainer)
        
        val otherUnits = listOf(
            UnitInfo("Florian Muster 1/44-1", "FF", "Anfahrt"),
            UnitInfo("Rettung Muster 1/83-1", "RD", "An Einsatzstelle")
        )

        otherUnits.forEach { addUnitToLayout(it, unitsContainer) }

        val actionButton = findViewById<Button>(R.id.btnAcceptAssignment)
        
        if (isAccepted) {
            actionButton.text = getString(R.string.btn_complete_assignment)
            addUnitToLayout(UnitInfo("SPMG Mobil 1", "SPMG", getString(R.string.unit_accepted_format, acceptanceTime)), unitsContainer)
        }

        actionButton.setOnClickListener {
            val currentTime = getCurrentTimestamp("HH:mm")
            
            if (!isAccepted) {
                // PHASE 1: ACCEPT
                isAccepted = true
                acceptanceTime = currentTime
                val ownUnit = UnitInfo("SPMG Mobil 1", "SPMG", getString(R.string.unit_accepted_format, acceptanceTime))
                addUnitToLayout(ownUnit, unitsContainer)
                
                actionButton.text = getString(R.string.btn_complete_assignment)
                
                // Return result to MainActivity
                val resultIntent = Intent().apply {
                    putExtra("assignmentId", assignmentId)
                    putExtra("newStatus", "ONGOING")
                    putExtra("acceptanceTime", acceptanceTime)
                }
                setResult(RESULT_OK, resultIntent)
                
                Toast.makeText(this, R.string.btn_accept_assignment, Toast.LENGTH_SHORT).show()
            } else {
                // PHASE 2: COMPLETE
                val completionTime = getCurrentTimestamp()
                
                // Return result to MainActivity
                val resultIntent = Intent().apply {
                    putExtra("assignmentId", assignmentId)
                    putExtra("newStatus", "COMPLETED")
                    putExtra("completionTime", completionTime)
                }
                setResult(RESULT_OK, resultIntent)

                val intent = Intent(this, AssignmentReportActivity::class.java).apply {
                    putExtra("title", title)
                    putExtra("creationTime", creationTime)
                    putExtra("acceptanceTime", acceptanceTime)
                    putExtra("completionTime", completionTime)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun addUnitToLayout(unit: UnitInfo, container: LinearLayout) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_unit, container, false)
        view.findViewById<TextView>(R.id.unitInfo).text = getString(R.string.unit_info_format, unit.callsign, unit.organization)
        view.findViewById<TextView>(R.id.unitStatus).text = unit.status
        container.addView(view)
    }
}
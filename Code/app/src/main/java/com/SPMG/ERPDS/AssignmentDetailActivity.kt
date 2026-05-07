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

    private fun getCurrentTimestamp(pattern: String = "dd.MM.yyyy HH:mm"): String {
        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("Europe/Vienna")
        return sdf.format(java.util.Date())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_assignment_detail)
        
        // Use current time in Vienna TimeZone as creation/received time
        creationTime = getCurrentTimestamp()
        
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
        findViewById<TextView>(R.id.detailNummer).text = "Einsatz-Nr: 2024-001"
        findViewById<TextView>(R.id.detailUhrzeit).text = "${getCurrentTimestamp("HH:mm")} Uhr"
        findViewById<TextView>(R.id.detailOrt).text = "Musterstraße 12, 12345 Musterstadt"
        findViewById<TextView>(R.id.detailBeschreibung).text = details

        val unitsContainer = findViewById<LinearLayout>(R.id.unitsContainer)
        
        val otherUnits = listOf(
            UnitInfo("Florian Muster 1/44-1", "FF", "Anfahrt"),
            UnitInfo("Rettung Muster 1/83-1", "RD", "An Einsatzstelle")
        )

        otherUnits.forEach { addUnitToLayout(it, unitsContainer) }

        val actionButton = findViewById<Button>(R.id.btnAcceptAssignment)
        actionButton.setOnClickListener {
            val currentTime = getCurrentTimestamp("HH:mm")
            
            if (!isAccepted) {
                // PHASE 1: ACCEPT
                isAccepted = true
                acceptanceTime = currentTime
                val ownUnit = UnitInfo("SPMG Mobil 1", "SPMG", "Einsatz angenommen um $acceptanceTime")
                addUnitToLayout(ownUnit, unitsContainer)
                
                actionButton.text = "Einsatz abschließen"
                Toast.makeText(this, "Einsatz angenommen", Toast.LENGTH_SHORT).show()
            } else {
                // PHASE 2: COMPLETE
                val completionTime = getCurrentTimestamp()
                val intent = Intent(this, AssignmentReportActivity::class.java).apply {
                    putExtra("title", title)
                    putExtra("creationTime", creationTime)
                    putExtra("acceptanceTime", acceptanceTime)
                    putExtra("completionTime", completionTime)
                }
                startActivity(intent)
                finish() // Close details after completion
            }
        }
    }

    private fun addUnitToLayout(unit: UnitInfo, container: LinearLayout) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_unit, container, false)
        view.findViewById<TextView>(R.id.unitInfo).text = "${unit.callsign} (${unit.organization}):"
        view.findViewById<TextView>(R.id.unitStatus).text = unit.status
        container.addView(view)
    }
}
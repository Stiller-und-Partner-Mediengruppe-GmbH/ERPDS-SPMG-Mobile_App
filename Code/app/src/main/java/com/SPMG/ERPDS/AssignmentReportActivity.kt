package com.SPMG.ERPDS

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AssignmentReportActivity : AppCompatActivity() {
    
    data class UnitReport(val name: String, val alarmTime: String, val arrivalTime: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_assignment_report)

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
        val creationTime = intent.getStringExtra("creationTime") ?: "Unbekannt"
        val completionTime = intent.getStringExtra("completionTime") ?: "Unbekannt"
        val acceptanceTime = intent.getStringExtra("acceptanceTime") ?: "Unbekannt"
        
        findViewById<TextView>(R.id.reportTitle).text = "Bericht: $title"
        findViewById<TextView>(R.id.reportDate).text = "Datum: ${creationTime.split(" ")[0]}"
        
        // Detailed timestamps from System Time
        findViewById<TextView>(R.id.reportTimestamps).text = 
            "Erstellt: $creationTime\nAbgeschlossen: $completionTime"
            
        findViewById<TextView>(R.id.reportSummary).text = 
            "Der Einsatz wurde erfolgreich abgeschlossen. Alle Systeme der $title sind wieder betriebsbereit."

        val container = findViewById<LinearLayout>(R.id.reportUnitsContainer)
        
        val units = listOf(
            UnitReport("SPMG Mobil 1 (SPMG)", acceptanceTime, acceptanceTime), // Vereinfacht für Beispiel
            UnitReport("Florian Muster 1/44-1 (FF)", "14:16", "14:25"),
            UnitReport("Rettung Muster 1/83-1 (RD)", "14:16", "14:28")
        )

        val inflater = LayoutInflater.from(this)
        units.forEach { unit ->
            val view = inflater.inflate(R.layout.item_unit_report, container, false)
            view.findViewById<TextView>(R.id.unitTitle).text = unit.name
            view.findViewById<TextView>(R.id.unitTimes).text = "Alarmiert: ${unit.alarmTime} | Eingetroffen: ${unit.arrivalTime}"
            container.addView(view)
        }
    }
}
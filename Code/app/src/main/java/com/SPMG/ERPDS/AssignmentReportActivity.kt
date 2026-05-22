package com.spmg.erpds

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray

class AssignmentReportActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val assignmentId = intent.getStringExtra("assignmentId") ?: ""
        val title = intent.getStringExtra("title") ?: getString(R.string.unknown)
        val number = intent.getStringExtra("number") ?: "2024-000"
        val location = intent.getStringExtra("location") ?: getString(R.string.placeholder_location)
        val creationTime = intent.getStringExtra("creationTime") ?: getString(R.string.unknown)
        val completionTime = intent.getStringExtra("completionTime") ?: getString(R.string.unknown)
        val acceptanceTime = intent.getStringExtra("acceptanceTime") ?: getString(R.string.unknown)
        val ownCallsign = intent.getStringExtra("ownCallsign") ?: "Persönliche Kennung"
        val notes = intent.getStringExtra("notes") ?: ""
        
        findViewById<TextView>(R.id.reportTitle).text = getString(R.string.report_title_prefix, "$number - $title")
        findViewById<TextView>(R.id.reportDate).text = getString(R.string.report_date_prefix, if (creationTime != getString(R.string.unknown)) creationTime.split(" ")[0] else getString(R.string.placeholder_date))
        
        findViewById<TextView>(R.id.reportTimestamps).text = 
            getString(R.string.report_timestamps_format, creationTime, completionTime)
            
        val summaryBuilder = StringBuilder()
        summaryBuilder.append("Einsatzort: $location\n\n")
        summaryBuilder.append(getString(R.string.report_summary_header)).append("\n")
        
        if (notes.isNotEmpty()) {
            summaryBuilder.append(notes)
        } else {
            summaryBuilder.append(getString(R.string.report_summary_default))
        }
        findViewById<TextView>(R.id.reportSummary).text = summaryBuilder.toString()

        val container = findViewById<LinearLayout>(R.id.reportUnitsContainer)
        
        // 1. Load structured units from persistence for this assignment
        val units = mutableListOf<UnitData>()
        val prefs = getSharedPreferences("AssignmentPrefs", MODE_PRIVATE)
        val json = prefs.getString("AssignmentsJson", null)
        if (json != null) {
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") == assignmentId) {
                        val unitsArray = obj.optJSONArray("units")
                        if (unitsArray != null) {
                            for (j in 0 until unitsArray.length()) {
                                val uo = unitsArray.getJSONObject(j)
                                units.add(UnitData(
                                    uo.getString("callsign"), uo.getString("organization"),
                                    uo.optString("status", ""), uo.optString("alarmTime", ""), uo.optString("arrivalTime", "")
                                ))
                            }
                        }
                        break
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // 2. Add the user's unit for the report
        val ownArrival = acceptanceTime.ifEmpty { getString(R.string.not_arrived) }
        val ownAlarm = try { creationTime.split(" ")[1] } catch (_: Exception) { creationTime }
        units.add(0, UnitData(ownCallsign, "SPMG", "Eingetroffen", ownAlarm, ownArrival))

        val inflater = LayoutInflater.from(this)
        units.forEach { unit ->
            val view = inflater.inflate(R.layout.item_unit_report, container, false)
            view.findViewById<TextView>(R.id.unitTitle).text = getString(R.string.unit_info_format, unit.callsign, unit.organization)
            
            val timeInfo = if (unit.arrivalTime.isNotEmpty()) {
                getString(R.string.unit_report_times_format, unit.alarmTime, unit.arrivalTime)
            } else {
                "Alarmiert: ${unit.alarmTime} | Status: ${unit.status}"
            }
            view.findViewById<TextView>(R.id.unitTimes).text = timeInfo
            container.addView(view)
        }
    }
}

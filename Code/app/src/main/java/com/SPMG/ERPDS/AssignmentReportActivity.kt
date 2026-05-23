package com.spmg.erpds

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import java.io.OutputStream

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
        
        var title = intent.getStringExtra("title") ?: ""
        var number = intent.getStringExtra("number") ?: ""
        var location = intent.getStringExtra("location") ?: ""
        var creationTime = intent.getStringExtra("creationTime") ?: ""
        var completionTime = intent.getStringExtra("completionTime") ?: ""
        var acceptanceTime = intent.getStringExtra("acceptanceTime") ?: ""
        var ownCallsign = intent.getStringExtra("ownCallsign") ?: ""
        var notes = intent.getStringExtra("notes") ?: ""

        val units = mutableListOf<UnitData>()
        
        // 1. Load data from persistence for this assignment to ensure no info loss
        val prefs = getSharedPreferences("AssignmentPrefs", MODE_PRIVATE)
        val json = prefs.getString("AssignmentsJson", null)
        if ((json != null) && assignmentId.isNotEmpty()) {
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") == assignmentId) {
                        if (title.isEmpty()) title = obj.optString("title", getString(R.string.unknown))
                        if (number.isEmpty()) number = obj.optString("number", "2024-000")
                        if (location.isEmpty()) location = obj.optString("location", getString(R.string.placeholder_location))
                        if (creationTime.isEmpty()) creationTime = obj.optString("creationTime", getString(R.string.unknown))
                        if (completionTime.isEmpty()) completionTime = obj.optString("completionTime", getString(R.string.unknown))
                        if (acceptanceTime.isEmpty()) acceptanceTime = obj.optString("acceptanceTime", getString(R.string.unknown))
                        if (ownCallsign.isEmpty()) ownCallsign = obj.optString("callsign", "Persönliche Kennung")
                        if (notes.isEmpty()) notes = obj.optString("notes", "")
                        
                        val unitsArray = obj.optJSONArray("units")
                        if (unitsArray != null) {
                            for (j in 0 until unitsArray.length()) {
                                val uo = unitsArray.getJSONObject(j)
                                units.add(
                                    UnitData(
                                        uo.getString("callsign"),
                                        uo.getString("organization"),
                                        uo.optString("status", ""),
                                        uo.optString("alarmTime", ""),
                                        uo.optString("arrivalTime", ""),
                                    ),
                                )
                            }
                        }
                        break
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // Fallbacks for direct intent calls without ID match
        if (title.isEmpty()) title = getString(R.string.unknown)
        if (number.isEmpty()) number = "2024-000"
        if (location.isEmpty()) location = getString(R.string.placeholder_location)
        if (creationTime.isEmpty()) creationTime = getString(R.string.unknown)
        if (completionTime.isEmpty()) completionTime = getString(R.string.unknown)

        findViewById<TextView>(R.id.reportTitle).text = getString(R.string.report_title_prefix, "$number - $title")
        findViewById<TextView>(R.id.reportDate).text = getString(R.string.report_date_prefix, if (creationTime != getString(R.string.unknown)) creationTime.split(" ")[0] else getString(R.string.placeholder_date))
        
        findViewById<TextView>(R.id.reportTimestamps).text = 
            getString(R.string.report_timestamps_format, creationTime, completionTime)
            
        val summaryBuilder = StringBuilder()
        summaryBuilder.append("Einsatzort: $location\n\n")
        if (notes.isNotEmpty()) {
            summaryBuilder.append("Zusammenfassung und Protokoll\n")
            summaryBuilder.append(notes).append("\n\n")
        } else {
            summaryBuilder.append(getString(R.string.report_summary_header)).append("\n")
            summaryBuilder.append(getString(R.string.report_summary_default))
        }
        findViewById<TextView>(R.id.reportSummary).text = summaryBuilder.toString()

        val container = findViewById<LinearLayout>(R.id.reportUnitsContainer)

        // 2. Add the user's unit for the report (only if not already in the list from persistence)
        val alreadyInList = units.any { it.callsign == ownCallsign }
        if (!alreadyInList && ownCallsign.isNotEmpty()) {
            val ownArrival = acceptanceTime.ifEmpty { getString(R.string.not_arrived) }
            val ownAlarm = try { creationTime.split(" ")[1] } catch (_: Exception) { creationTime }
            units.add(0, UnitData(ownCallsign, "SPMG", "Eingetroffen", ownAlarm, ownArrival))
        }

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

        findViewById<Button>(R.id.btnSaveToFile).setOnClickListener {
            saveReportToFile(number, title, location, creationTime, completionTime, summaryBuilder.toString(), units)
        }

        // Automatic persistent save on creation (only if not from history to avoid unnecessary disk I/O)
        val isFromHistory = intent.getBooleanExtra("isFromHistory", false)
        if (!isFromHistory) {
            saveReportToFile(number, title, location, creationTime, completionTime, summaryBuilder.toString(), units, silent = true)
        }
    }

    private fun saveReportToFile(
        number: String,
        title: String,
        location: String,
        creationTime: String,
        completionTime: String,
        summary: String,
        units: List<UnitData>,
        silent: Boolean = false,
    ) {
        val fileName = "Bericht_${number.replace("-", "_")}.txt"
        val relativePath = Environment.DIRECTORY_DOCUMENTS + "/ERPDS_Reports"
        
        val reportContent = StringBuilder().apply {
            append("========================================\n")
            append("      EINSATZPROTOKOLL - ERPDS          \n")
            append("========================================\n\n")
            append("Einsatz-Nr:   $number\n")
            append("Stichwort:    $title\n")
            append("Einsatzort:   $location\n\n")
            append("Zeitstempel:\n")
            append("  Alarmiert:      $creationTime\n")
            append("  Abgeschlossen:  $completionTime\n\n")
            append("----------------------------------------\n")
            append("PROTOKOLL & NOTIZEN:\n")
            append(summary).append("\n\n")
            append("----------------------------------------\n")
            append("EINHEITENÜBERSICHT:\n")
            units.forEach { unit ->
                append("- ${unit.callsign} (${unit.organization})\n")
                if (unit.arrivalTime.isNotEmpty()) {
                    append("  Status: Eingetroffen um ${unit.arrivalTime}\n")
                } else {
                    append("  Status: ${unit.status}\n")
                }
            }
            append("\n========================================\n")
            append("Erstellt am: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        }.toString()

        try {
            val resolver = contentResolver
            val contentUri = MediaStore.Files.getContentUri("external")
            
            // Check if file already exists to avoid duplicates. 
            // We use DISPLAY_NAME and RELATIVE_PATH to find the existing entry.
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND (${MediaStore.MediaColumns.RELATIVE_PATH} = ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} = ?)"
            val selectionArgs = arrayOf(fileName, relativePath, "$relativePath/")
            val cursor = resolver.query(contentUri, arrayOf(MediaStore.MediaColumns._ID), selection, selectionArgs, null)
            
            val uri: android.net.Uri? = if ((cursor != null) && cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                cursor.close()
                android.content.ContentUris.withAppendedId(contentUri, id)
            } else {
                cursor?.close()
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
                resolver.insert(contentUri, contentValues)
            }

            if (uri != null) {
                resolver.openOutputStream(uri, "wt")?.use { outputStream: OutputStream ->
                    outputStream.write(reportContent.toByteArray())
                }
                if (!silent) {
                    Toast.makeText(this, R.string.msg_report_saved, Toast.LENGTH_LONG).show()
                }
            } else if (!silent) {
                Toast.makeText(this, R.string.msg_save_failed, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (!silent) {
                Toast.makeText(this, "${getString(R.string.msg_save_failed)}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

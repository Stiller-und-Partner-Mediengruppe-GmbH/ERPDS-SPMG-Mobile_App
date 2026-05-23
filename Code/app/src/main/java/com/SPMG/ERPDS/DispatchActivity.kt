package com.spmg.erpds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class DispatchActivity : BaseActivity() {
    
    companion object {
        private const val PREFS_ASSIGNMENTS = "AssignmentPrefs"
        private const val KEY_DATA = "AssignmentsJson"
        private const val KEY_COUNTER = "AssignmentCounter"
    }

    private lateinit var allAssignments: MutableList<Assignment>
    private lateinit var adapter: AssignmentsAdapter
    private var tempAssignedUnits = mutableListOf<UnitData>()

    private val detailActivityLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val id = data?.getStringExtra("assignmentId")
            val newStatusStr = data?.getStringExtra("newStatus")
            
            if ((id != null) && (newStatusStr != null)) {
                val newStatus = AssignmentStatus.valueOf(newStatusStr)
                val assignment = allAssignments.find { it.id == id }
                assignment?.let {
                    it.status = newStatus
                    it.notes = LanguageUtils.sanitizeGermanText(data.getStringExtra("notes") ?: it.notes)
                    it.callsign = LanguageUtils.sanitizeGermanText(data.getStringExtra("callsign") ?: it.callsign)
                    
                    if (newStatus == AssignmentStatus.ONGOING) {
                        it.acceptanceTime = data.getStringExtra("acceptanceTime") ?: it.acceptanceTime
                    } else if (newStatus == AssignmentStatus.COMPLETED) {
                        it.completionTime = data.getStringExtra("completionTime") ?: it.completionTime
                    }
                    saveAssignments()
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

        loadAssignments()

        adapter = AssignmentsAdapter(
            buildAdapterItems(allAssignments),
            onOpenClick = { assignment ->
                val intent = Intent(this, AssignmentDetailActivity::class.java).apply {
                    putExtra("assignmentId", assignment.id)
                    putExtra("number", assignment.number)
                    putExtra("title", assignment.title)
                    putExtra("details", assignment.details)
                    putExtra("location", assignment.location)
                    putExtra("status", assignment.status.name)
                    putExtra("creationTime", assignment.creationTime)
                    putExtra("acceptanceTime", assignment.acceptanceTime)
                    putExtra("callsign", assignment.callsign)
                    putExtra("notes", assignment.notes)
                }
                detailActivityLauncher.launch(intent)
            },
            onDismissClick = { assignment ->
                Toast.makeText(this, "API: Status 'Unable to Respond' gesendet", Toast.LENGTH_SHORT).show()
                allAssignments.remove(assignment)
                saveAssignments()
                adapter.setData(buildAdapterItems(allAssignments))
            },
        ) { assignment ->
            val intent = Intent(this, AssignmentReportActivity::class.java).apply {
                putExtra("assignmentId", assignment.id)
                putExtra("isFromHistory", true)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddAssignment).setOnClickListener {
            tempAssignedUnits.clear()
            showAddWorkflowStep1()
        }
    }

    private fun showAddWorkflowStep1() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 32)
        }
        val tvLabel = TextView(this).apply {
            text = getString(R.string.label_select_department)
            setPadding(0, 0, 0, 16)
        }
        tvLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
        val rgDept = RadioGroup(this)
        val rbEMS = RadioButton(this).apply { text = getString(R.string.dept_ems); id = View.generateViewId() }
        val rbSec = RadioButton(this).apply { text = getString(R.string.dept_security); id = View.generateViewId() }
        val rbFire = RadioButton(this).apply { text = getString(R.string.dept_fire); id = View.generateViewId() }
        rgDept.addView(rbEMS); rgDept.addView(rbSec); rgDept.addView(rbFire)
        rbEMS.isChecked = true
        container.addView(tvLabel); container.addView(rgDept)

        AlertDialog.Builder(this)
            .setTitle("Einsatz erstellen - Schritt 1/3")
            .setView(container)
            .setPositiveButton("Weiter") { _, _ ->
                val dept = when {
                    rbEMS.isChecked -> "EMS"
                    rbSec.isChecked -> "SEC"
                    else -> "FIRE"
                }
                showAddWorkflowStep2(dept)
            }
            .setNegativeButton(R.string.btn_cancel, null).show()
    }

    private fun showAddWorkflowStep2(dept: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 32)
        }
        val etTitle = EditText(this).apply { hint = getString(R.string.hint_assignment_title); LanguageUtils.configureGermanInput(this) }
        val etLocation = EditText(this).apply { hint = getString(R.string.hint_assignment_location); LanguageUtils.configureGermanInput(this) }
        val etSummary = EditText(this).apply { hint = "Sachverhaltsbeschreibung"; setLines(3); LanguageUtils.configureGermanInput(this) }
        val etDetails = EditText(this).apply { hint = getString(R.string.hint_assignment_details); setLines(2); LanguageUtils.configureGermanInput(this) }
        container.addView(etTitle); container.addView(etLocation); container.addView(etSummary); container.addView(etDetails)

        AlertDialog.Builder(this)
            .setTitle("Einsatz erstellen - Schritt 2/3")
            .setView(container)
            .setPositiveButton("Weiter") { _, _ ->
                val title = LanguageUtils.sanitizeGermanText(etTitle.text.toString())
                if (title.isNotEmpty()) {
                    showAddWorkflowStep3(dept, title, etLocation.text.toString(), etSummary.text.toString(), etDetails.text.toString())
                } else {
                    Toast.makeText(this, "Stichwort erforderlich", Toast.LENGTH_SHORT).show()
                    showAddWorkflowStep2(dept)
                }
            }
            .setNegativeButton("Zurück") { _, _ -> showAddWorkflowStep1() }.show()
    }

    private fun showAddWorkflowStep3(dept: String, title: String, location: String, summary: String, details: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 32)
        }
        val tvLabel = TextView(this).apply {
            text = getString(R.string.label_einheiten)
            setPadding(0, 0, 0, 16)
        }
        tvLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
        
        val unitsSummary = TextView(this).apply {
            text = if (tempAssignedUnits.isEmpty()) getString(R.string.label_no_units_assigned) else getString(R.string.label_assigned_units_count, tempAssignedUnits.size)
            setPadding(0, 0, 0, 16)
        }

        val btnAddUnits = android.widget.Button(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(R.string.btn_add_units)
            setOnClickListener {
                UnitAssignmentDialog(this@DispatchActivity) { units ->
                    tempAssignedUnits.addAll(units)
                    unitsSummary.text = getString(R.string.label_assigned_units_count, tempAssignedUnits.size)
                }.show()
            }
        }

        container.addView(tvLabel)
        container.addView(unitsSummary)
        container.addView(btnAddUnits)

        AlertDialog.Builder(this)
            .setTitle("Einsatz erstellen - Schritt 3/3")
            .setView(container)
            .setPositiveButton("Einsatz anlegen") { _, _ ->
                val deptId = when(dept) { "EMS" -> "144" ; "SEC" -> "133"; else -> "122" }
                val generatedNumber = generateAssignmentNumber(deptId)
                val now = getCurrentTimestamp()

                val newAssignment = Assignment(
                    id = UUID.randomUUID().toString(),
                    number = generatedNumber,
                    title = title,
                    details = details,
                    location = location,
                    status = AssignmentStatus.NEW,
                    creationTime = now,
                    notes = "Ausgangssituation: $summary",
                    assignedUnits = tempAssignedUnits.toMutableList(),
                )
                
                allAssignments.add(0, newAssignment)
                saveAssignments()
                adapter.setData(buildAdapterItems(allAssignments))
                Toast.makeText(this, "Einsatz $generatedNumber angelegt", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Zurück") { _, _ -> showAddWorkflowStep2(dept) }.show()
    }

    private fun generateAssignmentNumber(deptId: String): String {
        val prefs = getSharedPreferences(PREFS_ASSIGNMENTS, MODE_PRIVATE)
        val counter = prefs.getInt(KEY_COUNTER, 0) + 1
        prefs.edit { putInt(KEY_COUNTER, counter) }
        val datePart = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Europe/Vienna")
        }.format(Date())
        return "$datePart-$deptId-$counter"
    }

    private fun saveAssignments() {
        val array = JSONArray()
        allAssignments.forEach { ass ->
            val obj = JSONObject().apply {
                put("id", ass.id); put("number", ass.number); put("title", ass.title); put("details", ass.details)
                put("location", ass.location); put("status", ass.status.name); put("creationTime", ass.creationTime)
                put("acceptanceTime", ass.acceptanceTime); put("completionTime", ass.completionTime)
                put("callsign", ass.callsign); put("notes", ass.notes)
                
                val unitsArray = JSONArray()
                ass.assignedUnits.forEach { unit ->
                    val unitObj = JSONObject().apply {
                        put("callsign", unit.callsign)
                        put("organization", unit.organization)
                        put("status", unit.status)
                        put("alarmTime", unit.alarmTime)
                        put("arrivalTime", unit.arrivalTime)
                    }
                    unitsArray.put(unitObj)
                }
                put("units", unitsArray)
            }
            array.put(obj)
        }
        getSharedPreferences(PREFS_ASSIGNMENTS, MODE_PRIVATE).edit { putString(KEY_DATA, array.toString()) }
    }

    private fun loadAssignments() {
        val prefs = getSharedPreferences(PREFS_ASSIGNMENTS, MODE_PRIVATE)
        val json = prefs.getString(KEY_DATA, null)
        allAssignments = mutableListOf()
        if (json != null) {
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val ass = Assignment(
                        id = obj.getString("id"), number = obj.optString("number", ""),
                        title = obj.getString("title"), details = obj.getString("details"),
                        location = obj.optString("location", ""),
                        status = AssignmentStatus.valueOf(obj.getString("status")),
                        creationTime = obj.optString("creationTime", ""),
                        acceptanceTime = obj.optString("acceptanceTime", ""),
                        completionTime = obj.optString("completionTime", ""),
                        callsign = obj.optString("callsign", ""),
                        notes = obj.optString("notes", ""),
                    )
                    val unitsArray = obj.optJSONArray("units")
                    if (unitsArray != null) {
                        for (j in 0 until unitsArray.length()) {
                            val uo = unitsArray.getJSONObject(j)
                            ass.assignedUnits.add(
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
                    allAssignments.add(ass)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (allAssignments.isEmpty()) {
            val now = getCurrentTimestamp()
            val ass1 = Assignment("1", "20240522-1400-144-1", "Einsatz Zentrale", "Unterstützung bei IT-Infrastruktur", "SPMG Zentrale, Wien", AssignmentStatus.NEW, creationTime = now)
            ass1.assignedUnits.add(UnitData("Florian 1/44", "FIRE", "Alarmiert", "14:00", ""))
            allAssignments.add(ass1)
            saveAssignments()
        }
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Europe/Vienna")
        return sdf.format(Date())
    }

    private fun buildAdapterItems(assignments: List<Assignment>): List<AdapterItem> {
        val items = mutableListOf<AdapterItem>()
        items.add(AdapterItem.Header("Neueste Einsätze", AssignmentStatus.NEW))
        items.addAll(
            assignments.asSequence()
                .filter { it.status == AssignmentStatus.NEW }
                .map { AdapterItem.AssignmentItem(it) }
                .toList(),
        )
        items.add(AdapterItem.Header("Laufende Einsätze", AssignmentStatus.ONGOING))
        items.addAll(
            assignments.asSequence()
                .filter { it.status == AssignmentStatus.ONGOING }
                .map { AdapterItem.AssignmentItem(it) }
                .toList(),
        )
        items.add(AdapterItem.Header("Abgeschlossene Einsätze", AssignmentStatus.COMPLETED))
        items.addAll(
            assignments.asSequence()
                .filter { it.status == AssignmentStatus.COMPLETED }
                .map { AdapterItem.AssignmentItem(it) }
                .toList(),
        )
        return items
    }
}

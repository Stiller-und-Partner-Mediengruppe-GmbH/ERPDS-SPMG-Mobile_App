package com.spmg.erpds

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AssignmentDetailActivity : BaseActivity() {

    private var isAccepted = false
    private var creationTime: String = ""
    private var acceptanceTime: String = ""
    private var assignmentId: String = ""
    private var assignmentNumber: String = ""
    private var assignmentLocation: String = ""
    private var activeCallsign: String = ""
    private var assignmentNotes: String = ""
    private var arrivalTime: String = ""
    private val assignedUnitsList = mutableListOf<UnitData>()

    private val fleetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        checkIdentitySync()
    }

    private fun getCurrentTimestamp(pattern: String = "dd.MM.yyyy HH:mm"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Europe/Vienna")
        return sdf.format(Date())
    }

    private fun isTimeTrackingActive(): Boolean {
        val checkInPrefs = getSharedPreferences("CheckInPrefs", MODE_PRIVATE)
        return checkInPrefs.getBoolean("IsCheckedIn", false)
    }

    private lateinit var etOngoingNotes: EditText
    private lateinit var notesLayout: View
    private lateinit var labelOngoingNotes: View
    private lateinit var notesDivider: View
    private lateinit var btnAccept: Button
    private lateinit var btnArrived: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignment_detail)
        
        assignmentId = intent.getStringExtra("assignmentId") ?: ""
        assignmentNumber = intent.getStringExtra("number") ?: "2024-000"
        val initialStatus = intent.getStringExtra("status") ?: "NEW"
        isAccepted = initialStatus == "ONGOING"
        
        creationTime = intent.getStringExtra("creationTime") ?: getCurrentTimestamp()
        acceptanceTime = intent.getStringExtra("acceptanceTime") ?: ""
        activeCallsign = intent.getStringExtra("callsign") ?: ""
        assignmentNotes = intent.getStringExtra("notes") ?: ""
        assignmentLocation = intent.getStringExtra("location") ?: getString(R.string.placeholder_location)

        etOngoingNotes = findViewById(R.id.etOngoingNotes)
        notesLayout = findViewById(R.id.ongoingNotesLayout)
        labelOngoingNotes = findViewById(R.id.labelOngoingNotes)
        notesDivider = findViewById(R.id.notesDivider)
        btnAccept = findViewById(R.id.btnAcceptAssignment)
        btnArrived = findViewById(R.id.btnArrivedAtScene)

        loadUnitsFromPersistence()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<TextView>(R.id.detailStichwort).text = intent.getStringExtra("title") ?: getString(R.string.unknown)
        findViewById<TextView>(R.id.detailNummer).text = getString(R.string.assignment_number_prefix, assignmentNumber)
        
        val displayTime = if (creationTime.isNotEmpty()) {
            try { creationTime.split(" ")[1] } catch (_: Exception) { getCurrentTimestamp("HH:mm") }
        } else { getCurrentTimestamp("HH:mm") }
        
        findViewById<TextView>(R.id.detailUhrzeit).text = getString(R.string.time_label_format, getString(R.string.label_uhrzeit), displayTime)
        findViewById<TextView>(R.id.detailOrt).text = assignmentLocation
        findViewById<TextView>(R.id.detailBeschreibung).text = intent.getStringExtra("details") ?: ""

        val unitsContainer = findViewById<LinearLayout>(R.id.unitsContainer)
        refreshUnitsDisplay(unitsContainer)

        updateNotesFieldVisibility()

        etOngoingNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveCurrentState()
            }
        })

        val fleetPrefs = getSharedPreferences("FleetPrefs", MODE_PRIVATE)
        val currentVehicle = fleetPrefs.getString("CurrentVehicle", null)
        
        if (isAccepted) {
            if (activeCallsign.isEmpty()) {
                activeCallsign = currentVehicle ?: "Persönliche Kennung"
            }
            btnAccept.text = getString(R.string.btn_complete_assignment)
            btnArrived.visibility = if (arrivalTime.isEmpty()) View.VISIBLE else View.GONE
            checkIdentitySync()
        }

        btnArrived.setOnClickListener {
            performArrival()
        }

        btnAccept.setOnClickListener {
            if (!isTimeTrackingActive()) {
                Toast.makeText(this, R.string.error_no_active_shift, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!isAccepted) {
                if (currentVehicle != null) {
                    performAcceptance(currentVehicle, unitsContainer, btnAccept)
                } else {
                    showIdentityPrompt(unitsContainer, btnAccept)
                }
            } else {
                showCompletionDialog()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAccepted) {
            saveCurrentState()
        }
    }

    private fun performArrival() {
        if (arrivalTime.isNotEmpty()) {
            Toast.makeText(this, "Ankunft bereits vermerkt", Toast.LENGTH_SHORT).show()
            return
        }
        
        arrivalTime = getCurrentTimestamp("HH:mm")
        val entry = "Am BE eingetroffen um $arrivalTime"
        assignmentNotes = if (assignmentNotes.isEmpty()) entry else "$assignmentNotes\n$entry"
        
        refreshUnitsDisplay(findViewById(R.id.unitsContainer))
        saveCurrentState()
        btnArrived.visibility = View.GONE
        Toast.makeText(this, "Ankunft am BE vermerkt", Toast.LENGTH_SHORT).show()
    }

    private var completionTime: String = ""

    private fun saveCurrentState(finalStatus: String? = null) {
        val prefs = getSharedPreferences("AssignmentPrefs", MODE_PRIVATE)
        val json = prefs.getString("AssignmentsJson", null) ?: return
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("id") == assignmentId) {
                    val statusToSave = finalStatus ?: if (isAccepted) "ONGOING" else "NEW"
                    obj.put("status", statusToSave)
                    obj.put("callsign", activeCallsign)
                    obj.put("acceptanceTime", acceptanceTime)
                    obj.put("completionTime", completionTime)
                    obj.put("notes", assignmentNotes)
                    obj.put("ongoingInput", etOngoingNotes.text.toString())
                    
                    val unitsArray = obj.optJSONArray("units") ?: JSONArray()
                    var found = false
                    for (j in 0 until unitsArray.length()) {
                        val uo = unitsArray.getJSONObject(j)
                        if (uo.getString("callsign") == activeCallsign) {
                            if (arrivalTime.isNotEmpty()) {
                                uo.put("status", getString(R.string.status_arrived_at_scene, arrivalTime))
                                uo.put("arrivalTime", arrivalTime)
                            } else if (statusToSave == "COMPLETED") {
                                uo.put("status", "Abgeschlossen")
                            }
                            found = true
                            break
                        }
                    }
                    if (!found && isAccepted && activeCallsign.isNotEmpty()) {
                        val newUserUnit = org.json.JSONObject().apply {
                            put("callsign", activeCallsign)
                            put("organization", "SPMG")
                            put("status", if (arrivalTime.isNotEmpty()) getString(R.string.status_arrived_at_scene, arrivalTime) else getString(R.string.unit_accepted_format, acceptanceTime))
                            put("alarmTime", try { creationTime.split(" ")[1] } catch (_: Exception) { creationTime })
                            put("arrivalTime", arrivalTime)
                        }
                        unitsArray.put(newUserUnit)
                    }
                    obj.put("units", unitsArray)
                    break
                }
            }
            prefs.edit { putString("AssignmentsJson", array.toString()) }
            syncStateToDispatch()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadUnitsFromPersistence() {
        val prefs = getSharedPreferences("AssignmentPrefs", MODE_PRIVATE)
        val json = prefs.getString("AssignmentsJson", null) ?: return
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("id") == assignmentId) {
                    val unitsArray = obj.optJSONArray("units") ?: return
                    assignedUnitsList.clear()
                    for (j in 0 until unitsArray.length()) {
                        val uo = unitsArray.getJSONObject(j)
                        val uData = UnitData(
                            uo.getString("callsign"), uo.getString("organization"),
                            uo.optString("status", ""), uo.optString("alarmTime", ""), uo.optString("arrivalTime", "")
                        )
                        assignedUnitsList.add(uData)
                        if (uData.callsign == activeCallsign && uData.arrivalTime.isNotEmpty()) {
                            arrivalTime = uData.arrivalTime
                        }
                    }
                    val ongoingInput = obj.optString("ongoingInput", "")
                    if (ongoingInput.isNotEmpty()) {
                        etOngoingNotes.setText(ongoingInput)
                    }
                    break
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun refreshUnitsDisplay(container: LinearLayout) {
        container.removeAllViews()
        assignedUnitsList.forEach { addUnitToLayout(it, container) }
        
        if (isAccepted && activeCallsign.isNotEmpty()) {
            val alreadyInList = assignedUnitsList.any { it.callsign == activeCallsign }
            if (!alreadyInList) {
                val status = if (arrivalTime.isNotEmpty()) getString(R.string.status_arrived_at_scene, arrivalTime) else getString(R.string.unit_accepted_format, acceptanceTime)
                val userUnit = UnitData(activeCallsign, "SPMG", status, arrivalTime = arrivalTime)
                addUnitToLayout(userUnit, container)
            }
        }
    }

    private fun checkIdentitySync() {
        val fleetPrefs = getSharedPreferences("FleetPrefs", MODE_PRIVATE)
        val currentVehicle = fleetPrefs.getString("CurrentVehicle", null)
        val personalCallsign = getString(R.string.label_personal_identity, getDeviceUserIdentity())
        
        if (!isAccepted) return

        val time = getCurrentTimestamp("HH:mm")
        var logEntry: String? = null
        var newCallsign: String? = null

        if ((activeCallsign.startsWith("Mitarbeiter") || activeCallsign == "Persönliche Kennung") && currentVehicle != null) {
            logEntry = "Identitätswechsel um $time: Von persönliche Kennung auf Fahrzeug '$currentVehicle' gewechselt."
            newCallsign = currentVehicle
        } else if (!activeCallsign.startsWith("Mitarbeiter") && activeCallsign != "Persönliche Kennung" && currentVehicle == null) {
            logEntry = "Identitätswechsel um $time: Fahrzeug abgemeldet. Fallback auf Mitarbeiterkennung."
            newCallsign = personalCallsign
        }

        if (newCallsign != null) {
            activeCallsign = newCallsign
            if (logEntry != null) {
                assignmentNotes = if (assignmentNotes.isEmpty()) logEntry else "$assignmentNotes\n$logEntry"
            }
            refreshUnitsDisplay(findViewById(R.id.unitsContainer))
            saveCurrentState()
        }
    }

    private fun updateNotesFieldVisibility() {
        val vis = if (isAccepted) View.VISIBLE else View.GONE
        notesLayout.visibility = vis
        labelOngoingNotes.visibility = vis
        notesDivider.visibility = vis
        
        if (::btnArrived.isInitialized) {
            btnArrived.visibility = if (isAccepted && arrivalTime.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showCompletionDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.hint_completion_report)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setLines(3)
            LanguageUtils.configureGermanInput(this)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.btn_complete_assignment))
            .setMessage(getString(R.string.label_completion_report))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_complete_assignment)) { _, _ ->
                val ongoing = etOngoingNotes.text.toString().trim()
                if (ongoing.isNotEmpty()) {
                    val time = getCurrentTimestamp("HH:mm")
                    val entry = "Laufende Notiz ($time):\n$ongoing"
                    assignmentNotes = if (assignmentNotes.isEmpty()) entry else "$assignmentNotes\n\n$entry"
                }

                val userReport = LanguageUtils.sanitizeGermanText(input.text.toString())
                if (userReport.isNotEmpty()) {
                    val time = getCurrentTimestamp("HH:mm")
                    val entry = "Einsatzabschlussmeldung ($time):\n$userReport"
                    assignmentNotes = if (assignmentNotes.isEmpty()) entry else "$assignmentNotes\n\n$entry"
                }
                performCompletion()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null).show()
    }

    private fun performCompletion() {
        completionTime = getCurrentTimestamp()
        saveCurrentState("COMPLETED")
        
        val resultIntent = Intent().apply {
            putExtra("assignmentId", assignmentId)
            putExtra("newStatus", "COMPLETED")
            putExtra("completionTime", completionTime)
            putExtra("callsign", activeCallsign)
            putExtra("notes", assignmentNotes)
            putExtra("acceptanceTime", acceptanceTime)
        }
        setResult(RESULT_OK, resultIntent)

        val intent = Intent(this, AssignmentReportActivity::class.java).apply {
            putExtra("assignmentId", assignmentId)
            putExtra("isFromHistory", false)
        }
        startActivity(intent)
        finish()
    }

    private fun showIdentityPrompt(container: LinearLayout, button: Button) {
        AlertDialog.Builder(this)
            .setTitle(R.string.prompt_identity_selection)
            .setMessage(R.string.prompt_identity_message)
            .setPositiveButton(R.string.btn_identity_personal) { _, _ ->
                performAcceptance(getString(R.string.label_personal_identity, getDeviceUserIdentity()), container, button)
            }
            .setNegativeButton(R.string.btn_identity_vehicle) { _, _ ->
                fleetLauncher.launch(Intent(this, FleetActivity::class.java))
            }
            .setCancelable(true).show()
    }

    private fun performAcceptance(callsign: String, container: LinearLayout, button: Button) {
        isAccepted = true
        activeCallsign = callsign
        acceptanceTime = getCurrentTimestamp("HH:mm")
        button.text = getString(R.string.btn_complete_assignment)
        btnArrived.visibility = View.VISIBLE
        updateNotesFieldVisibility()
        refreshUnitsDisplay(container)
        saveCurrentState()
        Toast.makeText(this, R.string.btn_accept_assignment, Toast.LENGTH_SHORT).show()
    }

    private fun syncStateToDispatch() {
        val resultIntent = Intent().apply {
            putExtra("assignmentId", assignmentId)
            putExtra("newStatus", "ONGOING")
            putExtra("callsign", activeCallsign)
            putExtra("acceptanceTime", acceptanceTime)
            putExtra("notes", assignmentNotes)
        }
        setResult(RESULT_OK, resultIntent)
    }

    private fun addUnitToLayout(unit: UnitData, container: LinearLayout) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_unit, container, false)
        view.findViewById<TextView>(R.id.unitInfo).text = getString(R.string.unit_info_format, unit.callsign, unit.organization)
        view.findViewById<TextView>(R.id.unitStatus).text = unit.status.ifEmpty { "Bereit" }
        container.addView(view)
    }
}

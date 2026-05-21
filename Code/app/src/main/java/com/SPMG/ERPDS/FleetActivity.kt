package com.SPMG.ERPDS

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class FleetActivity : BaseActivity() {

    private val PREFS_NAME = "FleetPrefs"
    private val KEY_VEHICLE = "CurrentVehicle"
    private val KEY_ROLE = "CurrentRole"

    private var currentVehicle: String? = null
    private var currentRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fleet)

        // Load Persistence
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentVehicle = prefs.getString(KEY_VEHICLE, null)
        currentRole = prefs.getString(KEY_ROLE, null)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        updateUI()

        findViewById<Button>(R.id.btnSimulateScan).setOnClickListener {
            if (isTimeTrackingActive()) {
                showRoleSelection()
            } else {
                Toast.makeText(this, "Keine aktive Schicht! Bitte zuerst in Zeiterfassung einchecken.", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btnConfirmCheckin).setOnClickListener {
            if (!isTimeTrackingActive()) {
                Toast.makeText(this, "Schicht beendet! Check-in nicht möglich.", Toast.LENGTH_LONG).show()
                clearFleetState()
                updateUI()
                return@setOnClickListener
            }

            val roleGroup = findViewById<ChipGroup>(R.id.roleChipGroup)
            val checkedId = roleGroup.checkedChipId
            
            if (checkedId != View.NO_ID) {
                val chip = findViewById<Chip>(checkedId)
                saveFleetState(getString(R.string.vehicle_mock_name), chip.text.toString())
                Toast.makeText(this, "Fahrzeug Check-in erfolgreich!", Toast.LENGTH_SHORT).show()
                updateUI()
            } else {
                Toast.makeText(this, "Bitte eine Rolle auswählen", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnFleetLeave).setOnClickListener {
            clearFleetState()
            Toast.makeText(this, "Fahrzeug verlassen", Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()
        // Redundancy Check: Clear fleet if time tracking is not active
        if (!isTimeTrackingActive() && currentVehicle != null) {
            clearFleetState()
            updateUI()
            Toast.makeText(this, "Außerhalb der Schichtzeit: Fuhrparkzuweisung beendet.", Toast.LENGTH_LONG).show()
        }
    }

    private fun isTimeTrackingActive(): Boolean {
        val checkInPrefs = getSharedPreferences("CheckInPrefs", Context.MODE_PRIVATE)
        return checkInPrefs.getBoolean("IsCheckedIn", false)
    }

    private fun saveFleetState(vehicle: String, role: String) {
        currentVehicle = vehicle
        currentRole = role
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_VEHICLE, vehicle)
            putString(KEY_ROLE, role)
            apply()
        }
    }

    private fun clearFleetState() {
        currentVehicle = null
        currentRole = null
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun showRoleSelection() {
        findViewById<View>(R.id.scannerContainer).visibility = View.VISIBLE
        findViewById<View>(R.id.scanOverlay).visibility = View.GONE
        findViewById<View>(R.id.btnSimulateScan).visibility = View.GONE
        findViewById<View>(R.id.roleSelectionCard).visibility = View.VISIBLE
        findViewById<View>(R.id.fleetOverviewContainer).visibility = View.GONE
    }

    private fun updateUI() {
        val scannerContainer = findViewById<View>(R.id.scannerContainer)
        val scanOverlay = findViewById<View>(R.id.scanOverlay)
        val btnSimulate = findViewById<View>(R.id.btnSimulateScan)
        val roleCard = findViewById<View>(R.id.roleSelectionCard)
        val fleetOverview = findViewById<View>(R.id.fleetOverviewContainer)

        if (currentVehicle == null) {
            // Scanner View
            scannerContainer.visibility = View.VISIBLE
            scanOverlay.visibility = View.VISIBLE
            btnSimulate.visibility = View.VISIBLE
            roleCard.visibility = View.GONE
            fleetOverview.visibility = View.GONE
        } else {
            // Dashboard View
            scannerContainer.visibility = View.GONE
            roleCard.visibility = View.GONE
            fleetOverview.visibility = View.VISIBLE
            
            findViewById<TextView>(R.id.tvActiveVehicle).text = currentVehicle
            findViewById<TextView>(R.id.tvActiveRole).text = currentRole
            findViewById<TextView>(R.id.tvRadioStatus).text = getString(R.string.radio_status_format, currentVehicle)
        }
    }
}
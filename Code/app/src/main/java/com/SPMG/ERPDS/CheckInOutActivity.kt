package com.SPMG.ERPDS

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class CheckInOutActivity : BaseActivity() {
    private val PREFS_CHECKIN = "CheckInPrefs"
    private val KEY_IS_CHECKED_IN = "IsCheckedIn"
    private val KEY_START_TIME = "StartTime"
    
    private val PREFS_FLEET = "FleetPrefs"

    private var isCheckedIn = false
    private var startTimeMillis: Long = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimerUI()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in_out)

        // Load Persistence
        val prefs = getSharedPreferences(PREFS_CHECKIN, MODE_PRIVATE)
        isCheckedIn = prefs.getBoolean(KEY_IS_CHECKED_IN, false)
        startTimeMillis = prefs.getLong(KEY_START_TIME, 0)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val btnAction = findViewById<Button>(R.id.btnToggleAction)

        updateStatusUI()

        btnAction.setOnClickListener {
            if (!isCheckedIn) {
                performCheckIn()
            } else {
                performCheckOut(manual = true)
            }
            updateStatusUI()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isCheckedIn) {
            handler.post(timerRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerRunnable)
    }

    private fun performCheckIn() {
        isCheckedIn = true
        startTimeMillis = System.currentTimeMillis()
        
        getSharedPreferences(PREFS_CHECKIN, MODE_PRIVATE).edit().apply {
            putBoolean(KEY_IS_CHECKED_IN, true)
            putLong(KEY_START_TIME, startTimeMillis)
            apply()
        }
        handler.post(timerRunnable)
        Toast.makeText(this, "Eingestempelt - Schicht gestartet", Toast.LENGTH_SHORT).show()
    }

    private fun performCheckOut(manual: Boolean) {
        isCheckedIn = false
        startTimeMillis = 0
        
        // 1. Clear Check-In State
        getSharedPreferences(PREFS_CHECKIN, MODE_PRIVATE).edit().clear().apply()
        
        // 2. Enforcement: Clear Fleet Assignment when leaving shift
        getSharedPreferences(PREFS_FLEET, MODE_PRIVATE).edit().clear().apply()
        
        handler.removeCallbacks(timerRunnable)
        
        if (manual) {
            Toast.makeText(this, "Ausgestempelt - Fuhrparkzuweisung automatisch beendet", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Zeitlimit erreicht - Schicht & Fuhrpark automatisch beendet", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatusUI() {
        val statusText = findViewById<TextView>(R.id.statusText)
        val btnAction = findViewById<Button>(R.id.btnToggleAction)
        val timerText = findViewById<TextView>(R.id.timerText)

        if (isCheckedIn) {
            statusText.text = getString(R.string.status_checked_in)
            btnAction.text = getString(R.string.btn_check_out)
        } else {
            statusText.text = getString(R.string.status_checked_out)
            btnAction.text = getString(R.string.btn_check_in)
            timerText.text = "00:00:00"
        }
    }

    private fun updateTimerUI() {
        if (!isCheckedIn) return
        
        val elapsed = System.currentTimeMillis() - startTimeMillis
        val hours = elapsed / 3600000
        val minutes = (elapsed % 3600000) / 60000
        val seconds = (elapsed % 60000) / 1000
        
        findViewById<TextView>(R.id.timerText).text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        
        // Role-based Limit Check
        val fleetPrefs = getSharedPreferences(PREFS_FLEET, MODE_PRIVATE)
        val currentRole = fleetPrefs.getString("CurrentRole", "") ?: ""
        
        // Austrian Standards: 12h for emergency services, 8h default for prototype safety
        val isEmergencyService = currentRole.contains("Sicherheit") || 
                                currentRole.contains("Sanitäter") || 
                                currentRole.contains("Transportführer")
        
        val limitHours = if (isEmergencyService) 12 else 8
        
        if (hours >= limitHours) {
            performCheckOut(manual = false)
            updateStatusUI()
        }
    }
}
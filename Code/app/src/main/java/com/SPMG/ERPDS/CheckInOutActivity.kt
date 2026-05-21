package com.SPMG.ERPDS

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.*

class CheckInOutActivity : BaseActivity() {
    private val PREFS_CHECKIN = "CheckInPrefs"
    private val KEY_IS_CHECKED_IN = "IsCheckedIn"
    private val KEY_START_TIME = "StartTime"
    private val KEY_TOTAL_PAUSE_TIME = "TotalPauseTime"
    private val KEY_IS_PAUSED = "IsPaused"
    private val KEY_PAUSE_START_TIME = "PauseStartTime"
    private val KEY_HISTORY = "ShiftHistory"
    
    private val PREFS_FLEET = "FleetPrefs"

    private var isCheckedIn = false
    private var isPaused = false
    private var startTimeMillis: Long = 0
    private var totalPauseTime: Long = 0
    private var pauseStartTime: Long = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isPaused) {
                updatePauseTimerUI()
            } else {
                updateTimerUI()
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in_out)

        // Load Persistence
        val prefs = getSharedPreferences(PREFS_CHECKIN, MODE_PRIVATE)
        isCheckedIn = prefs.getBoolean(KEY_IS_CHECKED_IN, false)
        isPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
        startTimeMillis = prefs.getLong(KEY_START_TIME, 0)
        totalPauseTime = prefs.getLong(KEY_TOTAL_PAUSE_TIME, 0)
        pauseStartTime = prefs.getLong(KEY_PAUSE_START_TIME, 0)

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
        val btnPause = findViewById<Button>(R.id.btnPause)

        updateStatusUI()
        loadHistoryUI()

        btnAction.setOnClickListener {
            if (!isCheckedIn) {
                performCheckIn()
            } else {
                performCheckOut(manual = true)
            }
            updateStatusUI()
        }

        btnPause.setOnClickListener {
            togglePause()
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
        isPaused = false
        startTimeMillis = System.currentTimeMillis()
        totalPauseTime = 0
        pauseStartTime = 0
        
        getSharedPreferences(PREFS_CHECKIN, MODE_PRIVATE).edit().apply {
            putBoolean(KEY_IS_CHECKED_IN, true)
            putBoolean(KEY_IS_PAUSED, false)
            putLong(KEY_START_TIME, startTimeMillis)
            putLong(KEY_TOTAL_PAUSE_TIME, 0)
            putLong(KEY_PAUSE_START_TIME, 0)
            apply()
        }
        handler.post(timerRunnable)
        Toast.makeText(this, "Eingestempelt - Schicht gestartet", Toast.LENGTH_SHORT).show()
    }

    private fun performCheckOut(manual: Boolean) {
        if (isPaused) {
            totalPauseTime += (System.currentTimeMillis() - pauseStartTime)
        }
        val endTime = System.currentTimeMillis()
        saveToHistory(startTimeMillis, endTime)
        
        isCheckedIn = false
        isPaused = false
        startTimeMillis = 0
        totalPauseTime = 0
        pauseStartTime = 0
        
        // 1. Clear Check-In State
        getSharedPreferences(PREFS_CHECKIN, MODE_PRIVATE).edit().apply {
            putBoolean(KEY_IS_CHECKED_IN, false)
            putBoolean(KEY_IS_PAUSED, false)
            putLong(KEY_START_TIME, 0)
            putLong(KEY_TOTAL_PAUSE_TIME, 0)
            putLong(KEY_PAUSE_START_TIME, 0)
            apply()
        }
        
        // 2. Enforcement: Clear Fleet Assignment when leaving shift
        getSharedPreferences(PREFS_FLEET, MODE_PRIVATE).edit().clear().apply()
        
        handler.removeCallbacks(timerRunnable)
        
        if (manual) {
            Toast.makeText(this, "Ausgestempelt - Fuhrparkzuweisung automatisch beendet", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Zeitlimit erreicht - Schicht & Fuhrpark automatisch beendet", Toast.LENGTH_LONG).show()
        }
        loadHistoryUI()
    }

    private fun togglePause() {
        val prefs = getSharedPreferences(PREFS_CHECKIN, MODE_PRIVATE)
        if (!isPaused) {
            // Start Pause
            isPaused = true
            pauseStartTime = System.currentTimeMillis()
            prefs.edit().apply {
                putBoolean(KEY_IS_PAUSED, true)
                putLong(KEY_PAUSE_START_TIME, pauseStartTime)
                apply()
            }
            Toast.makeText(this, "Pause gestartet (30 Min)", Toast.LENGTH_SHORT).show()
        } else {
            // End Pause
            isPaused = false
            val pauseDuration = System.currentTimeMillis() - pauseStartTime
            totalPauseTime += pauseDuration
            prefs.edit().apply {
                putBoolean(KEY_IS_PAUSED, false)
                putLong(KEY_TOTAL_PAUSE_TIME, totalPauseTime)
                putLong(KEY_PAUSE_START_TIME, 0)
                apply()
            }
            findViewById<TextView>(R.id.pauseTimerText).visibility = View.GONE
            Toast.makeText(this, "Pause beendet", Toast.LENGTH_SHORT).show()
        }
        updateStatusUI()
    }

    private fun updateStatusUI() {
        val statusText = findViewById<TextView>(R.id.statusText)
        val btnAction = findViewById<Button>(R.id.btnToggleAction)
        val btnPause = findViewById<Button>(R.id.btnPause)
        val timerText = findViewById<TextView>(R.id.timerText)
        val pauseTimerText = findViewById<TextView>(R.id.pauseTimerText)
        val historyLabel = findViewById<TextView>(R.id.historyLabel)
        val historyCard = findViewById<View>(R.id.historyCard)

        if (isCheckedIn) {
            statusText.text = getString(R.string.status_checked_in)
            btnAction.text = getString(R.string.btn_check_out)
            btnPause.visibility = View.VISIBLE
            btnPause.text = if (isPaused) getString(R.string.btn_pause_stop) else getString(R.string.btn_pause_start)
            pauseTimerText.visibility = if (isPaused) View.VISIBLE else View.GONE
        } else {
            statusText.text = getString(R.string.status_checked_out)
            btnAction.text = getString(R.string.btn_check_in)
            btnPause.visibility = View.GONE
            pauseTimerText.visibility = View.GONE
            timerText.text = "00:00:00"
        }
        
        // History is always visible
        historyLabel.text = getString(R.string.label_shift_history)
        historyLabel.visibility = View.VISIBLE
        historyCard.visibility = View.VISIBLE
    }

    private fun updateTimerUI() {
        if (!isCheckedIn || isPaused) return
        
        val elapsed = (System.currentTimeMillis() - startTimeMillis) - totalPauseTime
        val hours = elapsed / 3600000
        val minutes = (elapsed % 3600000) / 60000
        val seconds = (elapsed % 60000) / 1000
        
        findViewById<TextView>(R.id.timerText).text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        
        // Role-based Limit Check
        val fleetPrefs = getSharedPreferences(PREFS_FLEET, MODE_PRIVATE)
        val currentRole = fleetPrefs.getString("CurrentRole", "") ?: ""
        
        val isEmergencyService = currentRole.contains("Sicherheit") || 
                                currentRole.contains("Sanitäter") || 
                                currentRole.contains("Transportführer")
        
        val limitHours = if (isEmergencyService) 12 else 8
        
        if (hours >= limitHours) {
            performCheckOut(manual = false)
            updateStatusUI()
        }
    }

    private fun updatePauseTimerUI() {
        if (!isPaused) return
        
        val pauseElapsed = System.currentTimeMillis() - pauseStartTime
        val thirtyMinutesMillis = 30 * 60 * 1000L
        val remaining = thirtyMinutesMillis - pauseElapsed
        
        val displayRemaining = if (remaining > 0) remaining else 0
        val minutes = (displayRemaining / 60000)
        val seconds = (displayRemaining % 60000) / 1000
        
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        findViewById<TextView>(R.id.pauseTimerText).text = getString(R.string.label_pause_timer, timeString)
        
        if (remaining <= 0) {
            findViewById<TextView>(R.id.pauseTimerText).setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            findViewById<TextView>(R.id.pauseTimerText).setTextColor(ContextCompat.getColor(this, R.color.brand_dark_blue))
        }
    }

    private fun saveToHistory(start: Long, end: Long) {
        val prefs = getSharedPreferences(PREFS_CHECKIN, MODE_PRIVATE)
        val history = prefs.getStringSet(KEY_HISTORY, LinkedHashSet<String>()) ?: LinkedHashSet<String>()
        val newHistory = LinkedHashSet<String>(history)
        
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        // Subtract pauses from duration
        val duration = (end - start) - totalPauseTime
        val h = duration / 3600000
        val m = (duration % 3600000) / 60000
        
        val entry = getString(R.string.shift_history_entry, sdf.format(Date(start)), sdf.format(Date(end)), h, m)
        newHistory.add(entry)
        
        // Keep only last 10 entries
        val list = newHistory.toList()
        val trimmed = if (list.size > 10) list.subList(list.size - 10, list.size) else list
        
        prefs.edit().putStringSet(KEY_HISTORY, trimmed.toSet()).apply()
    }

    private fun loadHistoryUI() {
        val container = findViewById<LinearLayout>(R.id.historyContainer)
        val emptyText = findViewById<TextView>(R.id.emptyHistoryText)
        container.removeAllViews()
        
        val prefs = getSharedPreferences(PREFS_CHECKIN, MODE_PRIVATE)
        val historySet = prefs.getStringSet(KEY_HISTORY, emptySet()) ?: emptySet()
        val historyList = historySet.toList().reversed() // Show newest first

        if (historyList.isEmpty()) {
            emptyText.text = getString(R.string.label_no_history)
            emptyText.visibility = View.VISIBLE
            container.addView(emptyText)
        } else {
            emptyText.visibility = View.GONE
            for (entry in historyList) {
                val tv = TextView(this).apply {
                    text = entry
                    setPadding(0, 8, 0, 8)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                }
                container.addView(tv)
                
                // Add divider
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(ContextCompat.getColor(this@CheckInOutActivity, android.R.color.darker_gray))
                }
                container.addView(divider)
            }
        }
    }
}
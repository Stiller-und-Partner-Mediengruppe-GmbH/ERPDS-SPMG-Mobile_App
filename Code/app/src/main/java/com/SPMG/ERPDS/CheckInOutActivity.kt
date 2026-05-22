package com.spmg.erpds

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CheckInOutActivity : BaseActivity() {

    companion object {
        private const val PREFS_NAME = "CheckInPrefs"
        private const val PREFS_FLEET = "FleetPrefs"
        private const val KEY_IS_CHECKED_IN = "IsCheckedIn"
        private const val KEY_IS_PAUSED = "IsPaused"
        private const val KEY_CHECK_IN_TIME = "CheckInTime"
        private const val KEY_PAUSE_START_TIME = "PauseStartTime"
        private const val KEY_PAUSE_END_TIME = "PauseEndTime"
        private const val KEY_TOTAL_PAUSE_DURATION = "TotalPauseDuration"
        private const val KEY_HISTORY = "ShiftHistory"
        private const val PAUSE_DURATION_MS = 30 * 60 * 1000L
    }

    private var isCheckedIn = false
    private var isPaused = false
    private var checkInTime: Long = 0
    private var pauseStartTime: Long = 0
    private var pauseEndTime: Long = 0
    private var totalPauseDuration: Long = 0
    private var lastShiftPauseStarts = mutableListOf<Long>()
    private var lastShiftPauseEnds = mutableListOf<Long>()

    private lateinit var statusText: TextView
    private lateinit var timerText: TextView
    private lateinit var pauseTimerText: TextView
    private lateinit var btnToggleAction: Button
    private lateinit var btnPause: Button
    private lateinit var historyContainer: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private val updateTask = object : Runnable {
        override fun run() {
            updateTimers()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_check_in_out)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isCheckedIn = prefs.getBoolean(KEY_IS_CHECKED_IN, false)
        isPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
        checkInTime = prefs.getLong(KEY_CHECK_IN_TIME, 0)
        pauseStartTime = prefs.getLong(KEY_PAUSE_START_TIME, 0)
        pauseEndTime = prefs.getLong(KEY_PAUSE_END_TIME, 0)
        totalPauseDuration = prefs.getLong(KEY_TOTAL_PAUSE_DURATION, 0)
        
        loadCurrentShiftPauses()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        statusText = findViewById(R.id.statusText)
        timerText = findViewById(R.id.timerText)
        pauseTimerText = findViewById(R.id.pauseTimerText)
        btnToggleAction = findViewById(R.id.btnToggleAction)
        btnPause = findViewById(R.id.btnPause)
        historyContainer = findViewById(R.id.historyContainer)

        btnToggleAction.setOnClickListener {
            if (isCheckedIn) performCheckOut() else performCheckIn()
        }
        btnPause.setOnClickListener { togglePause() }

        loadHistory()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        if (isCheckedIn) {
            handler.post(updateTask)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateTask)
    }

    private fun performCheckIn() {
        isCheckedIn = true
        checkInTime = System.currentTimeMillis()
        totalPauseDuration = 0
        isPaused = false
        lastShiftPauseStarts.clear()
        lastShiftPauseEnds.clear()
        
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putBoolean(KEY_IS_CHECKED_IN, true)
            putBoolean(KEY_IS_PAUSED, false)
            putLong(KEY_CHECK_IN_TIME, checkInTime)
            putLong(KEY_TOTAL_PAUSE_DURATION, 0)
            putLong(KEY_PAUSE_START_TIME, 0)
            putLong(KEY_PAUSE_END_TIME, 0)
            putString("CurrentShiftPauseStarts", "")
            putString("CurrentShiftPauseEnds", "")
        }.apply()

        Toast.makeText(this, "Eingecheckt um ${formatTime(checkInTime)}", Toast.LENGTH_SHORT).show()
        handler.post(updateTask)
        updateUI()
    }

    private fun performCheckOut() {
        val now = System.currentTimeMillis()
        if (isPaused) {
            endPause(now)
        }

        val checkOutTime = now
        saveToHistory(checkInTime, checkOutTime, totalPauseDuration)

        isCheckedIn = false
        checkInTime = 0
        totalPauseDuration = 0
        lastShiftPauseStarts.clear()
        lastShiftPauseEnds.clear()
        
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putBoolean(KEY_IS_CHECKED_IN, false)
            putBoolean(KEY_IS_PAUSED, false)
            putLong(KEY_CHECK_IN_TIME, 0)
            putLong(KEY_TOTAL_PAUSE_DURATION, 0)
            putLong(KEY_PAUSE_START_TIME, 0)
            putLong(KEY_PAUSE_END_TIME, 0)
            putString("CurrentShiftPauseStarts", "")
            putString("CurrentShiftPauseEnds", "")
        }.apply()

        getSharedPreferences(PREFS_FLEET, MODE_PRIVATE).edit().clear().apply()

        handler.removeCallbacks(updateTask)
        updateUI()
        
        Toast.makeText(this, "Ausgecheckt. Schicht beendet.", Toast.LENGTH_LONG).show()
    }

    private fun togglePause() {
        if (!isPaused) {
            startPause()
        } else {
            endPause(System.currentTimeMillis())
        }
    }

    private fun startPause() {
        isPaused = true
        val now = System.currentTimeMillis()
        pauseStartTime = now
        pauseEndTime = now + PAUSE_DURATION_MS
        lastShiftPauseStarts.add(now)
        
        saveCurrentShiftPauses()
        
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putBoolean(KEY_IS_PAUSED, true)
            putLong(KEY_PAUSE_START_TIME, pauseStartTime)
            putLong(KEY_PAUSE_END_TIME, pauseEndTime)
        }.apply()
        
        Toast.makeText(this, "Pause gestartet (30 Min)", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun endPause(endTime: Long) {
        if (!isPaused) return
        
        isPaused = false
        val duration = endTime - pauseStartTime
        totalPauseDuration += duration
        lastShiftPauseEnds.add(endTime)
        
        saveCurrentShiftPauses()
        
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putBoolean(KEY_IS_PAUSED, false)
            putLong(KEY_PAUSE_START_TIME, 0)
            putLong(KEY_PAUSE_END_TIME, 0)
            putLong(KEY_TOTAL_PAUSE_DURATION, totalPauseDuration)
        }.apply()
        
        pauseTimerText.visibility = View.GONE
        Toast.makeText(this, "Pause beendet", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun updateUI() {
        btnToggleAction.text = if (isCheckedIn) getString(R.string.btn_check_out) else getString(R.string.btn_check_in)
        btnPause.visibility = if (isCheckedIn) View.VISIBLE else View.GONE
        pauseTimerText.visibility = if (isCheckedIn && isPaused) View.VISIBLE else View.GONE

        if (isCheckedIn) {
            if (isPaused) {
                statusText.text = "Außer Dienst (Pause 30 Min)"
                statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
            } else {
                statusText.text = getString(R.string.status_checked_in)
                statusText.setTextColor(getColor(android.R.color.holo_green_dark))
            }
            btnPause.text = if (isPaused) getString(R.string.btn_pause_stop) else getString(R.string.btn_pause_start)
        } else {
            statusText.text = getString(R.string.status_checked_out)
            statusText.setTextColor(getColor(android.R.color.darker_gray))
            timerText.text = "00:00:00"
        }
    }

    private fun updateTimers() {
        if (!isCheckedIn) return
        
        val now = System.currentTimeMillis()
        
        if (isPaused) {
            if (now >= pauseEndTime) {
                endPause(pauseEndTime)
                return
            }

            timerText.text = formatDuration(pauseStartTime - checkInTime - totalPauseDuration)
            
            val remainingPause = pauseEndTime - now
            pauseTimerText.text = "Pause endet in: ${formatDuration(remainingPause)}"
        } else {
            val totalDuration = now - checkInTime - totalPauseDuration
            timerText.text = formatDuration(totalDuration)
        }
    }

    private fun saveToHistory(start: Long, end: Long, pause: Long) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val historySet = prefs.getStringSet(KEY_HISTORY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date(start))
        
        val pauseDetails = StringBuilder()
        for (i in lastShiftPauseStarts.indices) {
            val pStart = formatTime(lastShiftPauseStarts[i])
            val pEnd = if (i < lastShiftPauseEnds.size) formatTime(lastShiftPauseEnds[i]) else "N/A"
            pauseDetails.append("$pStart-$pEnd,")
        }
        val pauseStr = if (pauseDetails.isNotEmpty()) pauseDetails.substring(0, pauseDetails.length - 1) else "Keine"
        
        val netDuration = (end - start) - pause
        val hours = (netDuration / (1000 * 60 * 60)).toInt()
        val mins = ((netDuration / (1000 * 60)) % 60).toInt()
        
        val entry = "$dateStr|${formatTime(start)}|${formatTime(end)}|$hours|$mins|$pauseStr"
        historySet.add(entry)
        prefs.edit().putStringSet(KEY_HISTORY, historySet).apply()
        loadHistory()
    }

    private fun loadHistory() {
        historyContainer.removeAllViews()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val history = prefs.getStringSet(KEY_HISTORY, emptySet())?.toList()?.sortedDescending() ?: emptyList()

        if (history.isEmpty()) {
            val tv = TextView(this).apply { text = getString(R.string.label_no_history); setPadding(0, 16, 0, 0) }
            historyContainer.addView(tv)
            return
        }

        val inflater = LayoutInflater.from(this)
        history.take(5).forEach { entry ->
            val parts = entry.split("|")
            if (parts.size >= 5) {
                val view = inflater.inflate(R.layout.item_shift_history, historyContainer, false)
                view.findViewById<TextView>(R.id.tvShiftDate).text = parts[0]
                
                val baseDetails = getString(R.string.shift_history_entry, parts[1], parts[2], parts[3].toInt(), parts[4].toInt())
                val pauseInfo = if (parts.size > 5) "\nPausen: ${parts[5]}" else ""
                
                view.findViewById<TextView>(R.id.tvShiftDetails).text = "$baseDetails$pauseInfo"
                historyContainer.addView(view)
            }
        }
    }

    private fun saveCurrentShiftPauses() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putString("CurrentShiftPauseStarts", lastShiftPauseStarts.joinToString(","))
            putString("CurrentShiftPauseEnds", lastShiftPauseEnds.joinToString(","))
        }.apply()
    }

    private fun loadCurrentShiftPauses() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val starts = prefs.getString("CurrentShiftPauseStarts", "") ?: ""
        val ends = prefs.getString("CurrentShiftPauseEnds", "") ?: ""
        
        lastShiftPauseStarts.clear()
        if (starts.isNotEmpty()) {
            starts.split(",").forEach { 
                try { lastShiftPauseStarts.add(it.toLong()) } catch (_: Exception) {}
            }
        }
        
        lastShiftPauseEnds.clear()
        if (ends.isNotEmpty()) {
            ends.split(",").forEach { 
                try { lastShiftPauseEnds.add(it.toLong()) } catch (_: Exception) {}
            }
        }
    }

    private fun formatTime(time: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Europe/Vienna")
        return sdf.format(Date(time))
    }

    private fun formatDuration(durationMs: Long): String {
        val sec = (durationMs / 1000) % 60
        val min = (durationMs / (1000 * 60)) % 60
        val hrs = (durationMs / (1000 * 60 * 60))
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, min, sec)
    }
}

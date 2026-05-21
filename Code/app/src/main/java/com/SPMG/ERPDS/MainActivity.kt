package com.SPMG.ERPDS

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set personalized greeting
        val userName = "Max Mustermann"
        val userNameTextView = findViewById<TextView>(R.id.userNameTextView)
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar[java.util.Calendar.HOUR_OF_DAY]
        val greetingResId = if (hour in 0..10) R.string.greeting_morning else R.string.greeting_day
        userNameTextView.text = getString(greetingResId, userName)

        // Fuhrpark (Konsolidierte Funktion)
        findViewById<Button>(R.id.btnQRCheckIn).setOnClickListener {
            startActivity(Intent(this, FleetActivity::class.java))
        }

        // Dispatch / Einsätze
        findViewById<Button>(R.id.btnDispatch).setOnClickListener {
            startActivity(Intent(this, DispatchActivity::class.java))
        }

        // Zeiterfassung & Sicherheit
        findViewById<Button>(R.id.btnCheckInOut).setOnClickListener {
            startActivity(Intent(this, CheckInOutActivity::class.java))
        }
        findViewById<Button>(R.id.btnPrivacy2FA).setOnClickListener {
            startActivity(Intent(this, PrivacyAuthActivity::class.java))
        }
        findViewById<Button>(R.id.btnWatchEmergency).setOnClickListener {
            launchFeature("Smartwatch Notruf")
        }
        findViewById<Button>(R.id.btnEvacList).setOnClickListener {
            launchFeature("Dynamische Evakuierungsliste")
        }

        // Gastro
        findViewById<Button>(R.id.btnWaiterPad).setOnClickListener {
            launchFeature("Waiter-Pad")
        }
        findViewById<Button>(R.id.btnSmartWallet).setOnClickListener {
            launchFeature("Smart Wallet")
        }
        findViewById<Button>(R.id.btnTicketScanner).setOnClickListener {
            launchFeature("Ticket- & Chip-Scanner")
        }
    }

    private fun launchFeature(title: String) {
        val intent = Intent(this, FeatureActivity::class.java).apply {
            putExtra("feature_title", title)
        }
        startActivity(intent)
    }
}
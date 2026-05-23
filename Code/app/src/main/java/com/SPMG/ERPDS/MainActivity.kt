package com.spmg.erpds

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : BaseActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if ((permissions[Manifest.permission.READ_CONTACTS] == true) || 
            (permissions[Manifest.permission.GET_ACCOUNTS] == true)) {
            // Berechtigungen erhalten, Namen neu laden
            updateGreeting()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkPermissionsAndLoadIdentity()

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
            launchFeature(getString(R.string.btn_watch_emergency))
        }

        // Sicherheit & Schutz
        findViewById<Button>(R.id.btnIdCheck).setOnClickListener {
            startActivity(Intent(this, IDCheckActivity::class.java))
        }
        findViewById<Button>(R.id.btnEvacList).setOnClickListener {
            launchFeature(getString(R.string.btn_evac_list))
        }

        // Gastro
        findViewById<Button>(R.id.btnWaiterPad).setOnClickListener {
            launchFeature(getString(R.string.btn_waiter_pad))
        }
        findViewById<Button>(R.id.btnSmartWallet).setOnClickListener {
            launchFeature(getString(R.string.btn_smart_wallet))
        }
        findViewById<Button>(R.id.btnTicketScanner).setOnClickListener {
            launchFeature(getString(R.string.btn_scanner))
        }

        findViewById<Button>(R.id.btnViewProfile).setOnClickListener {
            launchFeature(getString(R.string.section_profile))
        }
    }

    override fun onResume() {
        super.onResume()
        // Aktualisiert die Begrüßung (Zeit/Name) beim Zurückkehren
        updateGreeting()
        updateProfileSection()
    }

    private fun checkPermissionsAndLoadIdentity() {
        val hasContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val hasAccounts = ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED

        if (!hasContacts || !hasAccounts) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.GET_ACCOUNTS,
                ),
            )
        }
        
        // Initialer Versuch (evtl. nur Bluetooth/Modell Fallback)
        updateGreeting()
        updateProfileSection()
    }

    private fun updateGreeting() {
        val userName = getDeviceUserIdentity()
        val userNameTextView = findViewById<TextView>(R.id.userNameTextView)
        val greetingResId = when (java.util.Calendar.getInstance()[java.util.Calendar.HOUR_OF_DAY]) {
            in 5..10 -> R.string.greeting_morning
            in 18..22 -> R.string.greeting_evening
            in 23..24, in 0..4 -> R.string.greeting_night
            else -> R.string.greeting_day
        }

        userNameTextView.text = getString(greetingResId, userName)
    }

    private fun updateProfileSection() {
        val userName = getDeviceUserIdentity()
        findViewById<TextView>(R.id.profileNameDisplay).text = userName

        getDeviceUserProfilePicture()?.let { uri ->
            findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.profileImage).setImageURI(uri)
        }
    }

    private fun launchFeature(title: String) {
        val intent = Intent(this, FeatureActivity::class.java).apply {
            putExtra("feature_title", title)
        }
        startActivity(intent)
    }
}

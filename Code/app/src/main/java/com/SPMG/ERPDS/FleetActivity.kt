package com.spmg.erpds

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FleetActivity : BaseActivity() {

    companion object {
        private const val PREFS_NAME = "FleetPrefs"
        private const val KEY_VEHICLE = "CurrentVehicle"
        private const val KEY_ROLE = "CurrentRole"
    }

    private var currentVehicle: String? = null
    private var currentRole: String? = null
    private var scannedVehicleName: String? = null

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fleet)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Load Persistence
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        currentVehicle = prefs.getString(KEY_VEHICLE, null)
        currentRole = prefs.getString(KEY_ROLE, null)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
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
                scannedVehicleName = getString(R.string.vehicle_mock_name)
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
                saveFleetState(scannedVehicleName ?: getString(R.string.vehicle_mock_name), chip.text.toString())
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

        if (currentVehicle == null) {
            checkCameraPermission()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == 10 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            startCamera()
        } else {
            Toast.makeText(this, "Kamera-Berechtigung wird für den QR-Scan benötigt.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        val previewView = findViewById<PreviewView>(R.id.viewFinder)

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val barcodeScanner = BarcodeScanning.getClient()
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(barcodeScanner, imageProxy)
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
        } catch (exc: Exception) {
            Log.e("FleetActivity", "Use case binding failed", exc)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        handleBarcode(barcodes[0])
                    }
                }
                .addOnFailureListener {
                    Log.e("FleetActivity", "Barcode scanning failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleBarcode(barcode: Barcode) {
        val value = barcode.displayValue ?: return
        runOnUiThread {
            if (currentVehicle == null && scannedVehicleName == null) {
                if (isTimeTrackingActive()) {
                    scannedVehicleName = value
                    Toast.makeText(this, "QR-Code erkannt: $value", Toast.LENGTH_SHORT).show()
                    showRoleSelection()
                } else {
                    Toast.makeText(this, "Bitte zuerst einchecken!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isTimeTrackingActive() && currentVehicle != null) {
            clearFleetState()
            updateUI()
            Toast.makeText(this, "Außerhalb der Schichtzeit: Fuhrparkzuweisung beendet.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
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
        scannedVehicleName = null
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        // Restart camera if we just cleared the state
        checkCameraPermission()
    }

    private fun showRoleSelection() {
        // Stop camera analysis/preview when showing role selection to save resources
        cameraProvider?.unbindAll()

        findViewById<View>(R.id.scannerContainer).visibility = View.VISIBLE
        findViewById<View>(R.id.scanOverlay).visibility = View.GONE
        findViewById<View>(R.id.btnSimulateScan).visibility = View.GONE
        findViewById<View>(R.id.roleSelectionCard).visibility = View.VISIBLE
        findViewById<View>(R.id.fleetOverviewContainer).visibility = View.GONE
        
        // Update vehicle name in the card
        findViewById<TextView>(R.id.vehicle_found_name_text)?.text = scannedVehicleName ?: getString(R.string.vehicle_mock_name)
    }

    private fun updateUI() {
        val scannerContainer = findViewById<View>(R.id.scannerContainer)
        val scanOverlay = findViewById<View>(R.id.scanOverlay)
        val btnSimulate = findViewById<View>(R.id.btnSimulateScan)
        val roleCard = findViewById<View>(R.id.roleSelectionCard)
        val fleetOverview = findViewById<View>(R.id.fleetOverviewContainer)

        if (currentVehicle == null) {
            scannerContainer.visibility = View.VISIBLE
            scanOverlay.visibility = View.VISIBLE
            btnSimulate.visibility = View.VISIBLE
            roleCard.visibility = View.GONE
            fleetOverview.visibility = View.GONE
        } else {
            cameraProvider?.unbindAll()
            scannerContainer.visibility = View.GONE
            roleCard.visibility = View.GONE
            fleetOverview.visibility = View.VISIBLE
            
            findViewById<TextView>(R.id.tvActiveVehicle).text = currentVehicle
            findViewById<TextView>(R.id.tvActiveRole).text = currentRole
            findViewById<TextView>(R.id.tvRadioStatus).text = getString(R.string.radio_status_format, currentVehicle)
            
            updateCrewList()
        }
    }

    private fun updateCrewList() {
        val container = findViewById<LinearLayout>(R.id.crewContainer)
        container.removeAllViews()

        // Mock crew data based on vehicle type (e.g. RTW usually has 2-3 people)
        val mockCrew = mutableListOf<Pair<String, String>>()
        
        if (currentRole != getString(R.string.role_driver)) {
            mockCrew.add("Maximilian Muster" to getString(R.string.role_driver))
        }
        if (currentRole != getString(R.string.role_leader)) {
            mockCrew.add("Julia Beispiel" to getString(R.string.role_leader))
        }
        if (currentRole != getString(R.string.role_assistant)) {
            mockCrew.add("Lukas Test" to getString(R.string.role_assistant))
        }

        mockCrew.forEach { (name, role) ->
            val itemView = layoutInflater.inflate(R.layout.item_fleet_crew, container, false)
            itemView.findViewById<TextView>(R.id.tvMemberName).text = name
            itemView.findViewById<TextView>(R.id.tvMemberRole).text = role
            itemView.findViewById<Button>(R.id.btnNotifyMember).setOnClickListener {
                showSendNotificationDialog(name)
            }
            container.addView(itemView)
        }
    }

    private fun showSendNotificationDialog(name: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }
        
        val input = EditText(this).apply {
            hint = getString(R.string.hint_message_text)
            minLines = 2
        }
        container.addView(input)
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.prompt_send_message, name))
            .setView(container)
            .setPositiveButton(getString(R.string.btn_send)) { _, _ ->
                val message = input.text.toString()
                if (message.isNotEmpty()) {
                    Toast.makeText(this, "Nachricht an $name gesendet: $message", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}

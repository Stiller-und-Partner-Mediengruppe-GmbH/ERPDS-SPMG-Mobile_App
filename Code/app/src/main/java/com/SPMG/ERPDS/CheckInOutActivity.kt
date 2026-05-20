package com.SPMG.ERPDS

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CheckInOutActivity : AppCompatActivity() {
    private var isCheckedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_check_in_out)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val statusText = findViewById<TextView>(R.id.statusText)
        val btnAction = findViewById<Button>(R.id.btnToggleAction)

        btnAction.setOnClickListener {
            isCheckedIn = !isCheckedIn
            if (isCheckedIn) {
                statusText.text = getString(R.string.status_checked_in)
                btnAction.text = getString(R.string.btn_check_out)
            } else {
                statusText.text = getString(R.string.status_checked_out)
                btnAction.text = getString(R.string.btn_check_in)
            }
        }
    }
}
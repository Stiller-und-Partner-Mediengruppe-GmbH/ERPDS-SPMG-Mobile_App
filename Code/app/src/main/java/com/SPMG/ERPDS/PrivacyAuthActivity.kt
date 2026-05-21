package com.SPMG.ERPDS

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class PrivacyAuthActivity : BaseActivity() {
    private var isRevealed = false
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideCode() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_auth)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val codeCard = findViewById<MaterialCardView>(R.id.codeCard)
        codeCard.setOnClickListener {
            if (!isRevealed) {
                revealCode()
            } else {
                hideCode()
            }
        }
    }

    private fun revealCode() {
        isRevealed = true
        val authCode = findViewById<TextView>(R.id.authCode)
        val tapHint = findViewById<TextView>(R.id.tapHint)
        
        // Simulating a real 2FA code
        authCode.text = "482 917" 
        tapHint.alpha = 0f
        
        // Auto-hide after 10 seconds
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 10000)
    }

    private fun hideCode() {
        isRevealed = false
        val authCode = findViewById<TextView>(R.id.authCode)
        val tapHint = findViewById<TextView>(R.id.tapHint)
        
        authCode.text = getString(R.string.mask_code)
        tapHint.alpha = 1f
        hideHandler.removeCallbacks(hideRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideHandler.removeCallbacks(hideRunnable)
    }
}
package com.spmg.erpds

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class IDCheckActivity : BaseActivity() {

    private lateinit var resultCard: MaterialCardView
    private lateinit var authStatusText: TextView
    private lateinit var cardHolderName: TextView
    private lateinit var cardType: TextView
    private lateinit var cardId: TextView
    private lateinit var nfcIcon: ImageView
    private lateinit var statusTitle: TextView
    private lateinit var statusSubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_id_check)

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        resultCard = findViewById(R.id.resultCard)
        authStatusText = findViewById(R.id.authStatusText)
        cardHolderName = findViewById(R.id.cardHolderName)
        cardType = findViewById(R.id.cardType)
        cardId = findViewById(R.id.cardId)
        nfcIcon = findViewById(R.id.nfcIcon)
        statusTitle = findViewById(R.id.statusTitle)
        statusSubtitle = findViewById(R.id.statusSubtitle)

        findViewById<Button>(R.id.btnSimulateScan).setOnClickListener {
            simulateScan()
        }
    }

    private fun simulateScan() {
        // Mock-Daten für Demo-Zwecke
        val isAuthorized = (0..1).random() == 1
        val names = listOf("Max Mustermann", "Sabine Schmidt", "Thomas Müller", "Julia Wagner")
        val types = listOf("Dienstausweis (SPMG)", "Besucherausweis", "Fremdfirmen-ID")
        
        statusTitle.text = getString(R.string.status_card_read)
        statusSubtitle.text = getString(R.string.status_processing)
        
        resultCard.visibility = View.VISIBLE
        
        if (isAuthorized) {
            authStatusText.text = getString(R.string.status_access_granted)
            authStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            cardHolderName.text = names.random()
            nfcIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            authStatusText.text = getString(R.string.status_access_denied)
            authStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            cardHolderName.text = getString(R.string.unknown_holder)
            nfcIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
        
        cardType.text = getString(R.string.card_type_format, types.random())
        cardId.text = getString(R.string.card_uid_format, generateRandomUID())
    }

    private fun generateRandomUID(): String {
        val hexChars = "0123456789ABCDEF"
        return (1..7).joinToString(":") { 
            "${hexChars.random()}${hexChars.random()}"
        }
    }
}

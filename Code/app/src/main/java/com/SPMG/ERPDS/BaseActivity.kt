package com.SPMG.ERPDS

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        // Erzwingt das deutsche Locale (Schriftsatz) systemweit.
        // Das sorgt dafür, dass EditTexts und Tastaturen Umlaute korrekt verarbeiten.
        super.attachBaseContext(LanguageUtils.wrapContext(newBase))
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        // Stellt sicher, dass Locale-Änderungen auch bei Konfigurationswechseln (z.B. Rotation)
        // stabil bleiben und die Umlaute-Darstellung nicht bricht.
        if (overrideConfiguration != null) {
            val uiMode = overrideConfiguration.uiMode
            overrideConfiguration.setTo(baseContext.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }
}
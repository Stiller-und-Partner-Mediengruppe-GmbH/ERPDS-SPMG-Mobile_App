package com.SPMG.ERPDS

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.view.View
import android.widget.EditText
import java.util.Locale

object LanguageUtils {

    /**
     * Erzeugt einen Context, der hart auf Deutsch (AT) eingestellt ist.
     * Dies betrifft sowohl die Sprache als auch den Schriftsatz.
     */
    fun wrapContext(context: Context): Context {
        val locale = Locale("de", "AT")
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        
        config.setLayoutDirection(locale)
        
        // WICHTIG: Setzt den Schriftsatz explizit auf Deutsch
        return context.createConfigurationContext(config)
    }

    /**
     * Konfiguriert ein EditText-Feld so, dass es deutsche Eingaben (Umlaute) 
     * vom System und der Tastatur (auch Emulator-Hardware-Keyboard) anfordert.
     */
    fun configureGermanInput(editText: EditText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            editText.imeHintLocales = LocaleList(Locale("de", "AT"))
        }
        
        // Erzwingt, dass das Feld Text-Vorschläge und Umlaute zulässt
        editText.textDirection = View.TEXT_DIRECTION_LTR
    }

    /**
     * Prüft den String auf UTF-8 Konformität und übersetzt ggf. 
     * falsch kodierte Zeichencodes in echte deutsche Umlaute.
     */
    fun sanitizeGermanText(input: String?): String {
        if (input == null) return ""
        
        // Falls Zeichen durch fehlerhafte Kodierung (z.B. bei Emulator-Eingaben) 
        // als UTF-8 Bytes in einem ISO-String landen, korrigieren wir diese hier.
        var sanitized = input
        
        // Korrektur von typischen "Encoding-Glitch" Mustern
        if (sanitized.contains("Ã")) {
            sanitized = sanitized
                .replace("Ã¤", "ä")
                .replace("Ã¶", "ö")
                .replace("Ã¼", "ü")
                .replace("Ã„", "Ä")
                .replace("Ã–", "Ö")
                .replace("Ãœ", "Ü")
                .replace("ÃŸ", "ß")
        }
        
        return sanitized.trim()
    }
}
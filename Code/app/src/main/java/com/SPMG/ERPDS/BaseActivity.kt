package com.SPMG.ERPDS

import android.Manifest
import android.accounts.AccountManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

abstract class BaseActivity : AppCompatActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        // Erzwingt das deutsche Locale (Schriftsatz) systemweit.
        super.attachBaseContext(LanguageUtils.wrapContext(newBase))
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val uiMode = overrideConfiguration.uiMode
            overrideConfiguration.setTo(baseContext.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    /**
     * Lädt die Benutzerkennung/Identität aus dem Hauptkontakt (Profil) des Geräts.
     * Nutzt eine mehrstufige Suche für maximale Zuverlässigkeit.
     */
    protected fun getDeviceUserIdentity(): String {
        var identity: String? = null
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                // 1. Stufe: Den primären Anzeigenamen aus dem Profil-Root lesen (am zuverlässigsten)
                contentResolver.query(
                    ContactsContract.Profile.CONTENT_URI,
                    arrayOf(ContactsContract.Profile.DISPLAY_NAME, ContactsContract.Profile.DISPLAY_NAME_PRIMARY),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        identity = cursor.getString(0) ?: cursor.getString(1)
                    }
                }

                // 2. Stufe: Falls Anzeigename leer, explizit strukturierte Felder (Vor/Nachname) suchen
                if (identity.isNullOrEmpty()) {
                    val profileDataUri = Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.Contacts.Data.CONTENT_DIRECTORY)
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME
                    )
                    val where = "${ContactsContract.Data.MIMETYPE} = ?"
                    val whereArgs = arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)

                    contentResolver.query(profileDataUri, projection, where, whereArgs, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val firstName = cursor.getString(0) ?: ""
                            val lastName = cursor.getString(1) ?: ""
                            if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                identity = "$firstName $lastName".trim()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Fehler beim Kontaktzugriff (z.B. SecurityException trotz Permission)
            }
        }

        // 3. Stufe: Fallback auf Google-Konto (Formatierung von Vorname.Nachname)
        if (identity.isNullOrEmpty() && ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val accounts = AccountManager.get(this).getAccountsByType("com.google")
                if (accounts.isNotEmpty()) {
                    val email = accounts[0].name
                    val rawName = email.substringBefore("@")
                    identity = if (rawName.contains(".")) {
                        rawName.split(".").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    } else {
                        rawName.replaceFirstChar { it.uppercase() }
                    }
                }
            } catch (e: Exception) {}
        }

        // 4. Stufe: Personalisierter Bluetooth-Name (User-Set)
        if (identity.isNullOrEmpty()) {
            val btName = android.provider.Settings.Secure.getString(contentResolver, "bluetooth_name")
            if (!btName.isNullOrEmpty() && btName != android.os.Build.MODEL) {
                identity = btName
            }
        }

        // 5. Stufe: Letzter Ausweg - Gerätemodell oder Standard
        return identity ?: android.os.Build.MODEL ?: "Mitarbeiter"
    }
}
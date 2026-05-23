package com.spmg.erpds

import android.Manifest
import android.accounts.AccountManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

abstract class BaseActivity : AppCompatActivity() {
    
    override fun attachBaseContext(newBase: Context) {
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

    protected fun getDeviceUserIdentity(): String {
        var identity: String? = null
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                contentResolver.query(
                    ContactsContract.Profile.CONTENT_URI,
                    arrayOf(ContactsContract.Profile.DISPLAY_NAME, ContactsContract.Profile.DISPLAY_NAME_PRIMARY),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        identity = cursor.getString(0) ?: cursor.getString(1)
                    }
                }

                if (identity.isNullOrEmpty()) {
                    val profileDataUri = Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.Contacts.Data.CONTENT_DIRECTORY)
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
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
            } catch (_: Exception) {}
        }

        if (identity.isNullOrEmpty() && (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED)) {
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
            } catch (_: Exception) {}
        }

        if (identity.isNullOrEmpty()) {
            val btName = android.provider.Settings.Secure.getString(contentResolver, "bluetooth_name")
            if ((!btName.isNullOrEmpty()) && (btName != android.os.Build.MODEL)) {
                identity = btName
            }
        }

        return identity ?: android.os.Build.MODEL ?: "Mitarbeiter"
    }

    protected fun getDeviceUserProfilePicture(): Uri? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                contentResolver.query(
                    ContactsContract.Profile.CONTENT_URI,
                    arrayOf(ContactsContract.Profile.PHOTO_URI),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val uriString = cursor.getString(0)
                        if (!uriString.isNullOrEmpty()) {
                            return uriString.toUri()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }
}

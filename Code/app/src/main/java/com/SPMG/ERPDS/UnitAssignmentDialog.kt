package com.spmg.erpds

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class UnitAssignmentDialog(
    private val context: Context,
    private val onUnitsConfirmed: (List<UnitData>) -> Unit,
) {
    private val orgs = listOf(
        "MA-70(BR-W)", 
        "MA-68(BF-W)", 
        "EXE(LPD)", 
        "WW(MA-31)", 
        "WE(Wien Energie)", 
        "WN(Wiener Netze)",
    )

    fun show() {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val tvTitle = TextView(context).apply {
            text = context.getString(R.string.title_add_units)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            setPadding(0, 0, 0, 16)
        }
        container.addView(tvTitle)

        val unitsListLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(unitsListLayout)

        val btnAddRow = Button(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = context.getString(R.string.btn_add_unit_row)
            setOnClickListener {
                addUnitRow(unitsListLayout)
            }
        }
        container.addView(btnAddRow)

        // Add first row by default
        addUnitRow(unitsListLayout)

        AlertDialog.Builder(context)
            .setView(container)
            .setPositiveButton("Übernehmen") { _, _ ->
                val resultList = mutableListOf<UnitData>()
                for (i in 0 until unitsListLayout.childCount) {
                    val row = unitsListLayout.getChildAt(i) as LinearLayout
                    val spinner = row.getChildAt(0) as Spinner
                    val editText = row.getChildAt(1) as EditText
                    
                    val callsign = editText.text.toString().trim()
                    if (callsign.isNotEmpty()) {
                        val org = spinner.selectedItem.toString()
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        sdf.timeZone = TimeZone.getTimeZone("Europe/Vienna")
                        val now = sdf.format(Date())
                        
                        resultList.add(UnitData(callsign, org, "Alarmiert", now, ""))
                    }
                }
                onUnitsConfirmed(resultList)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun addUnitRow(parent: LinearLayout) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }

        val spinner = Spinner(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, orgs)
        }

        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            hint = "Funkrufname"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            LanguageUtils.configureGermanInput(this)
        }

        row.addView(spinner)
        row.addView(editText)
        parent.addView(row)
    }
}

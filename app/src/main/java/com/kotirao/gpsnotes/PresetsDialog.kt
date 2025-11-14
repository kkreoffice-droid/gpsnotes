package com.kotirao.gpsnotes

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter

class PresetsDialog(private val context: Context) {
    fun show() {
        val list = PresetsManager.getPresets(context)
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Presets")
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_presets, null)
        val edit = view.findViewById<android.widget.EditText>(R.id.editPreset)
        builder.setView(view)
        builder.setPositiveButton("Add") { _, _ ->
            val text = edit.text.toString().trim()
            if (text.isNotEmpty()) PresetsManager.addPreset(context, text)
        }
        builder.setNeutralButton("Manage") { _, _ ->
            val presets = PresetsManager.getPresets(context)
            val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, presets)
            val dlg = AlertDialog.Builder(context)
                .setTitle("Existing presets")
                .setAdapter(adapter) { d, which ->
                    val sel = presets[which]
                    AlertDialog.Builder(context).setMessage(sel)
                        .setPositiveButton("Delete") { _, _ -> PresetsManager.removePreset(context, sel) }
                        .setNegativeButton("Cancel", null).show()
                }.create()
            dlg.show()
        }
        builder.setNegativeButton("Close", null)
        builder.show()
    }
}

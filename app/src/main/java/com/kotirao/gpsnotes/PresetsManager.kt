package com.kotirao.gpsnotes

import android.content.Context

object PresetsManager {
    private const val PREFS = "gpsnotes_presets"
    private const val KEY_PRESETS = "presets"

    fun getPresets(context: Context): MutableList<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PRESETS, null) ?: return mutableListOf()
        return raw.split("||").filter { it.isNotEmpty() }.toMutableList()
    }

    fun addPreset(context: Context, preset: String) {
        val list = getPresets(context)
        list.add(preset)
        saveList(context, list)
    }

    fun removePreset(context: Context, preset: String) {
        val list = getPresets(context)
        list.remove(preset)
        saveList(context, list)
    }

    private fun saveList(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PRESETS, list.joinToString("||")).apply()
    }
}

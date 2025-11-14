package com.kotirao.gpsnotes

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException

object CsvStore {
    data class Entry(
        val filename: String,
        val timestamp: String,
        val latitude: Double?,
        val longitude: Double?,
        val accuracy: Float?,
        val address: String?,
        val note: String?
    )

    private const val CSV_NAME = "gpsnotes_metadata.csv"

    fun appendEntry(context: Context, e: Entry) {
        val csvFile = File(context.filesDir, CSV_NAME)
        val writeHeader = !csvFile.exists()
        try {
            FileWriter(csvFile, true).use { fw ->
                if (writeHeader) fw.append("filename,timestamp,latitude,longitude,accuracy,address,note\n")
                fw.append(escape(e.filename)).append(',')
                    .append(escape(e.timestamp)).append(',')
                    .append(e.latitude?.toString() ?: "").append(',')
                    .append(e.longitude?.toString() ?: "").append(',')
                    .append(e.accuracy?.toString() ?: "").append(',')
                    .append(escape(e.address)).append(',')
                    .append(escape(e.note)).append('\n')
            }
        } catch (io: IOException) {
            io.printStackTrace()
        }
    }

    private fun escape(s: String?): String = s?.replace("\n", " ")?.replace(",", " ") ?: ""
}

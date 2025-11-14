package com.kotirao.gpsnotes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

object ImageUtils {
    fun drawOverlayOnBitmap(src: Bitmap, lines: List<String>): Bitmap {
        val mutable = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint().apply {
            textSize = 40f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val padding = 20
        var y = mutable.height - padding - (lines.size * 50)
        for (line in lines) {
            paint.color = 0x99000000.toInt()
            canvas.drawText(line, padding.toFloat()+2, y.toFloat()+2, paint)
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawText(line, padding.toFloat(), y.toFloat(), paint)
            y += 50
        }
        return mutable
    }
}

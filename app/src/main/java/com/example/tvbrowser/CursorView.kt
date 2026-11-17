package com.example.tvbrowser

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

class CursorView(ctx: Context) : View(ctx) {
    var cx = 0f
    var cy = 0f
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2.5f
    }
    private val path = Path()

    init { isFocusable = false; isClickable = false }

    override fun onDraw(c: Canvas) {
        val s = resources.displayMetrics.density * 1.3f
        path.reset()
        path.moveTo(cx, cy)
        path.lineTo(cx, cy + 34 * s)
        path.lineTo(cx + 9 * s, cy + 26 * s)
        path.lineTo(cx + 16 * s, cy + 40 * s)
        path.lineTo(cx + 22 * s, cy + 37 * s)
        path.lineTo(cx + 15 * s, cy + 24 * s)
        path.lineTo(cx + 27 * s, cy + 24 * s)
        path.close()
        c.drawPath(path, fill)
        c.drawPath(path, stroke)
    }
}

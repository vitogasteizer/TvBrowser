package com.example.tvbrowser

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Four white corner arcs drawn at the edge of the safe area; shown while the margin is adjusted. */
class OverscanGuide(ctx: Context) : View(ctx) {
    var inset = 0f // percent of the screen on every side
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
    }

    init { isFocusable = false; isClickable = false }

    override fun onDraw(c: Canvas) {
        val u = Ui.unit(context)
        arc.strokeWidth = 7f * u
        val half = arc.strokeWidth / 2
        val r = 48f * u
        val l = width * inset / 100f + half
        val t = height * inset / 100f + half
        val rt = width - width * inset / 100f - half
        val b = height - height * inset / 100f - half
        c.drawArc(RectF(l, t, l + 2 * r, t + 2 * r), 180f, 90f, false, arc)
        c.drawArc(RectF(rt - 2 * r, t, rt, t + 2 * r), 270f, 90f, false, arc)
        c.drawArc(RectF(rt - 2 * r, b - 2 * r, rt, b), 0f, 90f, false, arc)
        c.drawArc(RectF(l, b - 2 * r, l + 2 * r, b), 90f, 90f, false, arc)
        text.textSize = 20f * u
        c.drawText(context.getString(R.string.overscan_hint), width / 2f, height / 2f, text)
    }
}

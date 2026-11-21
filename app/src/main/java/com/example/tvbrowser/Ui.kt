package com.example.tvbrowser

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

object Ui {
    val BG = 0xFF0E1116.toInt()
    val SURF = 0xFF171B22.toInt()
    val SURF2 = 0xFF222833.toInt()
    val LINE = 0xFF262C36.toInt()
    val TEXT = 0xFFF2F4F8.toInt()
    val MUTED = 0xFF9AA4B2.toInt()
    val ACC = 0xFF6EA8FF.toInt()
    val ON_ACC = 0xFF0B1220.toInt()

    /** One layout unit = 1/1500 of the screen's long side, so the UI scales with any TV resolution. */
    fun unit(ctx: Context): Float {
        val m = ctx.resources.displayMetrics
        return maxOf(m.widthPixels, m.heightPixels) / 1500f
    }

    fun dp(ctx: Context, v: Float) = (v * unit(ctx)).toInt()

    fun rect(ctx: Context, color: Int, r: Float, stroke: Int = 0, sw: Float = 0f): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(ctx, r).toFloat()
            if (stroke != 0) setStroke(dp(ctx, sw), stroke)
        }

    fun btnBg(ctx: Context, color: Int, r: Float): StateListDrawable = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), rect(ctx, color, r, ACC, 3f))
        addState(intArrayOf(android.R.attr.state_pressed), rect(ctx, color, r, MUTED, 3f))
        addState(intArrayOf(), rect(ctx, color, r))
    }

    fun fieldBg(ctx: Context): StateListDrawable = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), rect(ctx, BG, 26f, ACC, 3f))
        addState(intArrayOf(), rect(ctx, BG, 26f, LINE, 1f))
    }

    fun button(ctx: Context, text: String, primary: Boolean = false, sp: Float = 16f, onClick: (() -> Unit)? = null): Button =
        Button(ctx).apply {
            this.text = text
            isAllCaps = false
            px(sp)
            setTextColor(if (primary) ON_ACC else TEXT)
            stateListAnimator = null
            minWidth = 0; minimumWidth = 0
            minHeight = dp(ctx, 52f); minimumHeight = dp(ctx, 52f)
            gravity = Gravity.CENTER
            background = btnBg(ctx, if (primary) ACC else SURF2, 26f)
            setPadding(dp(ctx, 40f), 0, dp(ctx, 40f), 0)
            isFocusable = true
            if (onClick != null) setOnClickListener { onClick() }
        }

    fun icon(ctx: Context, res: Int, desc: String, primary: Boolean = false, onClick: (() -> Unit)? = null): ImageButton =
        ImageButton(ctx).apply {
            setImageResource(res)
            contentDescription = desc
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(ctx, 13f), dp(ctx, 13f), dp(ctx, 13f), dp(ctx, 13f))
            imageTintList = ColorStateList.valueOf(if (primary) ON_ACC else TEXT)
            background = btnBg(ctx, if (primary) ACC else SURF2, 26f)
            stateListAnimator = null
            isFocusable = true
            if (onClick != null) setOnClickListener { onClick() }
        }

    fun field(ctx: Context, hint: String, text: String = ""): EditText = EditText(ctx).apply {
        setSingleLine(true)
        setHint(hint)
        setText(text)
        setTextColor(TEXT)
        setHintTextColor(MUTED)
        px(18f)
        background = fieldBg(ctx)
        setPadding(dp(ctx, 22f), 0, dp(ctx, 22f), 0)
        setSelectAllOnFocus(true)
    }

    fun card(ctx: Context) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = rect(ctx, SURF, 28f, LINE, 1f)
        setPadding(dp(ctx, 34f), dp(ctx, 28f), dp(ctx, 34f), dp(ctx, 16f))
    }

    fun heading(ctx: Context, t: String) = TextView(ctx).apply {
        text = t; setTextColor(TEXT); px(22f); typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, dp(ctx, 12f))
    }

    fun note(ctx: Context, t: String) = TextView(ctx).apply {
        text = t; setTextColor(MUTED); px(16f)
        setPadding(0, 0, 0, dp(ctx, 12f))
    }

    fun row(ctx: Context, label: String, control: View): LinearLayout {
        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(ctx, 84f)
            addView(TextView(ctx).apply { text = label; setTextColor(TEXT); px(18f) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(ctx, 20f) })
            val lp = (control.layoutParams as? LinearLayout.LayoutParams)
                ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(control, lp)
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(View(ctx).apply { setBackgroundColor(LINE) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))
            addView(inner, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
    }

    fun seek(ctx: Context, min: Int, max: Int, value: Int, onChange: (Int) -> Unit): SeekBar = SeekBar(ctx).apply {
        this.max = max - min
        progress = value - min
        keyProgressIncrement = 1
        progressTintList = ColorStateList.valueOf(ACC)
        thumbTintList = ColorStateList.valueOf(ACC)
        layoutParams = LinearLayout.LayoutParams(dp(ctx, 320f), LinearLayout.LayoutParams.WRAP_CONTENT)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) { if (fromUser) onChange(p + min) }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
    }

    fun seg(ctx: Context, opts: List<String>, sel: Int, onSel: (Int) -> Unit): LinearLayout {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rect(ctx, BG, 28f)
            setPadding(dp(ctx, 6f), dp(ctx, 6f), dp(ctx, 6f), dp(ctx, 6f))
        }
        val btns = mutableListOf<Button>()
        fun paint(s: Int) {
            btns.forEachIndexed { i, b ->
                b.background = btnBg(ctx, if (i == s) ACC else Color.TRANSPARENT, 22f)
                b.setTextColor(if (i == s) ON_ACC else MUTED)
            }
        }
        opts.forEachIndexed { i, t ->
            val b = button(ctx, t, false, 16f) { paint(i); onSel(i) }
            b.setPadding(dp(ctx, 32f), 0, dp(ctx, 32f), 0)
            btns.add(b)
            box.addView(b, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(ctx, 52f)).apply { marginStart = dp(ctx, 3f) })
        }
        paint(sel)
        return box
    }

    private fun swatchBg(ctx: Context, c: Int, selected: Boolean): StateListDrawable {
        fun oval(stroke: Int) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(c)
            if (stroke != 0) setStroke(dp(ctx, 3f), stroke)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), oval(ACC))
            addState(intArrayOf(), oval(if (selected) ACC else 0))
        }
    }

    fun swatches(ctx: Context, colors: IntArray, sel: Int, onSel: (Int) -> Unit): LinearLayout {
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val btns = mutableListOf<Button>()
        fun paint(s: Int) { btns.forEachIndexed { i, b -> b.background = swatchBg(ctx, colors[i], i == s) } }
        colors.forEachIndexed { i, _ ->
            val b = Button(ctx)
            b.stateListAnimator = null
            b.isFocusable = true
            b.setOnClickListener { paint(i); onSel(i) }
            btns.add(b)
            box.addView(b, LinearLayout.LayoutParams(dp(ctx, 44f), dp(ctx, 44f)).apply { marginStart = dp(ctx, 14f) })
        }
        paint(sel)
        return box
    }

    fun onText(e: EditText, f: (String) -> Unit) {
        e.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { f(s?.toString() ?: "") }
        })
    }

    /** Full-screen page with a back button, title and scrolling body. */
    fun screen(ctx: Context, title: String, onBack: () -> Unit, body: View): FrameLayout {
        val f = FrameLayout(ctx)
        f.setBackgroundColor(BG)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 48f), dp(ctx, 32f), dp(ctx, 48f), dp(ctx, 24f))
        }
        val head = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(icon(ctx, R.drawable.ic_back, ctx.getString(R.string.back)) { onBack() }, LinearLayout.LayoutParams(dp(ctx, 60f), dp(ctx, 60f)))
        head.addView(TextView(ctx).apply {
            text = title; px(30f); typeface = Typeface.DEFAULT_BOLD; setTextColor(TEXT)
            setPadding(dp(ctx, 20f), 0, 0, 0)
        })
        col.addView(head)
        val sv = ScrollView(ctx)
        sv.addView(body)
        col.addView(sv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(ctx, 24f) })
        f.addView(col, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return f
    }

    /** Two equal columns filled with cards. */
    fun twoCols(ctx: Context, left: List<View>, right: List<View>): LinearLayout {
        fun col(items: List<View>) = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            items.forEach {
                addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = dp(ctx, 20f) })
            }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(col(left), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(ctx, 12f) })
            addView(col(right), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(ctx, 12f) })
        }
    }
}

fun TextView.px(v: Float) {
    setTextSize(TypedValue.COMPLEX_UNIT_PX, v * Ui.unit(context))
}

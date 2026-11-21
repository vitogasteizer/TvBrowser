package com.example.tvbrowser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object SettingsUi {
    class Actions(
        val onMode: () -> Unit,
        val onUa: () -> Unit,
        val onPrefs: () -> Unit,
        val openSync: () -> Unit,
        val clearHistory: () -> Unit,
        val clearCookies: () -> Unit,
        val onUpdate: () -> Unit,
        val onGuide: (Boolean) -> Unit,
        val onLang: () -> Unit
    )

    fun build(ctx: Context, s: Store, a: Actions, close: () -> Unit): View {
        val langCodes = ctx.resources.getStringArray(R.array.lang_codes).toList()
        val langNames = ctx.resources.getStringArray(R.array.lang_names).toList()
        val mouse = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.mouse)))
            addView(Ui.row(ctx, ctx.getString(R.string.cursor_speed), Ui.seek(ctx, 1, 10, s.speed) { s.speed = it }))
            addView(Ui.row(ctx, ctx.getString(R.string.acceleration), Ui.seek(ctx, 0, 10, s.accel) { s.accel = it }))
            addView(Ui.row(ctx, ctx.getString(R.string.cursor_size), Ui.seg(ctx, listOf("S", "M", "L"), s.cursorSize) { s.cursorSize = it; a.onPrefs() }))
            addView(Ui.row(ctx, ctx.getString(R.string.cursor_color), Ui.swatches(ctx, MainActivity.CURSOR_COLORS, s.cursorColor) { s.cursorColor = it; a.onPrefs() }))
        }
        val control = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.control)))
            addView(Ui.row(ctx, ctx.getString(R.string.mode), Ui.seg(ctx, listOf(ctx.getString(R.string.mouse), ctx.getString(R.string.buttons)), if (s.mouse) 0 else 1) { s.mouse = it == 0; a.onMode() }))
            addView(Ui.row(ctx, ctx.getString(R.string.scroll_speed), Ui.seek(ctx, 1, 10, s.scrollSpeed) { s.scrollSpeed = it }))
            addView(Ui.row(ctx, ctx.getString(R.string.language), Ui.seg(ctx, langNames, langCodes.indexOf(s.lang).coerceAtLeast(0)) { s.lang = langCodes[it]; a.onLang() }))
        }
        val zooms = listOf(75, 100, 125, 150)
        val ovVal = TextView(ctx).apply { setTextColor(Ui.MUTED); px(16f); gravity = Gravity.END; minWidth = Ui.dp(ctx, 76f) }
        fun ovText() { ovVal.text = String.format("%.1f%%", s.overscan10 / 10f) }
        ovText()
        val ovSeek = Ui.seek(ctx, 0, 100, s.overscan10) { s.overscan10 = it; ovText(); a.onPrefs(); a.onGuide(true) }
        ovSeek.setOnFocusChangeListener { _, f -> if (f) a.onGuide(true) }
        val ovBox = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ovVal)
            addView(ovSeek)
        }
        val homeField = Ui.field(ctx, ctx.getString(R.string.home_hint), if (s.home == "tvb:home") "" else s.home).apply {
            layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 320f), Ui.dp(ctx, 52f))
        }
        Ui.onText(homeField) { t -> s.home = if (t.isBlank()) "tvb:home" else t.trim() }
        val browser = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.browser)))
            addView(Ui.row(ctx, ctx.getString(R.string.site_version), Ui.seg(ctx, MainActivity.UA_NAMES, s.ua) { s.ua = it; a.onUa() }))
            addView(Ui.row(ctx, ctx.getString(R.string.text_scale), Ui.seg(ctx, zooms.map { "$it%" }, zooms.indexOf(s.zoom).coerceAtLeast(1)) { s.zoom = zooms[it]; a.onPrefs() }))
            addView(Ui.row(ctx, ctx.getString(R.string.overscan), ovBox))
            addView(Ui.row(ctx, ctx.getString(R.string.home_page), homeField))
            addView(Ui.row(ctx, ctx.getString(R.string.autohide), Ui.seg(ctx, listOf(ctx.getString(R.string.off), ctx.getString(R.string.on)), if (s.autohide) 1 else 0) { s.autohide = it == 1; a.onPrefs() }))
        }
        val data = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.data)))
            addView(Ui.row(ctx, ctx.getString(R.string.history), Ui.button(ctx, ctx.getString(R.string.clear), false, 16f) { a.clearHistory() }))
            addView(Ui.row(ctx, ctx.getString(R.string.cookies_logins), Ui.button(ctx, ctx.getString(R.string.clear), false, 16f) { a.clearCookies() }))
            addView(Ui.row(ctx, ctx.getString(R.string.sync), Ui.button(ctx, ctx.getString(R.string.open), true, 16f) { a.openSync() }))
        }
        val urlField = Ui.field(ctx, "https://github.com/.../app-debug.apk", s.updateUrl).apply {
            layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 320f), Ui.dp(ctx, 52f))
        }
        Ui.onText(urlField) { t -> s.updateUrl = t.trim() }
        val update = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.update)))
            addView(Ui.row(ctx, ctx.getString(R.string.version), TextView(ctx).apply { text = BuildConfig.VERSION_NAME; setTextColor(Ui.MUTED); px(18f) }))
            addView(Ui.row(ctx, ctx.getString(R.string.update_link), urlField))
            addView(Ui.row(ctx, ctx.getString(R.string.new_version), Ui.button(ctx, ctx.getString(R.string.update), true, 16f) { a.onUpdate() }))
        }
        return Ui.screen(ctx, ctx.getString(R.string.settings), close, Ui.twoCols(ctx, listOf(mouse, control, update), listOf(browser, data)))
    }
}

object SyncUi {
    private fun toast(ctx: Context, m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
    private val ID_RE = Regex("^usr_[a-z0-9]{6,}$")

    /** replace = this device takes the cloud data; otherwise both sides are merged. */
    private fun work(ctx: Context, s: Store, replace: Boolean, done: () -> Unit) {
        if (!Cloud.configured()) { toast(ctx, ctx.getString(R.string.no_firebase)); return }
        val id = s.syncId ?: s.deviceId
        toast(ctx, ctx.getString(R.string.working))
        Thread {
            val msg = try {
                val remote = Cloud.get(id)
                if (replace) {
                    if (remote == null) {
                        ctx.getString(R.string.nothing_in_cloud)
                    } else {
                        s.replaceBookmarks(remote.bookmarks)
                        s.replaceHistory(remote.history)
                        ctx.getString(R.string.replace_done, s.bookmarks().size, s.history().size)
                    }
                } else {
                    if (remote != null) { s.mergeBookmarks(remote.bookmarks); s.mergeHistory(remote.history) }
                    Cloud.put(id, Cloud.Data(s.bookmarksJson(), s.historyJson()))
                    ctx.getString(R.string.sync_done, s.bookmarks().size, s.history().size)
                }
            } catch (e: Exception) {
                ctx.getString(R.string.sync_failed, e.message ?: "")
            }
            Handler(Looper.getMainLooper()).post { toast(ctx, msg); done() }
        }.start()
    }

    fun build(ctx: Context, s: Store, close: () -> Unit): View {
        var hidden = false
        val idView = TextView(ctx).apply {
            setTextColor(Ui.TEXT); px(18f); typeface = Typeface.MONOSPACE
            background = Ui.rect(ctx, Ui.BG, 26f, Ui.LINE, 1f)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 22f), 0, Ui.dp(ctx, 22f), 0)
        }
        val qrImg = ImageView(ctx)
        val qrBox = FrameLayout(ctx).apply {
            background = Ui.rect(ctx, Color.WHITE, 22f)
            setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 14f), Ui.dp(ctx, 14f), Ui.dp(ctx, 14f))
            addView(qrImg, FrameLayout.LayoutParams(Ui.dp(ctx, 170f), Ui.dp(ctx, 170f)))
        }
        val status = TextView(ctx).apply { setTextColor(Ui.MUTED); px(16f) }
        val counts = TextView(ctx).apply { setTextColor(Ui.MUTED); px(16f) }
        val hint = TextView(ctx).apply { setTextColor(Ui.ACC); px(16f); visibility = View.GONE }
        val btnMerge = Ui.button(ctx, ctx.getString(R.string.sync_merge), true, 18f)
        val btnReplace = Ui.button(ctx, ctx.getString(R.string.sync_replace), false, 18f)
        val blinkers = mutableListOf<ObjectAnimator>()
        fun blink(on: Boolean) {
            blinkers.forEach { it.cancel() }
            blinkers.clear()
            btnMerge.alpha = 1f
            btnReplace.alpha = 1f
            hint.visibility = if (on) View.VISIBLE else View.GONE
            if (on) for (v in listOf<View>(btnMerge, btnReplace)) {
                val a = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.3f)
                a.duration = 650
                a.repeatMode = ValueAnimator.REVERSE
                a.repeatCount = ValueAnimator.INFINITE
                a.start()
                blinkers.add(a)
            }
        }
        hint.text = ctx.getString(R.string.sync_hint)
        fun refresh() {
            idView.text = if (hidden) "usr_••••••••••••••" else s.deviceId
            qrImg.setImageBitmap(ConnectUi.qr(s.deviceId, Ui.dp(ctx, 170f)))
            qrBox.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
            val sid = s.syncId
            status.text = if (sid == null) ctx.getString(R.string.syncing_own) else ctx.getString(R.string.syncing_with, sid)
            counts.text = ctx.getString(R.string.counts, s.bookmarks().size, s.history().size)
        }
        refresh()
        btnMerge.setOnClickListener { work(ctx, s, false) { blink(false); refresh() } }
        btnReplace.setOnClickListener { work(ctx, s, true) { blink(false); refresh() } }
        val wrapLp = { LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        val idRow = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(idView, LinearLayout.LayoutParams(0, Ui.dp(ctx, 52f), 1f))
            addView(Ui.button(ctx, ctx.getString(R.string.hide), false, 16f) { hidden = !hidden; refresh() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { marginStart = Ui.dp(ctx, 10f) })
        }
        val idCard = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.your_id)))
            addView(idRow)
            addView(Ui.note(ctx, ctx.getString(R.string.scan_id)),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = Ui.dp(ctx, 14f) })
            addView(qrBox, wrapLp().apply { bottomMargin = Ui.dp(ctx, 12f) })
        }

        val other = Ui.field(ctx, ctx.getString(R.string.paste_id))
        val connect = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.connect_other)))
            addView(Ui.note(ctx, ctx.getString(R.string.connect_other_hint)))
            addView(other, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 52f)))
            addView(status, wrapLp().apply { topMargin = Ui.dp(ctx, 12f) })
            addView(hint, wrapLp().apply { topMargin = Ui.dp(ctx, 6f) })
            addView(LinearLayout(ctx).apply {
                addView(Ui.button(ctx, ctx.getString(R.string.connect), true, 16f) {
                    val id = other.text.toString().trim().lowercase()
                    if (!ID_RE.matches(id)) {
                        toast(ctx, ctx.getString(R.string.invalid_id))
                    } else {
                        s.syncId = if (id == s.deviceId) null else id
                        refresh()
                        toast(ctx, ctx.getString(R.string.connected))
                        blink(s.syncId != null)
                    }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)))
                addView(Ui.button(ctx, ctx.getString(R.string.use_own_id), false, 16f) { s.syncId = null; blink(false); refresh() },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { marginStart = Ui.dp(ctx, 10f) })
                addView(Ui.button(ctx, ctx.getString(R.string.new_id), false, 16f) { s.syncId = null; s.newDeviceId(); blink(false); refresh(); toast(ctx, ctx.getString(R.string.new_id_created)) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { marginStart = Ui.dp(ctx, 10f) })
            }, wrapLp().apply { topMargin = Ui.dp(ctx, 14f); bottomMargin = Ui.dp(ctx, 10f) })
        }

        val data = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.data)))
            addView(counts, wrapLp().apply { bottomMargin = Ui.dp(ctx, 14f) })
            addView(btnMerge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 60f)))
            addView(btnReplace, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 60f)).apply { topMargin = Ui.dp(ctx, 12f); bottomMargin = Ui.dp(ctx, 10f) })
        }
        val safety = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.security)))
            addView(Ui.note(ctx, ctx.getString(R.string.security_note)))
        }
        val howto = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.sync_howto)))
            addView(Ui.note(ctx, ctx.getString(R.string.sync_help)))
        }
        return Ui.screen(ctx, ctx.getString(R.string.sync), close, Ui.twoCols(ctx, listOf(idCard, connect), listOf(data, safety, howto)))
    }
}

object ConnectUi {
    fun qr(text: String, px: Int): Bitmap {
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
        val w = m.width
        val h = m.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (x in 0 until w) for (y in 0 until h) bmp.setPixel(x, y, if (m.get(x, y)) Ui.ON_ACC else Color.WHITE)
        return Bitmap.createScaledBitmap(bmp, px, px, false)
    }

    fun build(ctx: Context, url: String?, onClose: () -> Unit, onStop: () -> Unit): Pair<View, TextView> {
        val frame = FrameLayout(ctx)
        frame.setBackgroundColor(Ui.BG)
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.rect(ctx, Ui.SURF, 40f, Ui.LINE, 1f)
            setPadding(Ui.dp(ctx, 44f), Ui.dp(ctx, 44f), Ui.dp(ctx, 44f), Ui.dp(ctx, 44f))
        }
        val side = Ui.dp(ctx, 340f)
        if (url != null) {
            val box = FrameLayout(ctx).apply {
                background = Ui.rect(ctx, Color.WHITE, 28f)
                setPadding(Ui.dp(ctx, 24f), Ui.dp(ctx, 24f), Ui.dp(ctx, 24f), Ui.dp(ctx, 24f))
            }
            val img = ImageView(ctx)
            img.setImageBitmap(qr(url, side))
            box.addView(img, FrameLayout.LayoutParams(side, side))
            card.addView(box, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = Ui.dp(ctx, 48f) })
        }
        val status = TextView(ctx).apply {
            text = ctx.getString(R.string.waiting_phone); setTextColor(Ui.MUTED); px(20f)
        }
        val right = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        right.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.connect_phone); setTextColor(Ui.TEXT); px(32f); typeface = Typeface.DEFAULT_BOLD
        })
        listOf(
            ctx.getString(R.string.step1),
            ctx.getString(R.string.step2),
            ctx.getString(R.string.step3)
        ).forEach {
            right.addView(TextView(ctx).apply { text = it; setTextColor(Ui.TEXT); px(20f); setPadding(0, Ui.dp(ctx, 14f), 0, 0) })
        }
        right.addView(TextView(ctx).apply {
            text = url ?: ctx.getString(R.string.no_wifi)
            setTextColor(Ui.TEXT); px(18f); typeface = Typeface.MONOSPACE
            background = Ui.rect(ctx, Ui.BG, 22f, Ui.LINE, 1f)
            setPadding(Ui.dp(ctx, 20f), Ui.dp(ctx, 14f), Ui.dp(ctx, 20f), Ui.dp(ctx, 14f))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = Ui.dp(ctx, 24f) })
        right.addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = Ui.dp(ctx, 20f) })
        right.addView(LinearLayout(ctx).apply {
            addView(Ui.button(ctx, ctx.getString(R.string.close), true, 18f) { onClose() }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 56f)))
            addView(Ui.button(ctx, ctx.getString(R.string.disconnect), false, 18f) { onStop() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 56f)).apply { marginStart = Ui.dp(ctx, 12f) })
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = Ui.dp(ctx, 24f) })
        card.addView(right)
        frame.addView(card, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        return frame to status
    }
}

object FilesUi {
    private fun fmt(b: Long) = if (b >= 1048576) "%.1f MB".format(b / 1048576.0) else "%d KB".format(maxOf(1L, b / 1024))

    fun build(ctx: Context, dir: File, onOpen: (File) -> Unit, onPick: () -> Unit, close: () -> Unit): View {
        val list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        fun refresh() {
            list.removeAllViews()
            val files = (dir.listFiles() ?: emptyArray<File>()).filter { it.isFile }.sortedByDescending { it.lastModified() }
            if (files.isEmpty()) list.addView(Ui.note(ctx, ctx.getString(R.string.no_files)))
            files.forEach { f ->
                val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
                val ob = Ui.button(ctx, f.name + "   (" + fmt(f.length()) + ")", false, 16f) { onOpen(f) }
                ob.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                row.addView(ob, LinearLayout.LayoutParams(0, Ui.dp(ctx, 52f), 1f))
                row.addView(Ui.button(ctx, ctx.getString(R.string.delete), false, 16f) { f.delete(); refresh() },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { marginStart = Ui.dp(ctx, 10f) })
                list.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = Ui.dp(ctx, 10f) })
            }
        }
        refresh()
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(Ui.note(ctx, ctx.getString(R.string.folder_label, dir.absolutePath)))
            addView(Ui.button(ctx, ctx.getString(R.string.pick_file), true, 16f) { onPick() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { bottomMargin = Ui.dp(ctx, 16f) })
            addView(list)
        }
        return Ui.screen(ctx, ctx.getString(R.string.files), close, body)
    }
}

object ListUi {
    fun build(
        ctx: Context, title: String, items: List<Pair<String, String>>,
        onOpen: (String) -> Unit, onDelete: ((String) -> Unit)?,
        actions: List<Pair<String, () -> Unit>>, close: () -> Unit
    ): View {
        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        if (actions.isNotEmpty()) {
            val bar = LinearLayout(ctx)
            actions.forEachIndexed { i, a ->
                bar.addView(Ui.button(ctx, a.first, i == 0, 16f) { a.second() },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { if (i > 0) marginStart = Ui.dp(ctx, 10f) })
            }
            body.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = Ui.dp(ctx, 18f) })
        }
        if (items.isEmpty()) body.addView(Ui.note(ctx, ctx.getString(R.string.empty)))
        items.forEach { (t, u) ->
            val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
            val ob = Ui.button(ctx, t.ifBlank { u } + "\n" + u, false, 15f) { onOpen(u) }
            ob.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            ob.maxLines = 2
            ob.ellipsize = TextUtils.TruncateAt.END
            ob.setPadding(Ui.dp(ctx, 18f), Ui.dp(ctx, 8f), Ui.dp(ctx, 18f), Ui.dp(ctx, 8f))
            row.addView(ob, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            if (onDelete != null) {
                row.addView(Ui.button(ctx, ctx.getString(R.string.delete), false, 15f) { onDelete(u) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { marginStart = Ui.dp(ctx, 10f) })
            }
            body.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = Ui.dp(ctx, 10f) })
        }
        return Ui.screen(ctx, title, close, body)
    }
}

object AddSiteUi {
    fun build(ctx: Context, onAdd: (String, String) -> Unit, close: () -> Unit): View {
        val n = Ui.field(ctx, ctx.getString(R.string.name_optional))
        val u = Ui.field(ctx, ctx.getString(R.string.address_example))
        val card = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, ctx.getString(R.string.new_site)))
            addView(Ui.note(ctx, ctx.getString(R.string.type_with_phone)))
            addView(n, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 52f)))
            addView(u, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 52f)).apply { topMargin = Ui.dp(ctx, 12f) })
            addView(LinearLayout(ctx).apply {
                addView(Ui.button(ctx, ctx.getString(R.string.add), true, 18f) { onAdd(n.text.toString(), u.text.toString()) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 56f)))
                addView(Ui.button(ctx, ctx.getString(R.string.cancel), false, 18f) { close() },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 56f)).apply { marginStart = Ui.dp(ctx, 10f) })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Ui.dp(ctx, 18f); bottomMargin = Ui.dp(ctx, 10f)
            })
        }
        val wrap = LinearLayout(ctx)
        wrap.addView(card, LinearLayout.LayoutParams(Ui.dp(ctx, 760f), LinearLayout.LayoutParams.WRAP_CONTENT))
        return Ui.screen(ctx, ctx.getString(R.string.add_site), close, wrap)
    }
}

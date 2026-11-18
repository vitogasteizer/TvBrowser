package com.example.tvbrowser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
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
        val onUpdate: () -> Unit
    )

    fun build(ctx: Context, s: Store, a: Actions, close: () -> Unit): View {
        val mouse = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, "მაუსი"))
            addView(Ui.row(ctx, "კურსორის სიჩქარე", Ui.seek(ctx, 1, 10, s.speed) { s.speed = it }))
            addView(Ui.row(ctx, "აჩქარება დაჭერისას", Ui.seek(ctx, 0, 10, s.accel) { s.accel = it }))
            addView(Ui.row(ctx, "კურსორის ზომა", Ui.seg(ctx, listOf("S", "M", "L"), s.cursorSize) { s.cursorSize = it; a.onPrefs() }))
            addView(Ui.row(ctx, "კურსორის ფერი", Ui.swatches(ctx, MainActivity.CURSOR_COLORS, s.cursorColor) { s.cursorColor = it; a.onPrefs() }))
        }
        val control = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, "მართვა"))
            addView(Ui.row(ctx, "რეჟიმი", Ui.seg(ctx, listOf("მაუსი", "ღილაკები"), if (s.mouse) 0 else 1) { s.mouse = it == 0; a.onMode() }))
            addView(Ui.row(ctx, "გადახვევის სიჩქარე", Ui.seek(ctx, 1, 10, s.scrollSpeed) { s.scrollSpeed = it }))
        }
        val zooms = listOf(75, 100, 125, 150)
        val homeField = Ui.field(ctx, "ცარიელი = საწყისი ეკრანი", if (s.home == "tvb:home") "" else s.home).apply {
            layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 320f), Ui.dp(ctx, 52f))
        }
        Ui.onText(homeField) { t -> s.home = if (t.isBlank()) "tvb:home" else t.trim() }
        val browser = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, "ბრაუზერი"))
            addView(Ui.row(ctx, "საიტის ვერსია", Ui.seg(ctx, MainActivity.UA_NAMES, s.ua) { s.ua = it; a.onUa() }))
            addView(Ui.row(ctx, "ტექსტის მასშტაბი", Ui.seg(ctx, zooms.map { "$it%" }, zooms.indexOf(s.zoom).coerceAtLeast(1)) { s.zoom = zooms[it]; a.onPrefs() }))
            addView(Ui.row(ctx, "საწყისი გვერდი", homeField))
            addView(Ui.row(ctx, "პანელის ავტომატური დამალვა", Ui.seg(ctx, listOf("გამორთ.", "ჩართ."), if (s.autohide) 1 else 0) { s.autohide = it == 1; a.onPrefs() }))
        }
        val data = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, "მონაცემები"))
            addView(Ui.row(ctx, "ისტორია", Ui.button(ctx, "გასუფთავება", false, 16f) { a.clearHistory() }))
            addView(Ui.row(ctx, "Cookie-ები და შესვლები", Ui.button(ctx, "გასუფთავება", false, 16f) { a.clearCookies() }))
            addView(Ui.row(ctx, "სინქრონიზაცია", Ui.button(ctx, "გახსნა", true, 16f) { a.openSync() }))
        }
        val urlField = Ui.field(ctx, "https://github.com/.../app-debug.apk", s.updateUrl).apply {
            layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 320f), Ui.dp(ctx, 52f))
        }
        Ui.onText(urlField) { t -> s.updateUrl = t.trim() }
        val update = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, "განახლება"))
            addView(Ui.row(ctx, "ვერსია", TextView(ctx).apply { text = BuildConfig.VERSION_NAME; setTextColor(Ui.MUTED); px(18f) }))
            addView(Ui.row(ctx, "განახლების ბმული", urlField))
            addView(Ui.row(ctx, "ახალი ვერსია", Ui.button(ctx, "განახლება", true, 16f) { a.onUpdate() }))
        }
        return Ui.screen(ctx, "პარამეტრები", close, Ui.twoCols(ctx, listOf(mouse, control, update), listOf(browser, data)))
    }
}

object SyncUi {
    private fun toast(ctx: Context, m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()

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
        fun refresh() {
            idView.text = if (hidden) "usr_••••••••••••••" else s.deviceId
            qrImg.setImageBitmap(ConnectUi.qr(s.deviceId, Ui.dp(ctx, 170f)))
            qrBox.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
        }
        refresh()
        val idRow = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(idView, LinearLayout.LayoutParams(0, Ui.dp(ctx, 52f), 1f))
            addView(Ui.button(ctx, "დამალვა", false, 16f) { hidden = !hidden; refresh() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { marginStart = Ui.dp(ctx, 10f) })
        }
        val idCard = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, "შენი მოწყობილობის ID"))
            addView(idRow)
            addView(Ui.note(ctx, "დაასკანერე ტელეფონით, რომ ID გადაიტანო სხვა მოწყობილობაზე."),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = Ui.dp(ctx, 14f) })
            addView(qrBox, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = Ui.dp(ctx, 12f) })
        }

        val other = Ui.field(ctx, "ჩასვი ID (მაგ: usr_...)")
        val connect = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, "სხვა მოწყობილობის დაკავშირება"))
            addView(Ui.note(ctx, "შეიყვანე სხვა მოწყობილობის ID იმავე მონაცემებზე წვდომისთვის. აკრეფა შეგიძლია ტელეფონითაც."))
            addView(other, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 52f)))
            addView(LinearLayout(ctx).apply {
                addView(Ui.button(ctx, "დაკავშირება", true, 16f) { toast(ctx, "Firebase ჯერ არ არის მიერთებული") },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)))
                addView(Ui.button(ctx, "ახალი ID", false, 16f) { s.newDeviceId(); refresh(); toast(ctx, "ახალი ID შეიქმნა") },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { marginStart = Ui.dp(ctx, 10f) })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Ui.dp(ctx, 14f); bottomMargin = Ui.dp(ctx, 10f)
            })
        }

        val data = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, "მონაცემები"))
            addView(Ui.note(ctx, "ბუქმარკები: ${s.bookmarks().size}    ისტორია: ${s.history().size}    ღრუბელი: —"))
            addView(LinearLayout(ctx).apply {
                addView(Ui.button(ctx, "სინქრონიზაცია", true, 18f) { toast(ctx, "Firebase ჯერ არ არის მიერთებული") },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 60f)))
                addView(Ui.button(ctx, "გადმოწერა", false, 18f) { toast(ctx, "Firebase ჯერ არ არის მიერთებული") },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 60f)).apply { marginStart = Ui.dp(ctx, 10f) })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = Ui.dp(ctx, 10f) })
        }
        val safety = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, "უსაფრთხოება"))
            addView(Ui.note(ctx, "ID მუშაობს როგორც პაროლი: ვისაც ის აქვს, ამ ბუქმარკებსა და ისტორიას ნახავს. არ გაუზიარო უცნობს."))
        }
        return Ui.screen(ctx, "სინქრონიზაცია", close, Ui.twoCols(ctx, listOf(idCard, connect), listOf(data, safety)))
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
            text = "ელოდება ტელეფონს…"; setTextColor(Ui.MUTED); px(20f)
        }
        val right = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        right.addView(TextView(ctx).apply {
            text = "ტელეფონის დაკავშირება"; setTextColor(Ui.TEXT); px(32f); typeface = Typeface.DEFAULT_BOLD
        })
        listOf(
            "1. ტელეფონი და ტელევიზორი ერთ Wi-Fi-ზე შეაერთე",
            "2. დაასკანერე QR კოდი კამერით",
            "3. ტელეფონი გახდება ტრეკპადი და კლავიატურა"
        ).forEach {
            right.addView(TextView(ctx).apply { text = it; setTextColor(Ui.TEXT); px(20f); setPadding(0, Ui.dp(ctx, 14f), 0, 0) })
        }
        right.addView(TextView(ctx).apply {
            text = url ?: "Wi-Fi ვერ ვიპოვე. შეაერთე ტელევიზორი ქსელში."
            setTextColor(Ui.TEXT); px(18f); typeface = Typeface.MONOSPACE
            background = Ui.rect(ctx, Ui.BG, 22f, Ui.LINE, 1f)
            setPadding(Ui.dp(ctx, 20f), Ui.dp(ctx, 14f), Ui.dp(ctx, 20f), Ui.dp(ctx, 14f))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = Ui.dp(ctx, 24f) })
        right.addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = Ui.dp(ctx, 20f) })
        right.addView(LinearLayout(ctx).apply {
            addView(Ui.button(ctx, "დახურვა", true, 18f) { onClose() }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 56f)))
            addView(Ui.button(ctx, "გათიშვა", false, 18f) { onStop() },
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
            if (files.isEmpty()) list.addView(Ui.note(ctx, "ფაილები ჯერ არ არის. ჩამოტვირთე ფაილი საიტზე ბმულზე დაჭერით."))
            files.forEach { f ->
                val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
                val ob = Ui.button(ctx, f.name + "   (" + fmt(f.length()) + ")", false, 16f) { onOpen(f) }
                ob.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                row.addView(ob, LinearLayout.LayoutParams(0, Ui.dp(ctx, 52f), 1f))
                row.addView(Ui.button(ctx, "წაშლა", false, 16f) { f.delete(); refresh() },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { marginStart = Ui.dp(ctx, 10f) })
                list.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = Ui.dp(ctx, 10f) })
            }
        }
        refresh()
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(Ui.note(ctx, "საქაღალდე: " + dir.absolutePath))
            addView(Ui.button(ctx, "სხვა ფაილის არჩევა…", true, 16f) { onPick() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { bottomMargin = Ui.dp(ctx, 16f) })
            addView(list)
        }
        return Ui.screen(ctx, "ფაილები", close, body)
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
        if (items.isEmpty()) body.addView(Ui.note(ctx, "ცარიელია"))
        items.forEach { (t, u) ->
            val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
            val ob = Ui.button(ctx, t.ifBlank { u } + "\n" + u, false, 15f) { onOpen(u) }
            ob.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            ob.maxLines = 2
            ob.ellipsize = TextUtils.TruncateAt.END
            ob.setPadding(Ui.dp(ctx, 18f), Ui.dp(ctx, 8f), Ui.dp(ctx, 18f), Ui.dp(ctx, 8f))
            row.addView(ob, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            if (onDelete != null) {
                row.addView(Ui.button(ctx, "წაშლა", false, 15f) { onDelete(u) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 52f)).apply { marginStart = Ui.dp(ctx, 10f) })
            }
            body.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = Ui.dp(ctx, 10f) })
        }
        return Ui.screen(ctx, title, close, body)
    }
}

object AddSiteUi {
    fun build(ctx: Context, onAdd: (String, String) -> Unit, close: () -> Unit): View {
        val n = Ui.field(ctx, "სახელი (არასავალდებულო)")
        val u = Ui.field(ctx, "მისამართი, მაგ. youtube.com")
        val card = Ui.card(ctx).apply {
            addView(Ui.heading(ctx, "ახალი საიტი"))
            addView(Ui.note(ctx, "ტექსტის აკრეფა შეგიძლია ტელეფონითაც (პანელზე ღილაკი ტელეფონი)."))
            addView(n, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 52f)))
            addView(u, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 52f)).apply { topMargin = Ui.dp(ctx, 12f) })
            addView(LinearLayout(ctx).apply {
                addView(Ui.button(ctx, "დამატება", true, 18f) { onAdd(n.text.toString(), u.text.toString()) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 56f)))
                addView(Ui.button(ctx, "გაუქმება", false, 18f) { close() },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(ctx, 56f)).apply { marginStart = Ui.dp(ctx, 10f) })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Ui.dp(ctx, 18f); bottomMargin = Ui.dp(ctx, 10f)
            })
        }
        val wrap = LinearLayout(ctx)
        wrap.addView(card, LinearLayout.LayoutParams(Ui.dp(ctx, 760f), LinearLayout.LayoutParams.WRAP_CONTENT))
        return Ui.screen(ctx, "საიტის დამატება", close, wrap)
    }
}

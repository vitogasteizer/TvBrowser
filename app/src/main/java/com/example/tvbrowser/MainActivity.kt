package com.example.tvbrowser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

class MainActivity : Activity() {

    companion object {
        val UAS = listOf(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Linux; Android 13; Pixel Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        )
        val UA_NAMES = listOf("Desktop", "Tablet", "Phone")
        val CURSOR_COLORS = intArrayOf(
            0xFFFFFFFF.toInt(), 0xFFFFD54A.toInt(), 0xFF6EE7A8.toInt(), 0xFFFF8A7A.toInt()
        )
        const val HOME = "tvb:home"
        const val START_BASE = "https://tvb.local/"
    }

    private lateinit var root: FrameLayout
    private lateinit var column: LinearLayout
    private lateinit var content: FrameLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var web: WebView
    private lateinit var cursor: CursorView
    private lateinit var urlBox: EditText
    private lateinit var modeBtn: Button
    private lateinit var uaBtn: Button
    private lateinit var promptBox: LinearLayout
    private lateinit var store: Store
    private val handler = Handler(Looper.getMainLooper())

    private var mouseMode = true
    private var uaMode = 0
    private var curX = 0f
    private var curY = 0f
    private var longPressed = false
    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null
    private var chrome: WebChromeClient? = null
    private var overlay: View? = null
    private var server: RemoteServer? = null
    private var connectStatus: TextView? = null
    private var dismissedUrl: String? = null
    private var pseudoFull = false
    private var promptVisible = false

    private val hidePrompt = Runnable { showPrompt(false) }
    private val hideBar = Runnable {
        if (store.autohide && customView == null && overlay == null && !urlBox.hasFocus()) {
            if (toolbar.hasFocus()) web.requestFocus()
            toolbar.visibility = View.GONE
        }
    }

    private fun dp(x: Float) = Ui.dp(this, x)
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    // ------------------------------------------------------------ UI
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        store = Store(this)
        mouseMode = store.mouse
        uaMode = store.ua

        root = FrameLayout(this).apply { setBackgroundColor(Ui.BG) }
        column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.rect(this@MainActivity, Ui.SURF, 40f)
            setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
        }
        content = FrameLayout(this)
        web = WebView(this)
        web.setBackgroundColor(Ui.BG)
        cursor = CursorView(this)

        urlBox = Ui.field(this, "მოძებნე ან ჩაწერე მისამართი").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, id, ev ->
                if (id == EditorInfo.IME_ACTION_GO || ev?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    go(text.toString()); true
                } else false
            }
        }
        modeBtn = Ui.button(this, "", true, 15f) { mouseMode = !mouseMode; applyMode(); web.requestFocus() }
        uaBtn = Ui.button(this, "", false, 15f) { uaMode = (uaMode + 1) % 3; store.ua = uaMode; applyUa(true); web.requestFocus() }

        fun add(v: View, w: Int = LinearLayout.LayoutParams.WRAP_CONTENT) {
            toolbar.addView(v, LinearLayout.LayoutParams(w, dp(52f)).apply { marginStart = dp(4f); marginEnd = dp(4f) })
        }
        add(Ui.button(this, "◀", false, 18f) { if (web.canGoBack()) web.goBack(); web.requestFocus() }, dp(54f))
        add(Ui.button(this, "▶", false, 18f) { if (web.canGoForward()) web.goForward(); web.requestFocus() }, dp(54f))
        add(Ui.button(this, "⟳", false, 18f) { web.reload(); web.requestFocus() }, dp(54f))
        add(Ui.button(this, "მთავარი", false, 15f) { open(HOME); web.requestFocus() })
        toolbar.addView(urlBox, LinearLayout.LayoutParams(0, dp(52f), 1f).apply { marginStart = dp(4f); marginEnd = dp(4f) })
        add(Ui.button(this, "სანიშნეები", false, 15f) { showBookmarks() })
        add(Ui.button(this, "ისტორია", false, 15f) { showHistory() })
        add(Ui.button(this, "ტელეფონი", false, 15f) { showConnect() })
        add(uaBtn)
        add(modeBtn)
        add(Ui.button(this, "პარამეტრები", false, 15f) { showSettings() })

        column.addView(toolbar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(dp(24f), dp(16f), dp(24f), dp(12f)) })

        promptBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            background = Ui.rect(this@MainActivity, Ui.SURF, 32f, Ui.ACC, 2f)
            setPadding(dp(24f), dp(16f), dp(24f), dp(16f))
            addView(TextView(this@MainActivity).apply {
                text = "ვიდეო ნაპოვნია\nგაუშვა სრულ ეკრანზე?"; setTextColor(Ui.TEXT); textSize = 17f
            })
            addView(Ui.button(this@MainActivity, "▶ სრულ ეკრანზე", true, 17f) { goFullscreen() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(56f)).apply { marginStart = dp(22f) })
            addView(Ui.button(this@MainActivity, "არა", false, 17f) { dismissPrompt() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(56f)).apply { marginStart = dp(10f) })
        }

        content.addView(web, FrameLayout.LayoutParams(-1, -1))
        content.addView(promptBox, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(32f), dp(32f)) })
        content.addView(cursor, FrameLayout.LayoutParams(-1, -1))
        column.addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        setupWeb()
        applyUa(false)
        applyMode()
        applyPrefs()
        content.post {
            curX = content.width / 2f
            curY = content.height / 2f
            updateCursor()
        }
        open(store.home)
    }

    // ------------------------------------------------------------ web
    inner class Bridge {
        @JavascriptInterface fun video(f: Boolean) { runOnUiThread { showPrompt(f) } }
        @JavascriptInterface fun pseudo(f: Boolean) { runOnUiThread { pseudoFull = f; if (f) showPrompt(false) } }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWeb() {
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            javaScriptCanOpenWindowsAutomatically = true
        }
        web.addJavascriptInterface(Bridge(), "TVB")
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val s = r.url.scheme ?: return false
                return s !in listOf("http", "https", "about", "data", "blob", "file")
            }
            override fun onPageStarted(v: WebView, url: String, favicon: Bitmap?) {
                dismissedUrl = null
                pseudoFull = false
                showPrompt(false)
            }
            override fun onPageFinished(v: WebView, url: String) {
                if (url.startsWith(START_BASE)) {
                    if (!urlBox.hasFocus()) urlBox.setText("")
                    return
                }
                if (!urlBox.hasFocus()) urlBox.setText(url)
                store.addHistory(v.title ?: url, url)
                v.evaluateJavascript(Js.INIT, null)
            }
        }
        val cc = object : WebChromeClient() {
            override fun onShowCustomView(view: View, cb: CustomViewCallback) {
                customView = view
                customCb = cb
                root.addView(view, FrameLayout.LayoutParams(-1, -1))
                column.visibility = View.GONE
                cursor.visibility = View.GONE
                showPrompt(false)
            }
            override fun onHideCustomView() {
                customView?.let { root.removeView(it) }
                customView = null
                customCb?.onCustomViewHidden()
                customCb = null
                column.visibility = View.VISIBLE
                applyMode()
            }
        }
        chrome = cc
        web.webChromeClient = cc
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun startHtml(): String {
        val defaults = listOf(
            "YouTube" to "https://www.youtube.com", "Vimeo" to "https://vimeo.com",
            "Twitch" to "https://www.twitch.tv", "Wikipedia" to "https://www.wikipedia.org",
            "Google" to "https://www.google.com"
        )
        val items = store.bookmarks().take(11).ifEmpty { defaults }
        val tiles = items.joinToString("") { (t, u) ->
            val l = (t.trim().firstOrNull() ?: '•').uppercaseChar()
            "<a class=t href=\"${esc(u)}\"><i>$l</i>${esc(t.take(22))}</a>"
        }
        return """<!doctype html><html lang="ka"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><style>
body{margin:0;min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:40px;background:#0E1116;color:#F2F4F8;font-family:sans-serif}
h1{margin:0;font-size:46px}
form{width:min(760px,80vw)}
input{width:100%;box-sizing:border-box;height:72px;border-radius:36px;border:2px solid #262C36;background:#171B22;color:#F2F4F8;padding:0 32px;font-size:24px}
input:focus{outline:3px solid #6EA8FF}
.g{display:flex;flex-wrap:wrap;gap:24px;justify-content:center;max-width:1300px}
.t{width:200px;height:180px;box-sizing:border-box;border-radius:28px;background:#171B22;border:2px solid #262C36;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px;color:#F2F4F8;text-decoration:none;font-size:22px;font-weight:600}
.t:focus{outline:4px solid #6EA8FF;background:#222833}
.t i{width:76px;height:76px;border-radius:38px;background:#243049;color:#6EA8FF;display:flex;align-items:center;justify-content:center;font-style:normal;font-size:34px;font-weight:700}
</style></head><body><h1>რა გინდა ნახო დღეს?</h1>
<form action="https://www.google.com/search" method="get"><input name="q" placeholder="ძებნა ან მისამართი" autocomplete="off"></form>
<div class="g">$tiles</div></body></html>"""
    }

    private fun open(url: String) {
        if (url == HOME) {
            urlBox.setText("")
            web.loadDataWithBaseURL(START_BASE, startHtml(), "text/html", "utf-8", null)
        } else {
            go(url)
        }
    }

    private fun go(input: String) {
        val s = input.trim()
        if (s.isEmpty()) return
        val url = when {
            s.startsWith("http://") || s.startsWith("https://") -> s
            s.contains(" ") || !s.contains(".") -> "https://www.google.com/search?q=" + Uri.encode(s)
            else -> "https://$s"
        }
        web.loadUrl(url)
        web.requestFocus()
    }

    private fun applyUa(reload: Boolean) {
        web.settings.apply {
            userAgentString = UAS[uaMode]
            useWideViewPort = uaMode == 0
            loadWithOverviewMode = uaMode == 0
        }
        uaBtn.text = UA_NAMES[uaMode]
        if (reload) web.reload()
    }

    private fun applyMode() {
        cursor.visibility = if (mouseMode && customView == null && overlay == null) View.VISIBLE else View.GONE
        modeBtn.text = if (mouseMode) "მაუსი" else "ღილაკები"
        store.mouse = mouseMode
        if (overlay == null) web.requestFocus()
    }

    private fun applyPrefs() {
        cursor.style(store.cursorSize, CURSOR_COLORS[store.cursorColor.coerceIn(0, CURSOR_COLORS.size - 1)])
        web.settings.textZoom = store.zoom
        if (!store.autohide) toolbar.visibility = View.VISIBLE
        scheduleHide()
    }

    private fun scheduleHide() {
        handler.removeCallbacks(hideBar)
        if (store.autohide) handler.postDelayed(hideBar, 5000)
    }

    private fun showBar() {
        toolbar.visibility = View.VISIBLE
        scheduleHide()
        toolbar.post { toolbar.getChildAt(0).requestFocus() }
    }

    // ------------------------------------------------------------ video prompt / fullscreen
    private fun showPrompt(show: Boolean) {
        handler.removeCallbacks(hidePrompt)
        if (show) {
            if (dismissedUrl != null && dismissedUrl == web.url) return
            if (customView != null || overlay != null || pseudoFull) return
            promptBox.visibility = View.VISIBLE
            promptVisible = true
            handler.postDelayed(hidePrompt, 15000)
        } else {
            promptBox.visibility = View.GONE
            promptVisible = false
        }
    }

    private fun dismissPrompt() {
        dismissedUrl = web.url
        showPrompt(false)
        web.requestFocus()
    }

    private fun touchAt(x: Float, y: Float, after: (() -> Unit)? = null) {
        val t = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0)
        web.dispatchTouchEvent(down)
        down.recycle()
        web.postDelayed({
            val up = MotionEvent.obtain(t, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0)
            web.dispatchTouchEvent(up)
            up.recycle()
            if (after != null) web.postDelayed({ after() }, 120)
        }, 60)
    }

    private fun goFullscreen() {
        showPrompt(false)
        if (customView != null) { chrome?.onHideCustomView(); return }
        if (pseudoFull) { web.evaluateJavascript("window.__tvbExit&&window.__tvbExit()", null); return }
        // a real touch gives the page the "user gesture" browsers require for fullscreen
        touchAt(2f, 2f) { web.evaluateJavascript("window.__tvbFull&&window.__tvbFull()", null) }
    }

    private fun togglePlay() { web.evaluateJavascript(Js.TOGGLE, null) }
    private fun seek(sec: Int) { web.evaluateJavascript(Js.SEEK.replace("__D__", sec.toString()), null) }

    // ------------------------------------------------------------ mouse cursor
    private fun updateCursor() { cursor.cx = curX; cursor.cy = curY; cursor.invalidate() }

    private fun hover() {
        val t = SystemClock.uptimeMillis()
        val ev = MotionEvent.obtain(t, t, MotionEvent.ACTION_HOVER_MOVE, curX, curY, 0)
        ev.source = InputDevice.SOURCE_MOUSE
        web.dispatchGenericMotionEvent(ev)
        ev.recycle()
    }

    private fun moveBy(dx: Float, dy: Float, toolbarOnTop: Boolean) {
        val w = content.width.toFloat()
        val h = content.height.toFloat()
        val edge = 2f * resources.displayMetrics.density
        if (dy > 0 && curY >= h - 1 - edge) web.scrollBy(0, (abs(dy) * 1.5f).toInt())
        if (dy < 0 && curY <= edge) {
            if (web.scrollY > 0) web.scrollBy(0, (-abs(dy) * 1.5f).toInt())
            else if (toolbarOnTop) { showBar(); return }
        }
        curX = (curX + dx).coerceIn(0f, w - 1)
        curY = (curY + dy).coerceIn(0f, h - 1)
        updateCursor()
        hover()
    }

    private fun promptButtonAt(x: Float, y: Float): Button? {
        if (!promptVisible) return null
        val cl = IntArray(2)
        content.getLocationOnScreen(cl)
        val sx = cl[0] + x
        val sy = cl[1] + y
        for (i in 0 until promptBox.childCount) {
            val v = promptBox.getChildAt(i)
            if (v is Button) {
                val l = IntArray(2)
                v.getLocationOnScreen(l)
                if (sx >= l[0] && sx <= l[0] + v.width && sy >= l[1] && sy <= l[1] + v.height) return v
            }
        }
        return null
    }

    private fun click() {
        val b = promptButtonAt(curX, curY)
        if (b != null) { b.performClick(); return }
        touchAt(curX, curY)
    }

    private fun pressKey(code: Int) {
        val t = SystemClock.uptimeMillis()
        dispatchKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0))
        dispatchKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0))
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN) scheduleHide()
        if (overlay != null) return super.dispatchKeyEvent(e)
        val k = e.keyCode
        if (customView != null) {
            val media = k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER ||
                k == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || k == KeyEvent.KEYCODE_MEDIA_PLAY || k == KeyEvent.KEYCODE_MEDIA_PAUSE
            if (media || k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (e.action == KeyEvent.ACTION_DOWN) {
                    if (media) { if (e.repeatCount == 0) togglePlay() }
                    else seek(if (k == KeyEvent.KEYCODE_DPAD_LEFT) -10 else 10)
                }
                return true
            }
            return super.dispatchKeyEvent(e)
        }
        if (!mouseMode || toolbar.hasFocus() || promptBox.hasFocus()) return super.dispatchKeyEvent(e)
        val d = resources.displayMetrics.density
        when (k) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (e.action == KeyEvent.ACTION_DOWN) {
                    val step = (4f + store.speed * 1.5f + min(e.repeatCount, 25) * (0.5f + store.accel * 0.25f)) * d
                    var dx = 0f
                    var dy = 0f
                    when (k) {
                        KeyEvent.KEYCODE_DPAD_UP -> dy = -step
                        KeyEvent.KEYCODE_DPAD_DOWN -> dy = step
                        KeyEvent.KEYCODE_DPAD_LEFT -> dx = -step
                        else -> dx = step
                    }
                    moveBy(dx, dy, true)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (e.action == KeyEvent.ACTION_DOWN && e.isLongPress) {
                    longPressed = true
                    showBar()
                } else if (e.action == KeyEvent.ACTION_UP) {
                    if (!longPressed) click()
                    longPressed = false
                }
                return true
            }
        }
        return super.dispatchKeyEvent(e)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            overlay != null -> closeOverlay()
            customView != null -> chrome?.onHideCustomView()
            pseudoFull -> web.evaluateJavascript("window.__tvbExit&&window.__tvbExit()", null)
            web.canGoBack() -> web.goBack()
            else -> super.onBackPressed()
        }
    }

    // ------------------------------------------------------------ phone remote
    private fun localIp(): String? {
        try {
            for (ni in Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp || ni.isLoopback) continue
                for (a in Collections.list(ni.inetAddresses)) {
                    if (a is Inet4Address && !a.isLoopbackAddress && a.isSiteLocalAddress) return a.hostAddress
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun ensureServer(): RemoteServer {
        val s = server ?: RemoteServer(
            { msg -> runOnUiThread { onRemote(msg) } },
            { n -> runOnUiThread { connectStatus?.let { it.text = if (n > 0) "● ტელეფონი დაკავშირებულია" else "ელოდება ტელეფონს…" } } }
        )
        server = s
        return s
    }

    private fun showConnect() {
        val srv = ensureServer()
        val ok = srv.start()
        val ip = localIp()
        val url = if (ok && ip != null) "http://$ip:${srv.port}/?k=${srv.token}" else null
        val (view, status) = ConnectUi.build(this, url, { closeOverlay() }, {
            srv.stop(); server = null; closeOverlay(); toast("ტელეფონი გათიშულია")
        })
        connectStatus = status
        showOverlay(view)
    }

    private fun focusTarget(): View = currentFocus ?: web

    private fun keyTo(v: View, code: Int) {
        val t = SystemClock.uptimeMillis()
        v.dispatchKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0))
        v.dispatchKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0))
    }

    private fun typeText(t: String) {
        val f = currentFocus
        if (f is EditText) {
            val s = f.selectionStart.coerceAtLeast(0)
            val e = f.selectionEnd.coerceAtLeast(0)
            f.text.replace(min(s, e), maxOf(s, e), t)
            return
        }
        web.evaluateJavascript(Js.TYPE.replace("__T__", JSONObject.quote(t)), null)
    }

    private fun onRemote(m: String) {
        val p = m.split(" ", limit = 3)
        try {
            when (p[0]) {
                "m" -> {
                    if (!mouseMode || overlay != null || customView != null) return
                    val dx = p[1].toFloat()
                    val dy = p[2].toFloat()
                    val mag = hypot(dx, dy)
                    val boost = 1f + store.accel / 10f * min(mag / 20f, 2f)
                    val f = 0.6f * store.speed * boost
                    moveBy(dx * f, dy * f, false)
                }
                "c" -> {
                    if (mouseMode && customView == null && overlay == null) click() else pressKey(KeyEvent.KEYCODE_DPAD_CENTER)
                }
                "s" -> web.scrollBy(0, (p[1].toFloat() * store.scrollSpeed * 0.5f).toInt())
                "d" -> pressKey(
                    when (p[1]) {
                        "l" -> KeyEvent.KEYCODE_DPAD_LEFT
                        "r" -> KeyEvent.KEYCODE_DPAD_RIGHT
                        "up" -> KeyEvent.KEYCODE_DPAD_UP
                        else -> KeyEvent.KEYCODE_DPAD_DOWN
                    }
                )
                "k" -> when (p[1]) {
                    "back" -> onBackPressed()
                    "reload" -> web.reload()
                    "full" -> goFullscreen()
                    "ok" -> if (mouseMode && customView == null && overlay == null) click() else pressKey(KeyEvent.KEYCODE_DPAD_CENTER)
                }
                "t" -> typeText(m.substring(2))
                "b" -> keyTo(focusTarget(), KeyEvent.KEYCODE_DEL)
                "e" -> keyTo(focusTarget(), KeyEvent.KEYCODE_ENTER)
                "mode" -> { mouseMode = p[1] == "0"; applyMode() }
            }
        } catch (_: Exception) {}
    }

    // ------------------------------------------------------------ overlays and dialogs
    private fun showOverlay(v: View) {
        overlay?.let { root.removeView(it) }
        overlay = v
        root.addView(v, FrameLayout.LayoutParams(-1, -1))
        column.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        cursor.visibility = View.GONE
        showPrompt(false)
        v.requestFocus()
    }

    private fun closeOverlay() {
        overlay?.let { root.removeView(it) }
        overlay = null
        connectStatus = null
        column.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        applyMode()
    }

    private fun showSettings() {
        val a = SettingsUi.Actions(
            onMode = { mouseMode = store.mouse; applyMode() },
            onUa = { uaMode = store.ua; applyUa(true) },
            onPrefs = { applyPrefs() },
            openSync = { showSync() },
            clearHistory = { store.clearHistory(); toast("ისტორია წაიშალა") },
            clearCookies = { CookieManager.getInstance().removeAllCookies(null); web.clearCache(true); toast("Cookie-ები წაიშალა") }
        )
        showOverlay(SettingsUi.build(this, store, a) { closeOverlay() })
    }

    private fun showSync() {
        showOverlay(SyncUi.build(this, store) { closeOverlay() })
    }

    private fun showBookmarks() {
        val l = store.bookmarks()
        val lv = ListView(this)
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, l.map { it.first + "\n" + it.second })
        val dlg = AlertDialog.Builder(this)
            .setTitle("სანიშნეები (დიდხანს დაჭერა = წაშლა)")
            .setView(lv)
            .setPositiveButton("+ ამ გვერდის დამატება") { _, _ ->
                val u = web.url
                if (u != null && !u.startsWith(START_BASE)) { store.addBookmark(web.title ?: u, u); toast("სანიშნე დაემატა") }
            }
            .setNegativeButton("დახურვა", null)
            .create()
        lv.setOnItemClickListener { _, _, i, _ -> dlg.dismiss(); go(l[i].second) }
        lv.setOnItemLongClickListener { _, _, i, _ -> store.removeBookmark(l[i].second); dlg.dismiss(); toast("წაიშალა"); true }
        dlg.show()
    }

    private fun showHistory() {
        val l = store.history()
        if (l.isEmpty()) { toast("ისტორია ცარიელია"); return }
        val lv = ListView(this)
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, l.map { it.first + "\n" + it.second })
        val dlg = AlertDialog.Builder(this)
            .setTitle("ისტორია")
            .setView(lv)
            .setNeutralButton("გასუფთავება") { _, _ -> store.clearHistory() }
            .setNegativeButton("დახურვა", null)
            .create()
        lv.setOnItemClickListener { _, _, i, _ -> dlg.dismiss(); go(l[i].second) }
        dlg.show()
    }

    override fun onPause() { super.onPause(); web.onPause() }
    override fun onResume() { super.onResume(); web.onResume() }
    override fun onDestroy() {
        server?.stop()
        handler.removeCallbacksAndMessages(null)
        web.destroy()
        super.onDestroy()
    }
}

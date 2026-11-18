package com.example.tvbrowser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import android.webkit.URLUtil
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
import java.io.File
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
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
        const val PICK_FILE = 77
    }

    private lateinit var root: FrameLayout
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
    private var dialog: Dialog? = null
    private var server: RemoteServer? = null
    private var connectStatus: TextView? = null
    private var dismissedUrl: String? = null
    private var pseudoFull = false
    private var promptVisible = false
    private var startShown = false
    private var dAx = 0f
    private var dAy = 0f

    private val hidePrompt = Runnable { showPrompt(false) }
    private val hideBar = Runnable {
        if (!store.autohide || customView != null || overlay != null || dialog != null || urlBox.hasFocus()) return@Runnable
        if (mouseMode && curY <= tbBottom()) { scheduleHide(); return@Runnable }
        if (toolbar.hasFocus()) web.requestFocus()
        toolbar.visibility = View.GONE
    }

    private fun dp(x: Float) = Ui.dp(this, x)
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    private fun tbBottom() = dp(68f).toFloat()

    // ------------------------------------------------------------ UI
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        store = Store(this)
        mouseMode = store.mouse
        uaMode = store.ua

        root = FrameLayout(this).apply { setBackgroundColor(Ui.BG) }
        content = FrameLayout(this)
        web = WebView(this)
        web.setBackgroundColor(Ui.BG)
        cursor = CursorView(this)
        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.rect(this@MainActivity, Ui.SURF, 30f, Ui.LINE, 1f)
            setPadding(dp(8f), dp(6f), dp(8f), dp(6f))
        }

        urlBox = Ui.field(this, "მოძებნე ან ჩაწერე მისამართი").apply {
            px(14f)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, id, ev ->
                if (id == EditorInfo.IME_ACTION_GO || ev?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    go(text.toString()); true
                } else false
            }
        }
        modeBtn = Ui.button(this, "", true, 13f) { mouseMode = !mouseMode; applyMode(); web.requestFocus() }
        uaBtn = Ui.button(this, "", false, 13f) { uaMode = (uaMode + 1) % 3; store.ua = uaMode; applyUa(true); web.requestFocus() }

        fun add(v: View, w: Int = LinearLayout.LayoutParams.WRAP_CONTENT) {
            toolbar.addView(v, LinearLayout.LayoutParams(w, dp(40f)).apply { marginStart = dp(3f); marginEnd = dp(3f) })
        }
        add(Ui.button(this, "◀", false, 15f) { if (web.canGoBack()) web.goBack(); web.requestFocus() }, dp(42f))
        add(Ui.button(this, "▶", false, 15f) { if (web.canGoForward()) web.goForward(); web.requestFocus() }, dp(42f))
        add(Ui.button(this, "⟳", false, 15f) { web.reload(); web.requestFocus() }, dp(42f))
        add(Ui.button(this, "მთავარი", false, 13f) { open(HOME); web.requestFocus() })
        toolbar.addView(urlBox, LinearLayout.LayoutParams(0, dp(40f), 1f).apply { marginStart = dp(3f); marginEnd = dp(3f) })
        add(Ui.button(this, "სანიშნეები", false, 13f) { showBookmarks() })
        add(Ui.button(this, "ისტორია", false, 13f) { showHistory() })
        add(Ui.button(this, "ფაილები", false, 13f) { showFiles() })
        add(Ui.button(this, "ტელეფონი", false, 13f) { showConnect() })
        add(uaBtn)
        add(modeBtn)
        add(Ui.button(this, "პარამეტრები", false, 13f) { showSettings() })

        promptBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            background = Ui.rect(this@MainActivity, Ui.SURF, 28f, Ui.ACC, 2f)
            setPadding(dp(20f), dp(14f), dp(20f), dp(14f))
            addView(TextView(this@MainActivity).apply {
                text = "ვიდეო ნაპოვნია\nგაუშვა სრულ ეკრანზე?"; setTextColor(Ui.TEXT); px(15f)
            })
            addView(Ui.button(this@MainActivity, "▶ სრულ ეკრანზე", true, 15f) { goFullscreen() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(46f)).apply { marginStart = dp(18f) })
            addView(Ui.button(this@MainActivity, "არა", false, 15f) { dismissPrompt() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(46f)).apply { marginStart = dp(8f) })
        }

        content.addView(web, FrameLayout.LayoutParams(-1, -1))
        content.addView(promptBox, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(28f), dp(28f)) })
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        root.addView(toolbar, FrameLayout.LayoutParams(-1, -2).apply { setMargins(dp(14f), dp(10f), dp(14f), 0) })
        root.addView(cursor, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        setupWeb()
        applyUa(false)
        applyMode()
        applyPrefs()
        root.post {
            curX = root.width / 2f
            curY = root.height / 2f
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
            allowFileAccess = true
            allowContentAccess = true
        }
        web.addJavascriptInterface(Bridge(), "TVB")
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.setDownloadListener { url, ua, cd, mime, _ -> startDownload(url, ua, cd, mime) }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val s = r.url.scheme ?: return false
                if (s == "tvb") {
                    if (r.url.host == "add") showAddSite()
                    return true
                }
                return s !in listOf("http", "https", "about", "data", "blob", "file", "content")
            }
            override fun onPageStarted(v: WebView, url: String, favicon: Bitmap?) {
                dismissedUrl = null
                pseudoFull = false
                startShown = false
                showPrompt(false)
            }
            override fun onPageFinished(v: WebView, url: String) {
                if (url.startsWith(START_BASE)) {
                    startShown = true
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
                content.visibility = View.GONE
                toolbar.visibility = View.GONE
                cursor.visibility = View.GONE
                showPrompt(false)
            }
            override fun onHideCustomView() {
                customView?.let { root.removeView(it) }
                customView = null
                customCb?.onCustomViewHidden()
                customCb = null
                content.visibility = View.VISIBLE
                toolbar.visibility = if (store.autohide) View.GONE else View.VISIBLE
                applyMode()
            }
        }
        chrome = cc
        web.webChromeClient = cc
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun startHtml(): String {
        val tiles = store.bookmarks().take(11).joinToString("") { (t, u) ->
            val l = (t.trim().firstOrNull() ?: '•').uppercaseChar()
            "<a class=t href=\"${esc(u)}\"><i>$l</i>${esc(t.take(22))}</a>"
        }
        return """<!doctype html><html lang="ka"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><style>
body{margin:0;min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4vw;background:#0E1116;color:#F2F4F8;font-family:sans-serif}
h1{margin:0;font-size:3.2vw}
form{width:50vw}
input{width:100%;box-sizing:border-box;height:5vw;border-radius:2.5vw;border:.15vw solid #262C36;background:#171B22;color:#F2F4F8;padding:0 2.2vw;font-size:1.8vw}
input:focus{outline:.25vw solid #6EA8FF}
.g{display:flex;flex-wrap:wrap;gap:1.6vw;justify-content:center;max-width:80vw}
.t{width:14vw;height:12vw;box-sizing:border-box;border-radius:2vw;background:#171B22;border:.15vw solid #262C36;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:1.1vw;color:#F2F4F8;text-decoration:none;font-size:1.6vw;font-weight:600}
.t:focus{outline:.3vw solid #6EA8FF;background:#222833}
.t i{width:5.4vw;height:5.4vw;border-radius:50%;background:#243049;color:#6EA8FF;display:flex;align-items:center;justify-content:center;font-style:normal;font-size:2.4vw;font-weight:700}
.add{border-style:dashed;color:#9AA4B2}.add i{background:transparent;border:.15vw dashed #3A4352;color:#9AA4B2}
</style></head><body><h1>რა გინდა ნახო დღეს?</h1>
<form action="https://www.google.com/search" method="get"><input name="q" placeholder="ძებნა ან მისამართი" autocomplete="off"></form>
<div class="g">$tiles<a class="t add" href="tvb://add"><i>+</i>დამატება</a></div></body></html>"""
    }

    private fun open(url: String) {
        if (url == HOME) {
            urlBox.setText("")
            web.loadDataWithBaseURL(START_BASE, startHtml(), "text/html", "utf-8", null)
        } else {
            go(url)
        }
    }

    private fun normUrl(input: String): String {
        val s = input.trim()
        return when {
            s.startsWith("http://") || s.startsWith("https://") || s.startsWith("file:") -> s
            s.contains(" ") || !s.contains(".") -> "https://www.google.com/search?q=" + Uri.encode(s)
            else -> "https://$s"
        }
    }

    private fun go(input: String) {
        if (input.isBlank()) return
        web.loadUrl(normUrl(input))
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
        if (overlay == null && dialog == null) web.requestFocus()
    }

    private fun applyPrefs() {
        cursor.style(store.cursorSize, CURSOR_COLORS[store.cursorColor.coerceIn(0, CURSOR_COLORS.size - 1)])
        web.settings.textZoom = store.zoom
        val lp = content.layoutParams as FrameLayout.LayoutParams
        lp.topMargin = if (store.autohide) 0 else tbBottom().toInt()
        content.layoutParams = lp
        if (!store.autohide) toolbar.visibility = View.VISIBLE
        scheduleHide()
    }

    private fun scheduleHide() {
        handler.removeCallbacks(hideBar)
        if (store.autohide) handler.postDelayed(hideBar, 5000)
    }

    private fun showBar(focus: Boolean) {
        if (customView != null) return
        toolbar.visibility = View.VISIBLE
        scheduleHide()
        if (focus) toolbar.post { toolbar.getChildAt(0).requestFocus() }
    }

    // ------------------------------------------------------------ downloads and files
    private fun downloadDir(): File {
        val d = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(filesDir, "Download")
        d.mkdirs()
        return d
    }

    private fun startDownload(url: String, ua: String?, cd: String?, mime: String?) {
        if (!url.startsWith("http")) { toast("ამ ტიპის ჩამოტვირთვა ვერ ხერხდება"); return }
        val name = URLUtil.guessFileName(url, cd, mime)
        toast("ჩამოტვირთვა დაიწყო: $name")
        Thread {
            try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.setRequestProperty("User-Agent", ua ?: UAS[0])
                CookieManager.getInstance().getCookie(url)?.let { c.setRequestProperty("Cookie", it) }
                c.instanceFollowRedirects = true
                c.connectTimeout = 15000
                c.readTimeout = 30000
                var f = File(downloadDir(), name)
                var i = 1
                while (f.exists()) { f = File(downloadDir(), "($i) $name"); i++ }
                c.inputStream.use { inp -> f.outputStream().use { out -> inp.copyTo(out) } }
                val fn = f.name
                runOnUiThread { toast("ჩამოტვირთულია: $fn") }
            } catch (e: Exception) {
                runOnUiThread { toast("ჩამოტვირთვა ვერ მოხერხდა") }
            }
        }.start()
    }

    private fun showFiles() {
        showOverlay(FilesUi.build(this, downloadDir(), { f -> openFile(f) }, { pickFile() }, { closeOverlay() }))
    }

    private fun openFile(f: File) {
        val ext = f.extension.lowercase()
        val ok = ext in listOf("html", "htm", "txt", "jpg", "jpeg", "png", "gif", "webp", "svg",
            "mp4", "webm", "m4v", "mov", "mkv", "mp3", "m4a", "ogg", "wav")
        if (!ok) { toast("ამ ტიპის ფაილის გახსნა ვერ ხერხდება (.$ext)"); return }
        closeOverlay()
        web.loadUrl(Uri.fromFile(f).toString())
        web.requestFocus()
    }

    private fun pickFile() {
        try {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(i, PICK_FILE)
        } catch (e: Exception) {
            toast("ამ მოწყობილობაზე ფაილის არჩევა ვერ ხერხდება")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == PICK_FILE && res == RESULT_OK) {
            val u = data?.data ?: return
            closeOverlay()
            web.loadUrl(u.toString())
            web.requestFocus()
        }
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
        val ev = MotionEvent.obtain(t, t, MotionEvent.ACTION_HOVER_MOVE, curX - content.left, curY - content.top, 0)
        ev.source = InputDevice.SOURCE_MOUSE
        web.dispatchGenericMotionEvent(ev)
        ev.recycle()
    }

    private fun moveBy(dx: Float, dy: Float, toolbarOnTop: Boolean) {
        val w = root.width.toFloat()
        val h = root.height.toFloat()
        val edge = 2f * resources.displayMetrics.density
        if (dy > 0 && curY >= h - 1 - edge) web.scrollBy(0, (abs(dy) * 1.5f).toInt())
        if (dy < 0 && curY <= edge) {
            if (web.scrollY > 0) web.scrollBy(0, (-abs(dy) * 1.5f).toInt())
            else if (toolbarOnTop) { showBar(true); return }
        }
        curX = (curX + dx).coerceIn(0f, w - 1)
        curY = (curY + dy).coerceIn(0f, h - 1)
        updateCursor()
        hover()
        if (store.autohide) { if (curY <= tbBottom()) showBar(false) else scheduleHide() }
    }

    private fun hit(v: View, x: Float, y: Float): Boolean {
        val rl = IntArray(2)
        root.getLocationOnScreen(rl)
        val l = IntArray(2)
        v.getLocationOnScreen(l)
        val sx = rl[0] + x
        val sy = rl[1] + y
        return sx >= l[0] && sx <= l[0] + v.width && sy >= l[1] && sy <= l[1] + v.height
    }

    private fun promptButtonAt(x: Float, y: Float): Button? {
        if (!promptVisible) return null
        for (i in 0 until promptBox.childCount) {
            val v = promptBox.getChildAt(i)
            if (v is Button && hit(v, x, y)) return v
        }
        return null
    }

    private fun toolbarViewAt(x: Float, y: Float): View? {
        if (toolbar.visibility != View.VISIBLE) return null
        for (i in 0 until toolbar.childCount) {
            val v = toolbar.getChildAt(i)
            if (hit(v, x, y)) return v
        }
        return null
    }

    private fun click() {
        val b = promptButtonAt(curX, curY)
        if (b != null) { b.performClick(); return }
        val tv = toolbarViewAt(curX, curY)
        if (tv != null) {
            if (tv is EditText) { showBar(false); tv.requestFocus() } else tv.performClick()
            return
        }
        touchAt(curX - content.left, curY - content.top)
    }

    private fun pressKey(code: Int) {
        val t = SystemClock.uptimeMillis()
        val down = KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0)
        val up = KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0)
        val d = dialog
        if (d != null) { d.dispatchKeyEvent(down); d.dispatchKeyEvent(up) }
        else { dispatchKeyEvent(down); dispatchKeyEvent(up) }
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
                    showBar(true)
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

    private fun focusOwner(): View? = dialog?.currentFocus ?: currentFocus

    private fun keyTo(v: View, code: Int) {
        val t = SystemClock.uptimeMillis()
        v.dispatchKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0))
        v.dispatchKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0))
    }

    private fun typeText(t: String) {
        val f = focusOwner()
        if (f is EditText) {
            val s = f.selectionStart.coerceAtLeast(0)
            val e = f.selectionEnd.coerceAtLeast(0)
            f.text.replace(min(s, e), maxOf(s, e), t)
            return
        }
        web.evaluateJavascript(Js.TYPE.replace("__T__", JSONObject.quote(t)), null)
    }

    private fun plainMouse() = mouseMode && customView == null && overlay == null && dialog == null

    private fun onRemote(m: String) {
        val p = m.split(" ", limit = 3)
        try {
            when (p[0]) {
                "m" -> {
                    val dx = p[1].toFloat()
                    val dy = p[2].toFloat()
                    if (overlay != null || dialog != null) {
                        // menus: swipes act like the D-pad
                        dAx += dx
                        dAy += dy
                        if (abs(dAy) > 45f) {
                            pressKey(if (dAy > 0) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP)
                            dAx = 0f; dAy = 0f
                        } else if (abs(dAx) > 45f) {
                            pressKey(if (dAx > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT)
                            dAx = 0f; dAy = 0f
                        }
                        return
                    }
                    if (!mouseMode || customView != null) return
                    val mag = hypot(dx, dy)
                    val boost = 1f + store.accel / 10f * min(mag / 20f, 2f)
                    val f = 0.6f * store.speed * boost
                    moveBy(dx * f, dy * f, false)
                }
                "c" -> if (plainMouse()) click() else pressKey(KeyEvent.KEYCODE_DPAD_CENTER)
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
                    "ok" -> if (plainMouse()) click() else pressKey(KeyEvent.KEYCODE_DPAD_CENTER)
                }
                "a" -> when (p[1]) {
                    "back" -> onBackPressed()
                    "fwd" -> if (web.canGoForward()) web.goForward()
                    "reload" -> web.reload()
                    "home" -> open(HOME)
                    "bm" -> showBookmarks()
                    "hist" -> showHistory()
                    "url" -> { showBar(false); urlBox.requestFocus() }
                    "set" -> showSettings()
                    "full" -> goFullscreen()
                }
                "t" -> typeText(m.substring(2))
                "b" -> keyTo(focusOwner() ?: web, KeyEvent.KEYCODE_DEL)
                "e" -> keyTo(focusOwner() ?: web, KeyEvent.KEYCODE_ENTER)
                "mode" -> { mouseMode = p[1] == "0"; applyMode() }
            }
        } catch (_: Exception) {}
    }

    // ------------------------------------------------------------ overlays and dialogs
    private fun blockFocus(b: Boolean) {
        val m = if (b) ViewGroup.FOCUS_BLOCK_DESCENDANTS else ViewGroup.FOCUS_BEFORE_DESCENDANTS
        content.descendantFocusability = m
        toolbar.descendantFocusability = m
    }

    private fun showOverlay(v: View) {
        overlay?.let { root.removeView(it) }
        overlay = v
        root.addView(v, FrameLayout.LayoutParams(-1, -1))
        blockFocus(true)
        cursor.visibility = View.GONE
        showPrompt(false)
        v.requestFocus()
    }

    private fun closeOverlay() {
        overlay?.let { root.removeView(it) }
        overlay = null
        connectStatus = null
        blockFocus(false)
        applyMode()
    }

    private fun track(d: AlertDialog) {
        dialog = d
        d.setOnDismissListener { if (dialog === d) dialog = null }
        d.show()
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

    private fun showAddSite() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22f), dp(12f), dp(22f), 0)
        }
        val n = Ui.field(this, "სახელი (არასავალდებულო)").apply { px(15f) }
        val u = Ui.field(this, "მისამართი, მაგ. youtube.com").apply { px(15f) }
        box.addView(n, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44f)))
        box.addView(u, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44f)).apply { topMargin = dp(10f) })
        val dlg = AlertDialog.Builder(this)
            .setTitle("საიტის დამატება")
            .setView(box)
            .setPositiveButton("დამატება") { _, _ -> addSite(n.text.toString(), u.text.toString()) }
            .setNegativeButton("გაუქმება", null)
            .create()
        track(dlg)
        u.requestFocus()
    }

    private fun addSite(name: String, address: String) {
        if (address.isBlank()) { toast("მისამართი ცარიელია"); return }
        val url = normUrl(address)
        val title = name.ifBlank { Uri.parse(url).host?.removePrefix("www.") ?: url }
        store.addBookmark(title, url)
        toast("დაემატა: $title")
        if (startShown) open(HOME)
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
                if (u != null && !u.startsWith(START_BASE)) {
                    store.addBookmark(web.title ?: u, u); toast("სანიშნე დაემატა")
                }
            }
            .setNeutralButton("+ ხელით") { _, _ -> showAddSite() }
            .setNegativeButton("დახურვა", null)
            .create()
        lv.setOnItemClickListener { _, _, i, _ -> dlg.dismiss(); go(l[i].second) }
        lv.setOnItemLongClickListener { _, _, i, _ ->
            store.removeBookmark(l[i].second); dlg.dismiss(); toast("წაიშალა")
            if (startShown) open(HOME)
            true
        }
        track(dlg)
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
        track(dlg)
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

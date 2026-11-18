package com.example.tvbrowser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
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
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
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
    private lateinit var modeBtn: ImageButton
    private lateinit var uaBtn: ImageButton
    private var overlayScroll: ScrollView? = null
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
        if (!autoHide() || customView != null || overlay != null || dialog != null || urlBox.hasFocus()) return@Runnable
        if (mouseMode && curY <= tbBottom()) { scheduleHide(); return@Runnable }
        if (toolbar.hasFocus()) web.requestFocus()
        toolbar.visibility = View.GONE
    }

    private fun autoHide() = store.autohide && mouseMode
    private fun dp(x: Float) = Ui.dp(this, x)
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    private fun tbBottom() = dp(76f).toFloat()

    // ------------------------------------------------------------ UI
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        store = Store(this)
        if (store.lastVersion != BuildConfig.VERSION_CODE) {
            store.lastVersion = BuildConfig.VERSION_CODE
            store.pendingTag?.let { store.updateTag = it; store.pendingTag = null }
        }
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
        modeBtn = Ui.icon(this, R.drawable.ic_mouse, "რეჟიმი", true) { mouseMode = !mouseMode; applyMode(); web.requestFocus() }
        uaBtn = Ui.icon(this, R.drawable.ic_desktop, "საიტის ვერსია") { uaMode = (uaMode + 1) % 3; store.ua = uaMode; applyUa(true); web.requestFocus() }

        fun add(v: View, w: Int = dp(48f)) {
            toolbar.addView(v, LinearLayout.LayoutParams(w, dp(48f)).apply { marginStart = dp(3f); marginEnd = dp(3f) })
        }
        add(Ui.icon(this, R.drawable.ic_back, "უკან") { if (web.canGoBack()) web.goBack(); web.requestFocus() })
        add(Ui.icon(this, R.drawable.ic_forward, "წინ") { if (web.canGoForward()) web.goForward(); web.requestFocus() })
        add(Ui.icon(this, R.drawable.ic_reload, "განახლება") { web.reload(); web.requestFocus() })
        add(Ui.icon(this, R.drawable.ic_home, "მთავარი") { open(HOME); web.requestFocus() })
        toolbar.addView(urlBox, LinearLayout.LayoutParams(0, dp(48f), 1f).apply { marginStart = dp(3f); marginEnd = dp(3f) })
        add(Ui.icon(this, R.drawable.ic_bookmark, "სანიშნეები") { showBookmarks() })
        add(Ui.icon(this, R.drawable.ic_history, "ისტორია") { showHistory() })
        add(Ui.icon(this, R.drawable.ic_folder, "ფაილები") { showFiles() })
        add(Ui.icon(this, R.drawable.ic_phone, "ტელეფონი") { showConnect() })
        add(uaBtn)
        add(modeBtn)
        add(Ui.icon(this, R.drawable.ic_settings, "პარამეტრები") { showSettings() })

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
                toolbar.visibility = if (autoHide()) View.GONE else View.VISIBLE
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
        uaBtn.setImageResource(listOf(R.drawable.ic_desktop, R.drawable.ic_tablet, R.drawable.ic_mobile)[uaMode])
        if (reload) web.reload()
    }

    private fun applyMode() {
        cursor.visibility = if (mouseMode && customView == null) View.VISIBLE else View.GONE
        modeBtn.setImageResource(if (mouseMode) R.drawable.ic_mouse else R.drawable.ic_dpad)
        store.mouse = mouseMode
        applyPrefs()
        if (overlay == null && dialog == null) web.requestFocus()
    }

    private fun applyPrefs() {
        cursor.style(store.cursorSize, CURSOR_COLORS[store.cursorColor.coerceIn(0, CURSOR_COLORS.size - 1)])
        web.settings.textZoom = store.zoom
        val lp = content.layoutParams as FrameLayout.LayoutParams
        lp.topMargin = if (autoHide()) 0 else tbBottom().toInt()
        content.layoutParams = lp
        if (!autoHide()) toolbar.visibility = View.VISIBLE
        scheduleHide()
    }

    private fun scheduleHide() {
        handler.removeCallbacks(hideBar)
        if (autoHide()) handler.postDelayed(hideBar, 5000)
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
        val sv = overlayScroll
        val amt = (abs(dy) * 1.5f).toInt()
        if (dy > 0 && curY >= h - 1 - edge) { if (sv != null) sv.scrollBy(0, amt) else web.scrollBy(0, amt) }
        if (dy < 0 && curY <= edge) {
            val sy = if (sv != null) sv.scrollY else web.scrollY
            if (sy > 0) { if (sv != null) sv.scrollBy(0, -amt) else web.scrollBy(0, -amt) }
            else if (toolbarOnTop && overlay == null) { showBar(true); return }
        }
        curX = (curX + dx).coerceIn(0f, w - 1)
        curY = (curY + dy).coerceIn(0f, h - 1)
        updateCursor()
        hover()
        if (autoHide() && overlay == null) { if (curY <= tbBottom()) showBar(false) else scheduleHide() }
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

    private fun touchOn(v: View, x: Float, y: Float) {
        val t = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0)
        v.dispatchTouchEvent(down)
        down.recycle()
        v.postDelayed({
            val up = MotionEvent.obtain(t, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0)
            v.dispatchTouchEvent(up)
            up.recycle()
        }, 60)
    }

    private fun click() {
        val ov = overlay
        if (ov != null) { touchOn(ov, curX, curY); return }
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
        val k = e.keyCode
        if (k == KeyEvent.KEYCODE_MENU && e.action == KeyEvent.ACTION_DOWN && overlay == null) { showBar(true); return true }
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
                    if (overlay == null) showBar(true)
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
                    if (!mouseMode || customView != null) return
                    val mag = hypot(dx, dy)
                    val boost = 1f + store.accel / 10f * min(mag / 20f, 2f)
                    val f = 0.6f * store.speed * boost
                    moveBy(dx * f, dy * f, false)
                }
                "c" -> if (mouseMode && customView == null) click() else pressKey(KeyEvent.KEYCODE_DPAD_CENTER)
                "s" -> { val sd = (p[1].toFloat() * store.scrollSpeed * 0.5f).toInt(); val sv = overlayScroll; if (sv != null) sv.scrollBy(0, sd) else web.scrollBy(0, sd) }
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
                    "ok" -> if (mouseMode && customView == null) click() else pressKey(KeyEvent.KEYCODE_DPAD_CENTER)
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
                    "files" -> showFiles()
                    "ua" -> { uaMode = (uaMode + 1) % 3; store.ua = uaMode; applyUa(true) }
                    "full" -> goFullscreen()
                }
                "v" -> volume(p[1])
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

    private fun findScroll(v: View): ScrollView? {
        if (v is ScrollView) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) { val r = findScroll(v.getChildAt(i)); if (r != null) return r }
        return null
    }

    private fun showOverlay(v: View) {
        overlay?.let { root.removeView(it) }
        overlay = v
        root.addView(v, FrameLayout.LayoutParams(-1, -1))
        blockFocus(true)
        overlayScroll = findScroll(v)
        cursor.bringToFront()
        showPrompt(false)
        v.requestFocus()
    }

    private fun closeOverlay() {
        overlay?.let { root.removeView(it) }
        overlay = null
        overlayScroll = null
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
            clearCookies = { CookieManager.getInstance().removeAllCookies(null); web.clearCache(true); toast("Cookie-ები წაიშალა") },
            onUpdate = { checkUpdate() }
        )
        showOverlay(SettingsUi.build(this, store, a) { closeOverlay() })
    }

    private fun showSync() {
        showOverlay(SyncUi.build(this, store) { closeOverlay() })
    }

    private fun showAddSite() {
        showOverlay(AddSiteUi.build(this, { n, u -> addSite(n, u) }) { closeOverlay() })
    }

    private fun addSite(name: String, address: String) {
        if (address.isBlank()) { toast("მისამართი ცარიელია"); return }
        val url = normUrl(address)
        val title = name.ifBlank { Uri.parse(url).host?.removePrefix("www.") ?: url }
        store.addBookmark(title, url)
        toast("დაემატა: $title")
        closeOverlay()
        if (startShown) open(HOME)
    }

    private fun showBookmarks() {
        showOverlay(ListUi.build(this, "სანიშნეები", store.bookmarks(),
            { u -> closeOverlay(); go(u) },
            { u -> store.removeBookmark(u); showBookmarks(); if (startShown) open(HOME) },
            listOf(
                "ამ გვერდის დამატება" to {
                    val u = web.url
                    if (u != null && !u.startsWith(START_BASE)) {
                        store.addBookmark(web.title ?: u, u); toast("სანიშნე დაემატა"); showBookmarks()
                    }
                },
                "ახალი საიტი" to { showAddSite() }
            )) { closeOverlay() })
    }

    private fun showHistory() {
        showOverlay(ListUi.build(this, "ისტორია", store.history(),
            { u -> closeOverlay(); go(u) }, null,
            listOf("გასუფთავება" to { store.clearHistory(); showHistory() })) { closeOverlay() })
    }

    // ------------------------------------------------------------ volume and updates
    private fun volume(a: String) {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        try {
            when (a) {
                "up" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                "dn" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                else -> if (Build.VERSION.SDK_INT >= 23) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                }
            }
        } catch (_: Exception) {}
    }

    private fun checkUpdate() {
        val url = store.updateUrl
        if (url.isBlank()) { toast("განახლების ბმული არ არის მითითებული"); return }
        toast("მოწმდება…")
        Thread {
            try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.instanceFollowRedirects = true
                c.connectTimeout = 15000
                c.readTimeout = 60000
                if (c.responseCode != 200) {
                    val code = c.responseCode
                    runOnUiThread { toast("განახლება ვერ მოიძებნა ($code)") }
                    return@Thread
                }
                val tag = c.getHeaderField("ETag") ?: c.getHeaderField("Last-Modified") ?: c.contentLength.toString()
                val f = File(downloadDir(), "update.apk")
                if (tag == store.updateTag) {
                    c.disconnect()
                    runOnUiThread { toast("უახლესი ვერსიაა") }
                    return@Thread
                }
                if (!(tag == store.pendingTag && f.exists())) {
                    runOnUiThread { toast("იტვირთება ახალი ვერსია…") }
                    c.inputStream.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                    store.pendingTag = tag
                } else {
                    c.disconnect()
                }
                runOnUiThread { installApk(f) }
            } catch (e: Exception) {
                runOnUiThread { toast("განახლება ვერ მოხერხდა") }
            }
        }.start()
    }

    private fun installApk(f: File) {
        try {
            if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
                toast("ჩართე უცნობი აპების დაყენება ამ აპისთვის და ისევ დააჭირე განახლებას")
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                return
            }
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
            val i = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            toast("ინსტალაცია ვერ გაიხსნა")
        }
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

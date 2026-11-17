package com.example.tvbrowser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import android.widget.Toast

class MainActivity : Activity() {

    private val UAS = listOf(
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; Pixel Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    )
    private val UA_NAMES = listOf("Desktop", "Tablet", "Phone")

    private lateinit var root: FrameLayout
    private lateinit var column: LinearLayout
    private lateinit var content: FrameLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var web: WebView
    private lateinit var cursor: CursorView
    private lateinit var urlBox: EditText
    private lateinit var modeBtn: Button
    private lateinit var uaBtn: Button
    private lateinit var store: Store

    private var mouseMode = true
    private var uaMode = 0
    private var curX = 0f
    private var curY = 0f
    private var longPressed = false
    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null

    private fun btn(t: String, f: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; textSize = 14f
        minWidth = 0; minimumWidth = 0
        setOnClickListener { f() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        store = Store(this)
        mouseMode = store.mouse
        uaMode = store.ua

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF202124.toInt())
        }
        content = FrameLayout(this)
        web = WebView(this)
        cursor = CursorView(this)
        content.addView(web, FrameLayout.LayoutParams(-1, -1))
        content.addView(cursor, FrameLayout.LayoutParams(-1, -1))

        urlBox = EditText(this).apply {
            setSingleLine(true)
            hint = "მისამართი ან ძებნა"
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setSelectAllOnFocus(true)
            setOnEditorActionListener { _, id, ev ->
                if (id == EditorInfo.IME_ACTION_GO || ev?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    go(text.toString()); true
                } else false
            }
        }
        modeBtn = btn("") { mouseMode = !mouseMode; applyMode() }
        uaBtn = btn("") { uaMode = (uaMode + 1) % 3; applyUa(true); web.requestFocus() }

        toolbar.addView(btn("◀") { if (web.canGoBack()) web.goBack(); web.requestFocus() })
        toolbar.addView(btn("▶") { if (web.canGoForward()) web.goForward(); web.requestFocus() })
        toolbar.addView(btn("⟳") { web.reload(); web.requestFocus() })
        toolbar.addView(urlBox, LinearLayout.LayoutParams(0, -2, 1f))
        toolbar.addView(btn("★+") {
            val u = web.url
            if (u != null) { store.addBookmark(web.title ?: u, u); toast("სანიშნე დაემატა") }
        })
        toolbar.addView(btn("სანიშნეები") { showBookmarks() })
        toolbar.addView(btn("ისტორია") { showHistory() })
        toolbar.addView(uaBtn)
        toolbar.addView(modeBtn)

        column.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
        column.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        setupWeb()
        applyUa(false)
        applyMode()
        content.post {
            curX = content.width / 2f; curY = content.height / 2f
            updateCursor()
        }
        web.loadUrl(store.lastUrl)
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
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val s = r.url.scheme ?: return false
                return s !in listOf("http", "https", "about", "data", "blob", "file")
            }
            override fun onPageFinished(v: WebView, url: String) {
                if (!urlBox.hasFocus()) urlBox.setText(url)
                store.addHistory(v.title ?: url, url)
                store.lastUrl = url
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, cb: CustomViewCallback) {
                customView = view; customCb = cb
                root.addView(view, FrameLayout.LayoutParams(-1, -1))
                column.visibility = View.GONE
            }
            override fun onHideCustomView() {
                customView?.let { root.removeView(it) }
                customView = null
                customCb?.onCustomViewHidden(); customCb = null
                column.visibility = View.VISIBLE
                web.requestFocus()
            }
        }
    }

    private fun applyUa(reload: Boolean) {
        web.settings.apply {
            userAgentString = UAS[uaMode]
            useWideViewPort = uaMode == 0
            loadWithOverviewMode = uaMode == 0
        }
        uaBtn.text = UA_NAMES[uaMode]
        store.ua = uaMode
        if (reload) web.reload()
    }

    private fun applyMode() {
        cursor.visibility = if (mouseMode) View.VISIBLE else View.GONE
        modeBtn.text = if (mouseMode) "რეჟიმი: მაუსი" else "რეჟიმი: ღილაკები"
        store.mouse = mouseMode
        web.requestFocus()
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

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun showList(
        title: String,
        items: List<Pair<String, String>>,
        onDelete: ((String) -> Unit)?,
        onClear: (() -> Unit)?
    ) {
        val lv = ListView(this)
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            items.map { "${it.first}\n${it.second}" })
        val b = AlertDialog.Builder(this).setTitle(title).setView(lv)
            .setNegativeButton("დახურვა", null)
        if (onClear != null) b.setNeutralButton("გასუფთავება") { _, _ -> onClear() }
        val dlg = b.create()
        lv.setOnItemClickListener { _, _, i, _ -> dlg.dismiss(); web.loadUrl(items[i].second); web.requestFocus() }
        if (onDelete != null) lv.setOnItemLongClickListener { _, _, i, _ ->
            onDelete(items[i].second); dlg.dismiss(); toast("წაიშალა"); true
        }
        dlg.show()
    }

    private fun showBookmarks() {
        val l = store.bookmarks()
        if (l.isEmpty()) { toast("სანიშნეები ცარიელია"); return }
        showList("სანიშნეები (დიდხანს დაჭერა = წაშლა)", l, { store.removeBookmark(it) }, null)
    }

    private fun showHistory() {
        val l = store.history()
        if (l.isEmpty()) { toast("ისტორია ცარიელია"); return }
        showList("ისტორია", l, null) { store.clearHistory() }
    }

    // ---------- mouse mode ----------
    private fun updateCursor() { cursor.cx = curX; cursor.cy = curY; cursor.invalidate() }

    private fun hover() {
        val t = SystemClock.uptimeMillis()
        val ev = MotionEvent.obtain(t, t, MotionEvent.ACTION_HOVER_MOVE, curX, curY, 0)
        ev.source = InputDevice.SOURCE_MOUSE
        web.dispatchGenericMotionEvent(ev)
        ev.recycle()
    }

    private fun tap() {
        val t = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, curX, curY, 0)
        web.dispatchTouchEvent(down); down.recycle()
        web.postDelayed({
            val up = MotionEvent.obtain(t, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, curX, curY, 0)
            web.dispatchTouchEvent(up); up.recycle()
        }, 60)
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (customView != null || !mouseMode || toolbar.hasFocus()) return super.dispatchKeyEvent(e)
        val d = resources.displayMetrics.density
        when (e.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (e.action == KeyEvent.ACTION_DOWN) {
                    val step = (10 + minOf(e.repeatCount, 25) * 2.5f) * d
                    var dx = 0f; var dy = 0f
                    when (e.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> dy = -step
                        KeyEvent.KEYCODE_DPAD_DOWN -> dy = step
                        KeyEvent.KEYCODE_DPAD_LEFT -> dx = -step
                        else -> dx = step
                    }
                    val w = content.width.toFloat(); val h = content.height.toFloat()
                    if (dy > 0 && curY >= h - 1) web.scrollBy(0, (step * 1.5f).toInt())
                    if (dy < 0 && curY <= 0f) {
                        if (web.scrollY > 0) web.scrollBy(0, (-step * 1.5f).toInt())
                        else { toolbar.getChildAt(0).requestFocus(); return true }
                    }
                    curX = (curX + dx).coerceIn(0f, w - 1)
                    curY = (curY + dy).coerceIn(0f, h - 1)
                    updateCursor(); hover()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (e.action == KeyEvent.ACTION_DOWN && e.isLongPress) {
                    longPressed = true
                    toolbar.getChildAt(0).requestFocus()
                } else if (e.action == KeyEvent.ACTION_UP) {
                    if (!longPressed) tap()
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
            customView != null -> (web.webChromeClient as WebChromeClient).onHideCustomView()
            web.canGoBack() -> web.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onPause() { super.onPause(); web.onPause() }
    override fun onResume() { super.onResume(); web.onResume() }
    override fun onDestroy() { web.destroy(); super.onDestroy() }
}

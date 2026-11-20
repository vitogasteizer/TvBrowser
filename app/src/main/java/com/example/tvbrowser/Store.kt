package com.example.tvbrowser

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import org.json.JSONArray
import org.json.JSONObject

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("tvb", Context.MODE_PRIVATE)
    private val isTv = (ctx.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)
        ?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

    private fun load(k: String): MutableList<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val a = JSONArray(p.getString(k, "[]"))
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out.add(o.getString("t") to o.getString("u"))
        }
        return out
    }

    private fun save(k: String, l: List<Pair<String, String>>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("t", it.first).put("u", it.second)) }
        p.edit().putString(k, a.toString()).apply()
    }

    private fun parse(json: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        try {
            val a = JSONArray(json)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(o.getString("t") to o.getString("u"))
            }
        } catch (e: Exception) {}
        return out
    }

    private fun merge(k: String, json: String, cap: Int) {
        val l = load(k)
        for (e in parse(json)) if (l.none { it.second == e.second }) l.add(e)
        save(k, l.take(cap))
    }

    fun bookmarksJson(): String = p.getString("b", "[]") ?: "[]"
    fun historyJson(): String = p.getString("h", "[]") ?: "[]"
    fun mergeBookmarks(json: String) = merge("b", json, 1000)
    fun mergeHistory(json: String) = merge("h", json, 200)

    fun bookmarks() = load("b")
    fun history() = load("h")

    fun addBookmark(t: String, u: String) {
        val l = load("b")
        if (l.none { it.second == u }) { l.add(0, t to u); save("b", l) }
    }

    fun removeBookmark(u: String) { save("b", load("b").filter { it.second != u }) }

    fun addHistory(t: String, u: String) {
        if (u.startsWith("about:") || u.startsWith("data:") || u.startsWith("file:") || u.startsWith("content:")) return
        val l = load("h")
        l.removeAll { it.second == u }
        l.add(0, t to u)
        save("h", l.take(200))
    }

    fun clearHistory() { p.edit().remove("h").apply() }

    private fun gi(k: String, d: Int) = p.getInt(k, d)
    private fun si(k: String, v: Int) { p.edit().putInt(k, v).apply() }

    var mouse: Boolean
        get() = p.getBoolean("mouse", true)
        set(v) { p.edit().putBoolean("mouse", v).apply() }
    var autohide: Boolean
        get() = p.getBoolean("autohide", true)
        set(v) { p.edit().putBoolean("autohide", v).apply() }
    var ua: Int
        get() = gi("ua", 0)
        set(v) { si("ua", v) }
    var speed: Int
        get() = gi("speed", 6)
        set(v) { si("speed", v) }
    var accel: Int
        get() = gi("accel", 4)
        set(v) { si("accel", v) }
    var scrollSpeed: Int
        get() = gi("scroll", 5)
        set(v) { si("scroll", v) }
    var cursorSize: Int
        get() = gi("csize", 1)
        set(v) { si("csize", v) }
    var cursorColor: Int
        get() = gi("ccolor", 0)
        set(v) { si("ccolor", v) }
    var zoom: Int
        get() = gi("zoom", 100)
        set(v) { si("zoom", v) }
    var home: String
        get() = p.getString("home", "tvb:home") ?: "tvb:home"
        set(v) { p.edit().putString("home", v).apply() }

    var syncId: String?
        get() = p.getString("syncid", null)
        set(v) { if (v == null) p.edit().remove("syncid").apply() else p.edit().putString("syncid", v).apply() }
    /** Safe margin on every side, in tenths of a percent of the screen (TV overscan). */
    var overscan10: Int
        get() = gi("overscan10", if (isTv) 20 else 0)
        set(v) { si("overscan10", v) }
    var lang: String
        get() = p.getString("lang", "") ?: ""
        set(v) { p.edit().putString("lang", v).apply() }

    var updateUrl: String
        get() = p.getString("uurl", null) ?: BuildConfig.UPDATE_URL
        set(v) { p.edit().putString("uurl", v).apply() }
    var updateTag: String?
        get() = p.getString("utag", null)
        set(v) { p.edit().putString("utag", v).apply() }
    var pendingTag: String?
        get() = p.getString("ptag", null)
        set(v) { p.edit().putString("ptag", v).apply() }
    var lastVersion: Int
        get() = gi("lastv", 0)
        set(v) { si("lastv", v) }

    val deviceId: String
        get() {
            val cur = p.getString("id", null)
            if (cur != null) return cur
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            val n = "usr_" + (1..14).map { chars.random() }.joinToString("")
            p.edit().putString("id", n).apply()
            return n
        }

    fun newDeviceId(): String { p.edit().remove("id").apply(); return deviceId }
}

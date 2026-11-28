package com.example.tvbrowser

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import org.json.JSONArray
import org.json.JSONObject

/** A video the user stopped watching: page address, title, position and length in seconds. */
class Watch(val url: String, val title: String, val pos: Int, val dur: Int, val ts: Long)

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
    fun replaceBookmarks(json: String) = save("b", parse(json))
    fun replaceHistory(json: String) = save("h", parse(json).take(200))
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
        set(v) { p.edit().putBoolean("mouse", v).apply(); markSettings() }
    var autohide: Boolean
        get() = p.getBoolean("autohide", true)
        set(v) { p.edit().putBoolean("autohide", v).apply(); markSettings() }
    var ua: Int
        get() = gi("ua", 0)
        set(v) { si("ua", v); markSettings() }
    var speed: Int
        get() = gi("speed", 6)
        set(v) { si("speed", v); markSettings() }
    var accel: Int
        get() = gi("accel", 4)
        set(v) { si("accel", v); markSettings() }
    var scrollSpeed: Int
        get() = gi("scroll", 5)
        set(v) { si("scroll", v); markSettings() }
    var cursorSize: Int
        get() = gi("csize", 1)
        set(v) { si("csize", v); markSettings() }
    var cursorColor: Int
        get() = gi("ccolor", 0)
        set(v) { si("ccolor", v); markSettings() }
    var zoom: Int
        get() = gi("zoom", 100)
        set(v) { si("zoom", v); markSettings() }
    var home: String
        get() = p.getString("home", "tvb:home") ?: "tvb:home"
        set(v) { p.edit().putString("home", v).apply(); markSettings() }

    var syncId: String?
        get() = p.getString("syncid", null)
        set(v) { if (v == null) p.edit().remove("syncid").apply() else p.edit().putString("syncid", v).apply() }
    /** Safe margin on every side, in tenths of a percent of the screen (TV overscan). */
    var overscan10: Int
        get() = gi("overscan10", if (isTv) 20 else 0)
        set(v) { si("overscan10", v) }
    var lang: String
        get() = p.getString("lang", "") ?: ""
        set(v) { p.edit().putString("lang", v).apply(); markSettings() }

    /** 0 = off, 1 = block popups, 2 = block ads and popups. */
    var blockMode: Int
        get() = gi("block", 2)
        set(v) { si("block", v); markSettings() }

    fun allowList(): MutableSet<String> {
        val out = mutableSetOf<String>()
        try {
            val a = JSONArray(p.getString("allow", "[]"))
            for (i in 0 until a.length()) out.add(a.getString(i))
        } catch (e: Exception) {}
        return out
    }
    fun isAllowed(host: String?): Boolean = host != null && allowList().contains(host)
    fun setAllowed(host: String, on: Boolean) {
        val l = allowList()
        if (on) l.add(host) else l.remove(host)
        p.edit().putString("allow", JSONArray(l.toList()).toString()).apply()
        markSettings()
    }

    private fun watchLoad(): MutableList<Watch> {
        val out = mutableListOf<Watch>()
        try {
            val a = JSONArray(p.getString("watch", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(Watch(o.getString("u"), o.optString("t"), o.getInt("p"), o.getInt("d"), o.optLong("s")))
            }
        } catch (e: Exception) {}
        return out
    }

    private fun watchSave(l: List<Watch>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("u", it.url).put("t", it.title).put("p", it.pos).put("d", it.dur).put("s", it.ts)) }
        p.edit().putString("watch", a.toString()).apply()
    }

    fun watch(): List<Watch> = watchLoad()
    fun watchJson(): String = p.getString("watch", "[]") ?: "[]"
    fun clearWatch() { p.edit().remove("watch").apply() }

    fun watchFor(url: String): Watch? {
        val u = url.substringBefore('#')
        return watchLoad().firstOrNull { it.url == u }
    }

    /** Remembers the position of a long video; finished or very short videos are forgotten. */
    fun saveProgress(url: String, title: String, pos: Int, dur: Int) {
        val u = url.substringBefore('#')
        val l = watchLoad()
        l.removeAll { it.url == u }
        if (dur >= 300 && pos >= 20 && pos <= dur - 90) {
            l.add(0, Watch(u, title, pos, dur, System.currentTimeMillis()))
        }
        watchSave(l.take(20))
    }

    /** Newer entry wins for the same address. */
    fun mergeWatch(json: String) {
        val l = watchLoad()
        try {
            val a = JSONArray(json)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val w = Watch(o.getString("u"), o.optString("t"), o.getInt("p"), o.getInt("d"), o.optLong("s"))
                val cur = l.firstOrNull { it.url == w.url }
                if (cur == null) l.add(w) else if (w.ts > cur.ts) { l.remove(cur); l.add(w) }
            }
        } catch (e: Exception) {}
        watchSave(l.sortedByDescending { it.ts }.take(20))
    }
    fun replaceWatch(json: String) { p.edit().putString("watch", json).apply() }

    // ---- settings that travel with the account (device-specific ones such as the screen margin stay local)
    private val syncInts = listOf("ua", "speed", "accel", "scroll", "csize", "ccolor", "zoom", "block")
    private val syncBools = listOf("mouse", "autohide")
    private val syncStrings = listOf("home", "lang", "allow")

    var syncSettings: Boolean
        get() = p.getBoolean("syncset", true)
        set(v) { p.edit().putBoolean("syncset", v).apply() }

    private fun markSettings() { p.edit().putLong("sts", System.currentTimeMillis()).apply() }

    fun settingsJson(): String {
        val o = JSONObject()
        for (k in syncInts) if (p.contains(k)) o.put(k, p.getInt(k, 0))
        for (k in syncBools) if (p.contains(k)) o.put(k, p.getBoolean(k, false))
        for (k in syncStrings) if (p.contains(k)) o.put(k, p.getString(k, "") ?: "")
        o.put("ts", p.getLong("sts", 0))
        return o.toString()
    }

    fun hasSettings(json: String): Boolean = try { JSONObject(json).length() > 1 } catch (e: Exception) { false }

    fun settingsNewer(json: String): Boolean =
        hasSettings(json) && (try { JSONObject(json).optLong("ts", 0) } catch (e: Exception) { 0L }) > p.getLong("sts", 0)

    fun applySettings(json: String) {
        try {
            val o = JSONObject(json)
            val e = p.edit()
            for (k in syncInts) if (o.has(k)) e.putInt(k, o.getInt(k))
            for (k in syncBools) if (o.has(k)) e.putBoolean(k, o.getBoolean(k))
            for (k in syncStrings) if (o.has(k)) e.putString(k, o.getString(k))
            e.putLong("sts", o.optLong("ts", System.currentTimeMillis()))
            e.apply()
        } catch (e: Exception) {}
    }

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

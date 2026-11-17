package com.example.tvbrowser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("tvb", Context.MODE_PRIVATE)

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

    fun bookmarks() = load("b")
    fun history() = load("h")

    fun addBookmark(t: String, u: String) {
        val l = load("b")
        if (l.none { it.second == u }) { l.add(0, t to u); save("b", l) }
    }

    fun removeBookmark(u: String) { save("b", load("b").filter { it.second != u }) }

    fun addHistory(t: String, u: String) {
        if (u.startsWith("about:") || u.startsWith("data:")) return
        val l = load("h")
        l.removeAll { it.second == u }
        l.add(0, t to u)
        save("h", l.take(200))
    }

    fun clearHistory() { p.edit().remove("h").apply() }

    var mouse: Boolean
        get() = p.getBoolean("mouse", true)
        set(v) { p.edit().putBoolean("mouse", v).apply() }

    var ua: Int
        get() = p.getInt("ua", 0)
        set(v) { p.edit().putInt("ua", v).apply() }

    var lastUrl: String
        get() = p.getString("last", "https://www.google.com") ?: "https://www.google.com"
        set(v) { p.edit().putString("last", v).apply() }
}

package com.example.tvbrowser

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Minimal Firestore REST client. One document per device ID in the collection "tvbrowser". */
object Cloud {
    class Data(val bookmarks: String, val history: String)

    fun configured() = BuildConfig.FIREBASE_API_KEY.isNotBlank() && BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()

    private val root get() = "projects/${BuildConfig.FIREBASE_PROJECT_ID}/databases/(default)/documents"

    private fun open(url: String, method: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 20000
        return c
    }

    private fun fail(c: HttpURLConnection): IOException {
        val body = try { c.errorStream?.bufferedReader()?.readText()?.take(160) ?: "" } catch (e: Exception) { "" }
        return IOException("HTTP ${c.responseCode} $body")
    }

    /** Returns null when this ID has no document yet. */
    fun get(id: String): Data? {
        val c = open("https://firestore.googleapis.com/v1/$root/tvbrowser/$id?key=${BuildConfig.FIREBASE_API_KEY}", "GET")
        try {
            if (c.responseCode == 404) return null
            if (c.responseCode != 200) throw fail(c)
            val f = JSONObject(c.inputStream.bufferedReader().readText()).optJSONObject("fields") ?: return Data("[]", "[]")
            return Data(
                f.optJSONObject("bookmarks")?.optString("stringValue", "[]") ?: "[]",
                f.optJSONObject("history")?.optString("stringValue", "[]") ?: "[]"
            )
        } finally {
            c.disconnect()
        }
    }

    fun put(id: String, d: Data) {
        val c = open("https://firestore.googleapis.com/v1/$root:commit?key=${BuildConfig.FIREBASE_API_KEY}", "POST")
        try {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            val fields = JSONObject()
                .put("bookmarks", JSONObject().put("stringValue", d.bookmarks))
                .put("history", JSONObject().put("stringValue", d.history))
                .put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
            val write = JSONObject()
                .put("update", JSONObject().put("name", "$root/tvbrowser/$id").put("fields", fields))
                .put("updateMask", JSONObject().put("fieldPaths", JSONArray(listOf("bookmarks", "history", "updatedAt"))))
            val body = JSONObject().put("writes", JSONArray().put(write)).toString()
            c.outputStream.use { it.write(body.toByteArray()) }
            if (c.responseCode != 200) throw fail(c)
        } finally {
            c.disconnect()
        }
    }
}

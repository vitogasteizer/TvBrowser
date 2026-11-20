package com.example.tvbrowser

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Firestore REST client. One document per device ID in the collection "tvbrowser".
 * The database may be called "(default)" or, if it was created by hand, "default"; both are tried.
 */
object Cloud {
    class Data(val bookmarks: String, val history: String)

    private val DBS = listOf("(default)", "default")
    @Volatile private var good: String? = null

    fun configured() = BuildConfig.FIREBASE_API_KEY.isNotBlank() && BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()

    private fun candidates(): List<String> = good?.let { listOf(it) } ?: DBS
    private fun docs(db: String) = "projects/${BuildConfig.FIREBASE_PROJECT_ID}/databases/$db/documents"

    private fun open(url: String, method: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 20000
        return c
    }

    private fun errBody(c: HttpURLConnection): String =
        try { c.errorStream?.bufferedReader()?.readText()?.take(200) ?: "" } catch (e: Exception) { "" }

    private fun missingDb(code: Int, body: String) =
        code == 404 && body.contains("database", true) && body.contains("does not exist", true)

    /** Returns null when this ID has no document yet. */
    fun get(id: String): Data? {
        var last: IOException? = null
        for (db in candidates()) {
            val c = open("https://firestore.googleapis.com/v1/${docs(db)}/tvbrowser/$id?key=${BuildConfig.FIREBASE_API_KEY}", "GET")
            try {
                val code = c.responseCode
                if (code == 200) {
                    good = db
                    val f = JSONObject(c.inputStream.bufferedReader().readText()).optJSONObject("fields") ?: return Data("[]", "[]")
                    return Data(
                        f.optJSONObject("bookmarks")?.optString("stringValue", "[]") ?: "[]",
                        f.optJSONObject("history")?.optString("stringValue", "[]") ?: "[]"
                    )
                }
                val body = errBody(c)
                if (missingDb(code, body)) { last = IOException("HTTP $code $body"); continue }
                if (code == 404) { good = db; return null }
                throw IOException("HTTP $code $body")
            } finally {
                c.disconnect()
            }
        }
        throw last ?: IOException("No Firestore database found")
    }

    fun put(id: String, d: Data) {
        var last: IOException? = null
        for (db in candidates()) {
            val c = open("https://firestore.googleapis.com/v1/${docs(db)}:commit?key=${BuildConfig.FIREBASE_API_KEY}", "POST")
            try {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                val fields = JSONObject()
                    .put("bookmarks", JSONObject().put("stringValue", d.bookmarks))
                    .put("history", JSONObject().put("stringValue", d.history))
                    .put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                val write = JSONObject()
                    .put("update", JSONObject().put("name", "${docs(db)}/tvbrowser/$id").put("fields", fields))
                    .put("updateMask", JSONObject().put("fieldPaths", JSONArray(listOf("bookmarks", "history", "updatedAt"))))
                val body = JSONObject().put("writes", JSONArray().put(write)).toString()
                c.outputStream.use { it.write(body.toByteArray()) }
                val code = c.responseCode
                if (code == 200) { good = db; return }
                val eb = errBody(c)
                if (missingDb(code, eb)) { last = IOException("HTTP $code $eb"); continue }
                throw IOException("HTTP $code $eb")
            } finally {
                c.disconnect()
            }
        }
        throw last ?: IOException("No Firestore database found")
    }
}

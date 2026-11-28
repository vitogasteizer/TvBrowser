package com.example.tvbrowser

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Host-based ad and popup blocker: a bundled list plus an optional downloaded list. */
object AdBlock {
    const val LIST_URL = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=nohtml&showintro=0&mimetype=plaintext"

    @Volatile private var hosts: Set<String> = emptySet()
    @Volatile var blocked = 0

    fun count() = hosts.size

    private fun parse(line: String, out: HashSet<String>) {
        val t = line.trim().lowercase()
        if (t.isEmpty() || t.startsWith("#")) return
        val parts = t.split(Regex("\\s+"))
        val h = if (parts.size >= 2 && parts[0].first().isDigit()) parts[1] else parts[0]
        if (h.contains('.') && !h.contains('/') && !h.contains(':')) out.add(h)
    }

    fun load(ctx: Context) {
        Thread {
            val s = HashSet<String>()
            try { ctx.assets.open("adhosts.txt").bufferedReader().useLines { l -> l.forEach { parse(it, s) } } } catch (_: Exception) {}
            try {
                val f = File(ctx.filesDir, "filters.txt")
                if (f.exists()) f.useLines { l -> l.forEach { parse(it, s) } }
            } catch (_: Exception) {}
            hosts = s
        }.start()
    }

    /** Downloads the larger filter list. Returns the number of known hosts, or -1 on failure. */
    fun update(ctx: Context): Int {
        return try {
            val c = URL(LIST_URL).openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 30000
            if (c.responseCode != 200) return -1
            val tmp = File(ctx.filesDir, "filters.tmp")
            c.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            c.disconnect()
            tmp.renameTo(File(ctx.filesDir, "filters.txt"))
            val s = HashSet<String>()
            try { ctx.assets.open("adhosts.txt").bufferedReader().useLines { l -> l.forEach { parse(it, s) } } } catch (_: Exception) {}
            File(ctx.filesDir, "filters.txt").useLines { l -> l.forEach { parse(it, s) } }
            hosts = s
            s.size
        } catch (e: Exception) {
            -1
        }
    }

    /** True when the host, or one of its parent domains, is on the list. */
    fun isBlocked(host: String): Boolean {
        var h = host.lowercase()
        val set = hosts
        while (true) {
            if (set.contains(h)) return true
            val i = h.indexOf('.')
            if (i < 0) return false
            h = h.substring(i + 1)
            if (!h.contains('.')) return false
        }
    }
}

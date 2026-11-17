package com.example.tvbrowser

import android.util.Base64
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList

/** Tiny HTTP + WebSocket server: serves the phone remote page and receives commands. */
class RemoteServer(
    private val onMsg: (String) -> Unit,
    private val onClients: (Int) -> Unit
) {
    val token: String = (1..6).map { "abcdefghijkmnpqrstuvwxyz23456789".random() }.joinToString("")
    var port = 8080
    @Volatile private var running = false
    private var server: ServerSocket? = null
    private val clients = CopyOnWriteArrayList<Socket>()

    fun start(): Boolean {
        if (running) return true
        val s = try { ServerSocket(8080) } catch (e: Exception) {
            try { ServerSocket(0) } catch (e2: Exception) { return false }
        }
        server = s
        port = s.localPort
        running = true
        Thread {
            while (running) {
                try {
                    val c = s.accept()
                    Thread { handle(c) }.start()
                } catch (e: Exception) { break }
            }
        }.start()
        return true
    }

    fun stop() {
        running = false
        try { server?.close() } catch (_: Exception) {}
        clients.forEach { try { it.close() } catch (_: Exception) {} }
        clients.clear()
        onClients(0)
    }

    private fun readLine(i: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = i.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == 10) break
            if (b != 13) sb.append(b.toChar())
        }
        return sb.toString()
    }

    private fun readFully(i: InputStream, buf: ByteArray) {
        var off = 0
        while (off < buf.size) {
            val n = i.read(buf, off, buf.size - off)
            if (n < 0) throw java.io.IOException("eof")
            off += n
        }
    }

    private fun respond(o: OutputStream, code: Int, type: String, body: String) {
        val b = body.toByteArray(Charsets.UTF_8)
        val head = "HTTP/1.1 $code OK\r\nContent-Type: $type\r\nContent-Length: ${b.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n"
        o.write(head.toByteArray())
        o.write(b)
        o.flush()
    }

    private fun handle(c: Socket) {
        try {
            c.tcpNoDelay = true
            val inp = c.getInputStream().buffered()
            val out = c.getOutputStream()
            val reqLine = readLine(inp) ?: return
            val headers = HashMap<String, String>()
            while (true) {
                val l = readLine(inp) ?: return
                if (l.isEmpty()) break
                val i = l.indexOf(':')
                if (i > 0) headers[l.substring(0, i).trim().lowercase()] = l.substring(i + 1).trim()
            }
            val path = reqLine.split(" ").getOrNull(1) ?: "/"
            if (!path.contains("k=$token")) {
                respond(out, 403, "text/plain", "Forbidden")
                c.close()
                return
            }
            if (headers["upgrade"]?.lowercase() == "websocket") {
                ws(c, inp, out, headers["sec-websocket-key"] ?: "")
                return
            }
            respond(out, 200, "text/html; charset=utf-8", RemotePage.HTML)
            c.close()
        } catch (e: Exception) {
            try { c.close() } catch (_: Exception) {}
        }
    }

    private fun ws(c: Socket, inp: InputStream, out: OutputStream, key: String) {
        val acc = Base64.encodeToString(
            MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()),
            Base64.NO_WRAP
        )
        out.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: $acc\r\n\r\n").toByteArray())
        out.flush()
        clients.add(c)
        onClients(clients.size)
        try {
            while (true) {
                val b0 = inp.read()
                if (b0 < 0) break
                val b1 = inp.read()
                if (b1 < 0) break
                val op = b0 and 0x0F
                var len = (b1 and 0x7F).toLong()
                if (len == 126L) {
                    len = ((inp.read() shl 8) or inp.read()).toLong()
                } else if (len == 127L) {
                    len = 0
                    repeat(8) { len = (len shl 8) or inp.read().toLong() }
                }
                val masked = (b1 and 0x80) != 0
                val mask = ByteArray(4)
                if (masked) readFully(inp, mask)
                if (len > 1_000_000L) break
                val data = ByteArray(len.toInt())
                readFully(inp, data)
                if (masked) for (i in data.indices) data[i] = (data[i].toInt() xor mask[i % 4].toInt()).toByte()
                if (op == 8) break
                if (op == 9 && data.size < 126) {
                    synchronized(out) {
                        out.write(byteArrayOf(0x8A.toByte(), data.size.toByte()))
                        out.write(data)
                        out.flush()
                    }
                }
                if (op == 1) onMsg(String(data, Charsets.UTF_8))
            }
        } catch (e: Exception) {
        } finally {
            clients.remove(c)
            onClients(clients.size)
            try { c.close() } catch (_: Exception) {}
        }
    }
}

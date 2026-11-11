package com.sindantechnologies.gharhisab

import com.sindantechnologies.gharhisab.data.backup.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.net.*
import java.io.*

class DriveHttpTest {
    private class Connection(url: URL, private val status: Int, private val response: ByteArray) : HttpURLConnection(url) {
        val sent = ByteArrayOutputStream()
        var closed = false
        override fun getResponseCode() = status
        override fun getInputStream() = ByteArrayInputStream(response)
        override fun getOutputStream() = sent
        override fun connect() {}
        override fun disconnect() { closed = true }
        override fun usingProxy() = false
    }
    @Test fun multipartUploadsBinaryBytesToApplicationDataFolder() = runBlocking {
        lateinit var request: Connection
        val client = DriveBackupClient("test-token") { url -> Connection(url, 200, "{\"id\":\"file1\"}".toByteArray()).also { request = it } }
        val encrypted = byteArrayOf(0, 127, -128, -1, 10)
        client.upload(encrypted)
        assertEquals("POST", request.requestMethod)
        assertEquals("www.googleapis.com", request.url.host)
        assertEquals("/upload/drive/v3/files", request.url.path)
        assertEquals("Bearer test-token", request.getRequestProperty("Authorization"))
        assertFalse(request.instanceFollowRedirects)
        val contentType = request.getRequestProperty("Content-Type")
        val boundary = contentType.substringAfter("boundary=")
        val body = request.sent.toByteArray()
        val text = String(body, Charsets.ISO_8859_1)
        assertTrue(text.contains("\"parents\":[\"appDataFolder\"]"))
        assertTrue(text.startsWith("--$boundary\r\n"))
        assertTrue(text.endsWith("\r\n--$boundary--\r\n"))
        val dataStart = text.indexOf("Content-Type: application/octet-stream\r\n\r\n") + "Content-Type: application/octet-stream\r\n\r\n".length
        assertArrayEquals(encrypted, body.copyOfRange(dataStart, dataStart + encrypted.size))
        assertTrue(request.closed)
    }
    @Test fun listsApplicationDataAndDownloadsSelectedBinary() = runBlocking {
        val requests = mutableListOf<Connection>()
        val client = DriveBackupClient("test-token") { url ->
            val bytes = if (url.query.contains("alt=media")) byteArrayOf(1, 2, -1)
                else "{\"files\":[{\"id\":\"file1\",\"name\":\"GharHisab_one\",\"modifiedTime\":\"today\"}]}".toByteArray()
            Connection(url, 200, bytes).also { requests.add(it) }
        }
        assertEquals("file1", client.list().single().id)
        assertTrue(requests.first().url.query.contains("spaces=appDataFolder"))
        assertArrayEquals(byteArrayOf(1, 2, -1), client.download("file1"))
        assertEquals("/drive/v3/files/file1", requests.last().url.path)
        assertTrue(requests.all { it.closed })
    }
    @Test fun httpFailuresAreTypedAndConnectionsAreClosed() = runBlocking {
        for (code in listOf(401, 403, 429, 500)) {
            lateinit var request: Connection
            val client = DriveBackupClient("test-token") { url -> Connection(url, code, byteArrayOf()).also { request = it } }
            val failure = runCatching { client.list() }.exceptionOrNull()
            if (code == 401) assertTrue(failure is DriveBackupClient.AuthorizationExpired)
            else assertEquals(code, (failure as DriveBackupClient.HttpFailure).statusCode)
            assertTrue(request.closed)
        }
    }
}

package com.sindantechnologies.gharhisab.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import java.util.UUID

/** Immutable snapshots in the user's appDataFolder. No automatic last-writer-wins sync. */
class DriveBackupClient(
    private val token: String,
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) : DriveService {
    data class RemoteBackup(val id: String, val name: String, val modifiedTime: String)
    class AuthorizationExpired : java.io.IOException()
    class HttpFailure(val statusCode: Int) : java.io.IOException()
    class ResponseTooLarge : java.io.IOException()
    private fun request(url: String, method: String = "GET", body: ByteArray? = null, contentType: String = "application/octet-stream"): ByteArray {
        val connection = openConnection(URL(url))
        try {
            connection.connectTimeout = 20_000; connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType)
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            if (code == 401) throw AuthorizationExpired()
            if (code !in 200..299) throw HttpFailure(code)
            return connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > BackupManager.MAX_BYTES) throw ResponseTooLarge()
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally { connection.disconnect() }
    }
    override suspend fun list(): List<RemoteBackup> = withContext(Dispatchers.IO) {
        val query = java.net.URLEncoder.encode("trashed = false and name contains 'GharHisab_'", "UTF-8")
        val json = JSONObject(String(request("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$query&orderBy=modifiedTime%20desc&pageSize=20&fields=files(id,name,modifiedTime)")))
        val files = json.getJSONArray("files")
        (0 until files.length()).map { files.getJSONObject(it).let { f -> RemoteBackup(f.getString("id"), f.getString("name"), f.getString("modifiedTime")) } }
    }
    override suspend fun upload(encrypted: ByteArray): Unit = withContext(Dispatchers.IO) {
        require(encrypted.size <= BackupManager.MAX_BYTES)
        val boundary = UUID.randomUUID().toString()
        val metadata = JSONObject().put("name", "GharHisab_${System.currentTimeMillis()}.gharhisab")
            .put("parents", org.json.JSONArray().put("appDataFolder"))
        val prefix = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n--$boundary\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray()
        request("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id", "POST", prefix + encrypted + "\r\n--$boundary--\r\n".toByteArray(), "multipart/related; boundary=$boundary")
        Unit
    }
    override suspend fun download(id: String): ByteArray = withContext(Dispatchers.IO) {
        require(id.matches(Regex("[A-Za-z0-9_-]+")))
        request("https://www.googleapis.com/drive/v3/files/$id?alt=media")
    }
}

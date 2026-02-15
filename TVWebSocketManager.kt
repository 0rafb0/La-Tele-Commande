// TVWebSocketManager.kt
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import android.util.Log

class TVWebSocketManager(private val tvIp: String) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect() {
        val request = Request.Builder().url("ws://$tvIp:8001/api/v2/channels/samsung.remote.control?name=MyApp").build()
        webSocket = client.newWebSocket(request, TVWebSocketListener())
    }

    fun disconnect() {
        webSocket?.close(1000, "Fermeture de la connexion")
    }

    fun sendCommand(command: String) {
        val jsonCommand = "{\"method\":\"ms.remote.control\", \"params\":{\"Cmd\":\"Click\", \"DataOfCmd\":\"$command\", \"Option\":false, \"TypeOfRemote\":\"SendRemoteKey\"}}"
        webSocket?.send(jsonCommand)
    }

    private inner class TVWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            super.onOpen(webSocket, response)
            Log.d("TV_REMOTE", "Connexion WebSocket établie")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.d("TV_REMOTE", "Message reçu : $text")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            Log.d("TV_REMOTE", "Message binaire reçu")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            Log.d("TV_REMOTE", "Fermeture de la connexion : $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            super.onFailure(webSocket, t, response)
            Log.e("TV_REMOTE", "Erreur WebSocket : ${t.message}")
        }
    }
}

package com.example.latelecommande

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.util.concurrent.TimeUnit
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TeleCommandeAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    TVRemoteApp()
                }
            }
        }
    }
}

@Composable
fun TVRemoteApp() {
    var tvIp by remember { mutableStateOf(TextFieldValue("192.168.1.149")) }
    var tvWebSocketManager: TVWebSocketManager? by remember { mutableStateOf(null) }
    val context = LocalContext.current
    var connectionStatus by remember { mutableStateOf("Non connecté") }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Section de connexion
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                "Contrôle TV Samsung (WebSocket)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tvIp,
                onValueChange = { tvIp = it },
                label = { Text("Adresse IP de la TV", color = Color.White) },
                placeholder = { Text("Ex: 192.168.1.149") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF333333),
                    unfocusedContainerColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedIndicatorColor = Color.Blue,
                    unfocusedIndicatorColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Statut : $connectionStatus",
                style = MaterialTheme.typography.bodyMedium,
                color = if (connectionStatus == "Connecté") Color.Green else Color.Red
            )

            if (connectionError != null) {
                Text(
                    "Erreur : $connectionError",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (tvWebSocketManager == null || !tvWebSocketManager!!.isConnected()) {
                        tvWebSocketManager = TVWebSocketManager(tvIp.text)
                        tvWebSocketManager?.setConnectionListener { isConnected, error ->
                            connectionStatus = if (isConnected) "Connecté" else "Non connecté"
                            connectionError = error
                            showControls = isConnected
                        }
                        tvWebSocketManager?.connect()
                        Toast.makeText(context, "Connexion en cours...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Déjà connecté !", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), contentColor = Color.White)
            ) {
                Text(if (tvWebSocketManager != null && tvWebSocketManager!!.isConnected()) "Reconnecter" else "Se connecter")
            }
        }

        // Section des commandes (avec défilement)
        if (showControls && tvWebSocketManager != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .verticalScroll(rememberScrollState())
            ) {
                TVRemoteScreen(tvWebSocketManager!!)
            }
        }
    }
}

@Composable
fun TVRemoteScreen(tvWebSocketManager: TVWebSocketManager) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text("Commandes de base :", style = MaterialTheme.typography.titleSmall, color = Color.White)

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(
                        onClick = { tvWebSocketManager.sendCommand("KEY_VOLDOWN") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444), contentColor = Color.White)
                    ) {
                        Text("Volume -")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { tvWebSocketManager.sendCommand("KEY_VOLUP") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444), contentColor = Color.White)
                    ) {
                        Text("Volume +")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { tvWebSocketManager.sendCommand("KEY_UP") },
                        modifier = Modifier
                            .width(120.dp)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444), contentColor = Color.White)
                    ) {
                        Text("↑", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { tvWebSocketManager.sendCommand("KEY_LEFT") },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444), contentColor = Color.White)
                        ) {
                            Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { tvWebSocketManager.sendCommand("KEY_ENTER") },
                            modifier = Modifier
                                .height(55.dp)
                                .padding(top = 5.dp, start = 2.5.dp, end = 2.5.dp, bottom = 2.5.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                        ) {
                            Text("OK", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { tvWebSocketManager.sendCommand("KEY_RIGHT") },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444), contentColor = Color.White)
                        ) {
                            Text("→", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { tvWebSocketManager.sendCommand("KEY_DOWN") },
                        modifier = Modifier
                            .width(120.dp)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444), contentColor = Color.White)
                    ) {
                        Text("↓", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { tvWebSocketManager.sendCommand("KEY_HOME") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
            ) {
                Text("Accueil")
            }
            Button(
                onClick = { tvWebSocketManager.sendCommand("KEY_RETURN") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
            ) {
                Text("Retour")
            }
            Button(
                onClick = { tvWebSocketManager.sendCommand("KEY_POWER") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2), contentColor = Color.White)
            ) {
                Text("Power")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Applications :", style = MaterialTheme.typography.titleSmall, color = Color.White)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { tvWebSocketManager.sendNetflixCommand() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.Red)
            ) {
                Text("Netflix")
            }

            Button(
                onClick = { tvWebSocketManager.sendCanalPlusCommand() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
            ) {
                Text("Canal+")
            }

            Button(
                onClick = { tvWebSocketManager.sendPrimeVideoCommand() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2), contentColor = Color.White)
            ) {
                Text("Prime Vidéo")
            }

            Button(
                onClick = { tvWebSocketManager.sendIPTVCommand() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E44AD), contentColor = Color.White)
            ) {
                Text("IPTV")
            }
        }
    }
}

class TVWebSocketManager(private val tvIp: String, private val name: String = "SamsungRemote") {
    private val client: OkHttpClient
    private var webSocket: WebSocket? = null
    private var token: String? = null
    private var isConnected = false
    private var connectionAttempts = 0
    private val maxConnectionAttempts = 3
    private var connectionListener: ((Boolean, String?) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isGettingToken = false

    init {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        client = OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .pingInterval(10, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun getEncodedName(): String {
        return Base64.encodeToString(name.toByteArray(), Base64.NO_WRAP)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("TV_WS", "Connexion WebSocket établie avec succès")
            if (isGettingToken) {
                isConnected = true
                connectionAttempts = 0
                connectionListener?.invoke(true, null)
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("TV_WS", "Message reçu : $text")
            try {
                val json = JSONObject(text)
                if (json.has("data")) {
                    val data = json.getJSONObject("data")
                    if (data.has("token")) {
                        token = data.getString("token")
                        Log.d("TV_WS", "Token reçu : $token")
                        if (!isGettingToken) {
                            reconnectWithToken()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TV_WS", "Erreur lors de l'analyse du message : ${e.message}")
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("TV_WS", "Fermeture de la connexion : $reason (code: $code)")
            if (token != null && !isConnected) {
                handler.postDelayed({
                    reconnectWithToken()
                }, 1000)
            } else {
                isConnected = false
                connectionListener?.invoke(false, "Déconnecté : $reason")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("TV_WS", "Erreur WebSocket : ${t.message}", t)
            isConnected = false
            connectionListener?.invoke(false, "Erreur : ${t.message}")

            if (connectionAttempts < maxConnectionAttempts) {
                connectionAttempts++
                handler.postDelayed({
                    Log.d("TV_WS", "Nouvelle tentative de connexion (attempt $connectionAttempts)")
                    if (token != null) {
                        reconnectWithToken()
                    } else {
                        connect()
                    }
                }, 2000)
            } else {
                Log.e("TV_WS", "Nombre maximal de tentatives de connexion atteint")
            }
        }
    }

    fun setConnectionListener(listener: (Boolean, String?) -> Unit) {
        connectionListener = listener
    }

    fun connect() {
        isGettingToken = true
        try {
            val encodedName = getEncodedName()
            val url = "wss://$tvIp:8002/api/v2/channels/samsung.remote.control?name=$encodedName"
            Log.d("TV_WS", "Tentative de connexion à $url")
            connectToUrl(url)
        } catch (e: Exception) {
            Log.e("TV_WS", "Erreur lors de la connexion : ${e.message}")
            connectionListener?.invoke(false, "Erreur : ${e.message}")
        }
    }

    private fun reconnectWithToken() {
        if (token == null) {
            Log.e("TV_WS", "Token non disponible pour la reconnexion")
            return
        }
        isGettingToken = false
        try {
            val encodedName = getEncodedName()
            val url = "wss://$tvIp:8002/api/v2/channels/samsung.remote.control?name=$encodedName&token=$token"
            Log.d("TV_WS", "Reconnexion avec token à $url")
            connectToUrl(url)
        } catch (e: Exception) {
            Log.e("TV_WS", "Erreur lors de la reconnexion : ${e.message}")
            connectionListener?.invoke(false, "Erreur de reconnexion : ${e.message}")
        }
    }

    private fun connectToUrl(url: String) {
        try {
            val request = Request.Builder().url(url).build()
            webSocket?.cancel()
            webSocket = client.newWebSocket(request, listener)
        } catch (e: Exception) {
            Log.e("TV_WS", "Erreur de connexion à $url : ${e.message}")
            connectionListener?.invoke(false, "Erreur de connexion : ${e.message}")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Fermeture normale")
        isConnected = false
        connectionListener?.invoke(false, "Déconnecté manuellement")
    }

    fun sendCommand(command: String) {
        if (!isConnected) {
            Log.e("TV_WS", "WebSocket non connecté, impossible d'envoyer la commande")
            connectionListener?.invoke(false, "Non connecté : impossible d'envoyer la commande")
            return
        }

        val jsonCommand = JSONObject()
            .put("method", "ms.remote.control")
            .put("params", JSONObject()
                .put("Cmd", "Click")
                .put("DataOfCmd", command)
                .put("Option", false)
                .put("TypeOfRemote", "SendRemoteKey"))

        val commandString = jsonCommand.toString()
        Log.d("TV_WS", "Envoi de la commande : $commandString")
        webSocket?.send(commandString)
    }

    fun sendNetflixCommand() {
        if (!isConnected) {
            Log.e("TV_WS", "WebSocket non connecté, impossible d'envoyer la commande Netflix")
            connectionListener?.invoke(false, "Non connecté : impossible d'envoyer la commande Netflix")
            return
        }

        Handler(Looper.getMainLooper()).post {
            sendCommand("KEY_HOME")

            Handler(Looper.getMainLooper()).postDelayed({
                for (i in 1..4) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        sendCommand("KEY_RIGHT")
                    }, 200 * i.toLong())
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    sendCommand("KEY_ENTER")

                    Handler(Looper.getMainLooper()).postDelayed({
                        sendCommand("KEY_ENTER")
                    }, 3000)
                }, 1000)
            }, 1000)
        }
    }

    fun sendCanalPlusCommand() {
        if (!isConnected) {
            Log.e("TV_WS", "WebSocket non connecté, impossible d'envoyer la commande Canal+")
            connectionListener?.invoke(false, "Non connecté : impossible d'envoyer la commande Canal+")
            return
        }

        Handler(Looper.getMainLooper()).post {
            sendCommand("KEY_HOME")

            Handler(Looper.getMainLooper()).postDelayed({
                for (i in 1..3) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        sendCommand("KEY_RIGHT")
                    }, 200 * i.toLong())
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    sendCommand("KEY_ENTER")
                }, 800)
            }, 1000)
        }
    }

    fun sendPrimeVideoCommand() {
        if (!isConnected) {
            Log.e("TV_WS", "WebSocket non connecté, impossible d'envoyer la commande Prime Vidéo")
            connectionListener?.invoke(false, "Non connecté : impossible d'envoyer la commande Prime Vidéo")
            return
        }

        Handler(Looper.getMainLooper()).post {
            sendCommand("KEY_HOME")

            Handler(Looper.getMainLooper()).postDelayed({
                for (i in 1..5) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        sendCommand("KEY_RIGHT")
                    }, 200 * i.toLong())
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    sendCommand("KEY_ENTER")
                }, 1200)
            }, 1000)
        }
    }

    fun sendIPTVCommand() {
        if (!isConnected) {
            Log.e("TV_WS", "WebSocket non connecté, impossible d'envoyer la commande IPTV")
            connectionListener?.invoke(false, "Non connecté : impossible d'envoyer la commande IPTV")
            return
        }

        Handler(Looper.getMainLooper()).post {
            sendCommand("KEY_HOME")

            Handler(Looper.getMainLooper()).postDelayed({
                for (i in 1..6) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        sendCommand("KEY_RIGHT")
                    }, 200 * i.toLong())
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    sendCommand("KEY_ENTER")
                }, 1400)
            }, 1000)
        }
    }

    fun isConnected(): Boolean = isConnected
}

@Composable
fun TeleCommandeAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6),
            tertiary = Color(0xFF3700B3)
        ),
        typography = MaterialTheme.typography,
        content = content
    )
}

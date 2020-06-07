package com.groundzero.webrtc.webrtc

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.groundzero.webrtc.MainActivityListener
import com.groundzero.webrtc.R
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import okhttp3.*
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.lang.Exception

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SignallingClient(
    private val listener: SignallingClientListener,
    private val mainActivityListener: MainActivityListener
) {

    private val gson = Gson()
    private lateinit var socket: WebSocket
    private val client = OkHttpClient()
    private val socketRequest: Request = Request.Builder().url("$SERVER_URL:$PORT").build()

    private val sendChannel = ConflatedBroadcastChannel<String>()
    val sendData = sendChannel.openSubscription()

    fun openSocket() {
        socket = client.newWebSocket(socketRequest, socketListener())
    }

    private fun socketListener() = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            listener.onConnectionEstablished()
            CoroutineScope(Main).launch {
                mainActivityListener.showMessage(R.string.connection_established)
            }

           CoroutineScope(IO).launch {
               while (true) {
                   sendData.poll()?.let {
                       socket.send(it)
                   }
               }
           }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)

            try{
                val jsonObject = gson.fromJson(text, JsonObject::class.java)
                if (jsonObject.has("serverUrl")) {
                    listener.onIceCandidateReceived(gson.fromJson(jsonObject, IceCandidate::class.java))
                } else if (jsonObject.has("type") && jsonObject.get("type").asString == "OFFER") {
                    listener.onOfferReceived(gson.fromJson(jsonObject, SessionDescription::class.java))
                } else if (jsonObject.has("type") && jsonObject.get("type").asString == "ANSWER") {
                    listener.onAnswerReceived(gson.fromJson(jsonObject, SessionDescription::class.java))
                }
            }catch (e: Exception) {
                println("Unable to deserialize data")
            }
        }
    }

    fun send(dataObject: Any?) = CoroutineScope(IO).launch {
        sendChannel.send(gson.toJson(dataObject))
    }

    fun onDestroy() {
        socket.close(CLOSING_STATUS, null)
    }

    companion object {
        private const val SERVER_URL = "ws://192.168.0.16"
        private const val PORT = "3000"
        private const val CLOSING_STATUS = 1000
    }
}
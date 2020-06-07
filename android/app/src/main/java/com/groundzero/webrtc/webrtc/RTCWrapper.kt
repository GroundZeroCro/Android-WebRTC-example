package com.groundzero.webrtc.webrtc

import android.content.Context
import android.view.View
import com.groundzero.webrtc.MainActivityListener
import com.groundzero.webrtc.RTCWrapperListener
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class RTCWrapper(
    private val context: Context,
    private val listener: MainActivityListener,
    private val wrapperListener: RTCWrapperListener
) {

    private lateinit var rtcClient: RTCClient
    private lateinit var signallingClient: SignallingClient

    fun createRTC() {
        rtcClient = RTCClient(context, peerConnectionObservable()).also {
            wrapperListener.instantiateSurfaceView { it.getEglContext() }
            wrapperListener.startVideoCapture { it }
            wrapperListener.onCallClick { it.call(sdpObserver) }
        }
        signallingClient =
            SignallingClient(signalingClientListener(), listener).also { it.openSocket() }
    }

    private fun peerConnectionObservable() = object : PeerConnectionObserver() {

        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            super.onIceCandidate(iceCandidate)
            signallingClient.send(iceCandidate)
            rtcClient.addIceCandidate(iceCandidate)
        }

        override fun onAddStream(mediaStream: MediaStream?) {
            super.onAddStream(mediaStream)
            wrapperListener.onAddStream { mediaStream }
        }
    }

    private fun signalingClientListener() = object :
        SignallingClientListener {

        override fun onConnectionEstablished() {
        }

        override fun onOfferReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            rtcClient.answer(sdpObserver)
            wrapperListener.progressBarVisibility(View.GONE)
        }

        override fun onAnswerReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            wrapperListener.progressBarVisibility(View.GONE)
        }

        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
            rtcClient.addIceCandidate(iceCandidate)
        }
    }

    private val sdpObserver = object : AppSdpObserver() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            signallingClient.send(p0)
        }
    }

    fun onDestroy() {
        signallingClient.onDestroy()
    }
}
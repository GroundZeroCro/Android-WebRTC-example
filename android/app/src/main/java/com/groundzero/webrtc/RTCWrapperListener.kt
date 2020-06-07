package com.groundzero.webrtc

import com.groundzero.webrtc.webrtc.RTCClient
import org.webrtc.EglBase
import org.webrtc.MediaStream

interface RTCWrapperListener {
    fun onCallClick(onClick: () -> Unit)
    fun instantiateSurfaceView(eglBaseContext: () -> EglBase.Context)
    fun removeSurfaceView()
    fun onAddStream(add: () -> MediaStream?)
    fun progressBarVisibility(visibility: Int)
    fun startVideoCapture(client: () -> RTCClient)
    fun connectToServerButtonVisibility(visibility: Int)
    fun callServerButtonVisibility(visibility: Int)
}
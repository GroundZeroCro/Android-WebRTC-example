package com.groundzero.webrtc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.groundzero.webrtc.webrtc.*
import kotlinx.android.synthetic.main.activity_main.*
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class MainActivity : AppCompatActivity(), MainActivityListener {

    private lateinit var rtcClient: RTCClient
    private lateinit var signallingClient: SignallingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkCameraPermission()

        send_message.setOnClickListener { showMessage(R.string.test_button) }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                CAMERA_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
        } else {
            instantiateRTC()
        }
    }

    private fun instantiateRTC() {
        rtcClient = RTCClient(application, peerConnectionObservable()).also {
            it.initSurfaceView(remote_view)
            it.initSurfaceView(local_view)
            it.startLocalVideoCapture(local_view)
            call_button.setOnClickListener { _ ->
                it.call(sdpObserver)
            }
        }
        signallingClient =
            SignallingClient(signalingClientListener(), this).also { it.openSocket() }
    }

    private fun peerConnectionObservable() = object : PeerConnectionObserver() {

        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            super.onIceCandidate(iceCandidate)
            signallingClient.send(iceCandidate)
            rtcClient.addIceCandidate(iceCandidate)
        }

        override fun onAddStream(mediaStream: MediaStream?) {
            super.onAddStream(mediaStream)
            mediaStream?.videoTracks?.get(0)?.addSink(remote_view)
        }
    }

    private fun signalingClientListener() = object :
        SignallingClientListener {

        override fun onConnectionEstablished() {
            call_button.isClickable = true
        }

        override fun onOfferReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            rtcClient.answer(sdpObserver)
            remote_view_loading.isGone = true
        }

        override fun onAnswerReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            remote_view_loading.isGone = true
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

    private fun requestCameraPermission(dialogShown: Boolean = false) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, CAMERA_PERMISSION
            ) && !dialogShown
        ) {
            showPermissionRationaleDialog()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(CAMERA_PERMISSION), REQUEST_CODE
            )
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("This app need the camera to function")
            .setPositiveButton("Grant") { dialog, _ ->
                dialog.dismiss()
                requestCameraPermission(true)
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                onCameraPermissionDenied()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            instantiateRTC()
        } else {
            onCameraPermissionDenied()
        }
    }

    private fun onCameraPermissionDenied() = showMessage(R.string.camera_denied)

    override fun showMessage(resMessage: Int) =
        Toast.makeText(this, getString(resMessage), Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        super.onDestroy()
        signallingClient.destroy()
    }

    companion object {
        private const val REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
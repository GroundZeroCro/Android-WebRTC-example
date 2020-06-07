package com.groundzero.webrtc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.groundzero.webrtc.webrtc.RTCClient
import com.groundzero.webrtc.webrtc.RTCWrapper
import kotlinx.android.synthetic.main.activity_main.*
import org.webrtc.EglBase
import org.webrtc.MediaStream

class MainActivity : AppCompatActivity(), MainActivityListener, RTCWrapperListener {

    private lateinit var rtcWrapper: RTCWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rtcWrapper = RTCWrapper(this, this, this)
        checkCameraPermission()
        connect_to_server.setOnClickListener {
            rtcWrapper.retrySocketOpening()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                CAMERA_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
        } else {
            rtcWrapper.createRTC()
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
            }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            rtcWrapper.createRTC()
        } else {
            onCameraPermissionDenied()
        }
    }

    private fun onCameraPermissionDenied() = showMessage(R.string.camera_denied)

    override fun showMessage(resMessage: Int) =
        Toast.makeText(this, getString(resMessage), Toast.LENGTH_LONG).show()

    override fun onCallClick(onClick: () -> Unit) =
        call_button.setOnClickListener { onClick.invoke() }

    override fun instantiateSurfaceView(eglBaseContext: () -> EglBase.Context) {
        local_view.setMirror(true)
        local_view.setEnableHardwareScaler(true)
        local_view.init(eglBaseContext.invoke(), null)

        remote_view.setMirror(true)
        remote_view.setEnableHardwareScaler(true)
        remote_view.init(eglBaseContext.invoke(), null)
    }

    override fun removeSurfaceView() {
        local_view.clearImage()
        local_view.release()
        remote_view.clearImage()
        remote_view.release()
    }

    override fun startVideoCapture(client: () -> RTCClient) =
        client.invoke().startLocalVideoCapture(local_view)

    override fun connectToServerButtonVisibility(visibility: Int) {
        connect_to_server.visibility = visibility
    }

    override fun callServerButtonVisibility(visibility: Int) {
        call_button.visibility = visibility
    }

    override fun onAddStream(add: () -> MediaStream?) {
        add.invoke()?.videoTracks?.get(0)?.addSink(remote_view)
    }

    override fun progressBarVisibility(visibility: Int) {
        remote_view_loading.visibility = visibility
    }

    override fun onDestroy() {
        super.onDestroy()
        rtcWrapper.onDestroy()
    }

    companion object {
        private const val REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
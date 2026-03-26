package com.ibrahimcanerdogan.facedetectionapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ibrahimcanerdogan.facedetectionapp.camera.CameraManager
import com.ibrahimcanerdogan.facedetectionapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        cameraManager = CameraManager(
            context        = this,
            cameraView     = binding.cameraView,
            graphicOverlay = binding.viewGraphicOverlay,
            lifecycleOwner = this
        )

        setupButtons()
        askCameraPermission()
    }

    // ─── Buttons ──────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.buttonTurnCamera.setOnClickListener {
            cameraManager.changeCamera()
        }
        binding.buttonStopCamera.setOnClickListener {
            cameraManager.cameraStop()
            setButtonState(running = false)
        }
        binding.buttonStartCamera.setOnClickListener {
            cameraManager.cameraStart()
            setButtonState(running = true)
        }
        // NEW: Capture photo — orange rectangle drawn at capture moment
        binding.buttonCapturePhoto.setOnClickListener {
            cameraManager.capturePhoto()
        }
    }

    private fun setButtonState(running: Boolean) {
        binding.buttonStopCamera.visibility    = if (running) View.VISIBLE  else View.INVISIBLE
        binding.buttonStartCamera.visibility   = if (running) View.INVISIBLE else View.VISIBLE
        binding.buttonCapturePhoto.visibility  = if (running) View.VISIBLE  else View.INVISIBLE
    }

    // ─── Permissions ──────────────────────────────────────────────────────────

    private val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA)
    } else {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun askCameraPermission() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            setButtonState(running = true)
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                cameraManager.cameraStart()
                setButtonState(running = true)
            } else {
                Toast.makeText(this, "Permissions required for full functionality", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST = 100
    }
}

package com.ibrahimcanerdogan.facedetectionapp.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.controls.Facing
import com.ibrahimcanerdogan.facedetectionapp.graphic.GraphicOverlay
import java.io.OutputStream

/**
 * Manages the [CameraView] lifecycle, camera switching, and photo capture.
 *
 * On capture:
 *  1. Sets [FaceAnalyzer.isCapturing] = true  → next detection draw uses ORANGE rectangles.
 *  2. Takes a screenshot of the CameraView + GraphicOverlay merged into a Bitmap.
 *  3. Saves the Bitmap to the device gallery (MediaStore / Pictures folder).
 */
class CameraManager(
    private val context: Context,
    private val cameraView: CameraView,
    private val graphicOverlay: GraphicOverlay<*>,
    lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver {

    val faceAnalyzer: FaceAnalyzer = FaceAnalyzer(graphicOverlay)
    private var currentFacing: Facing = Facing.FRONT

    init {
        cameraView.setLifecycleOwner(lifecycleOwner)
        cameraView.addFrameProcessor(faceAnalyzer)

        cameraView.facing           = currentFacing
        faceAnalyzer.isFrontFacing  = true
        graphicOverlay.isFrontFacing = true

        cameraView.addCameraListener(object : CameraListener() {
            override fun onCameraError(exception: com.otaliastudios.cameraview.CameraException) {
                Log.e(TAG, "CameraView error: $exception")
            }
        })

        lifecycleOwner.lifecycle.addObserver(this)
    }

    // ─── Public controls ──────────────────────────────────────────────────────

    fun cameraStart() {
        if (!cameraView.isOpened) cameraView.open()
    }

    fun cameraStop() {
        cameraView.close()
        graphicOverlay.clear()
    }

    fun changeCamera() {
        currentFacing = if (currentFacing == Facing.FRONT) Facing.BACK else Facing.FRONT
        val isFront = currentFacing == Facing.FRONT
        cameraView.facing            = currentFacing
        faceAnalyzer.isFrontFacing   = isFront
        graphicOverlay.isFrontFacing = isFront
        graphicOverlay.clear()
    }

    /**
     * Captures the current frame with an ORANGE face rectangle overlay and saves
     * the merged bitmap to the device gallery.
     */
    fun capturePhoto() {
        // 1. Signal FaceAnalyzer to draw orange rectangles on the very next detection
        faceAnalyzer.isCapturing = true

        // 2. Give the overlay one frame to redraw in orange (~100 ms), then screenshot
        cameraView.postDelayed({
            try {
                val bitmap = mergeCameraAndOverlay()
                saveBitmapToGallery(bitmap)
                Toast.makeText(context, "📸 Photo saved to Gallery!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "capturePhoto failed: $e")
                Toast.makeText(context, "Capture failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, 120)
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Creates a Bitmap by drawing the CameraView preview + GraphicOverlay on top.
     * Both views must be laid out (non-zero size) for this to work.
     */
    private fun mergeCameraAndOverlay(): Bitmap {
        val w = cameraView.width.takeIf { it > 0 }
            ?: throw IllegalStateException("CameraView has zero width")
        val h = cameraView.height.takeIf { it > 0 }
            ?: throw IllegalStateException("CameraView has zero height")

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw camera preview layer
        cameraView.draw(canvas)
        // Draw overlay (face rectangles) on top
        graphicOverlay.draw(canvas)

        return bitmap
    }

    /**
     * Saves [bitmap] to the device's Pictures/FaceDetectionApp folder using MediaStore.
     * Works on Android 9 (API 28) and above.
     */
    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "FaceDetection_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/FaceDetectionApp")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("MediaStore insert returned null URI")

        var stream: OutputStream? = null
        try {
            stream = resolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open OutputStream for URI")
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        } finally {
            stream?.close()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onResume(owner: LifecycleOwner)  { cameraView.open() }
    override fun onPause(owner: LifecycleOwner)   { cameraView.close() }
    override fun onDestroy(owner: LifecycleOwner) {
        faceAnalyzer.release()
        cameraView.destroy()
    }

    companion object {
        private const val TAG = "CameraManager"
    }
}

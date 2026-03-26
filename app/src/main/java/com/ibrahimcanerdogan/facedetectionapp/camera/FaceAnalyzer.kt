package com.ibrahimcanerdogan.facedetectionapp.camera

import android.annotation.SuppressLint
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.frame.FrameProcessor
import com.ibrahimcanerdogan.facedetectionapp.graphic.GraphicOverlay
import com.ibrahimcanerdogan.facedetectionapp.graphic.RectangleOverlay

/**
 * A CameraView [FrameProcessor] that feeds every camera frame into MLKit Face Detection
 * and draws the results onto [graphicOverlay].
 *
 * isFrontFacing is managed externally by CameraManager.
 * isCapturing flag turns rectangles ORANGE for one capture cycle.
 */
class FaceAnalyzer(
    private val graphicOverlay: GraphicOverlay<*>
) : FrameProcessor {

    /** Updated by CameraManager whenever the camera facing changes. */
    @Volatile var isFrontFacing: Boolean = true

    /**
     * Set to true before taking a photo — next detection draw will use ORANGE rectangles.
     * Automatically resets to false after one draw cycle.
     */
    @Volatile var isCapturing: Boolean = false

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    @SuppressLint("UnsafeOptInUsageError")
    override fun process(frame: Frame) {
        val data     = frame.getData<ByteArray>() ?: return
        val width    = frame.size.width
        val height   = frame.size.height
        val rotation = frame.rotationToUser

        graphicOverlay.setCameraInfo(width, height, isFrontFacing)

        val inputImage = InputImage.fromByteArray(
            data, width, height, rotation,
            InputImage.IMAGE_FORMAT_NV21
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces: List<Face> ->
                onSuccess(faces, width, height)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed: $e")
            }
    }

    private fun onSuccess(faces: List<Face>, frameWidth: Int, frameHeight: Int) {
        // Snapshot & reset the capture flag atomically so only one frame gets orange
        val capturing = isCapturing
        if (capturing) isCapturing = false

        graphicOverlay.clear()
        faces.forEach { face ->
            val graphic = RectangleOverlay(
                overlay      = graphicOverlay,
                face         = face,
                frameWidth   = frameWidth,
                frameHeight  = frameHeight,
                isCapturing  = capturing
            )
            graphicOverlay.add(graphic)
        }
        graphicOverlay.postInvalidate()
    }

    fun release() {
        try { detector.close() } catch (e: Exception) { Log.e(TAG, "release: $e") }
    }

    companion object {
        private const val TAG = "FaceAnalyzer"
    }
}

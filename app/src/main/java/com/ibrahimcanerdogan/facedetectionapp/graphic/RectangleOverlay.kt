package com.ibrahimcanerdogan.facedetectionapp.graphic

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.mlkit.vision.face.Face
import com.ibrahimcanerdogan.facedetectionapp.utils.CameraUtils

/**
 * Draws a bounding rectangle around a detected face on the [GraphicOverlay].
 *
 * Color behaviour:
 *  - Normal live preview  → GREEN  (default)
 *  - Photo capture moment → ORANGE (set isCapturing = true before drawing)
 *
 * @param overlay       The overlay this graphic belongs to.
 * @param face          The MLKit [Face] object containing the bounding box.
 * @param frameWidth    Width of the camera frame (pixels) used for coordinate mapping.
 * @param frameHeight   Height of the camera frame (pixels) used for coordinate mapping.
 * @param isCapturing   When true the rectangle and label are drawn in orange.
 */
class RectangleOverlay(
    private val overlay: GraphicOverlay<*>,
    private val face: Face,
    private val frameWidth: Int,
    private val frameHeight: Int,
    private val isCapturing: Boolean = false
) : GraphicOverlay.Graphic(overlay) {

    private val rectColor  = if (isCapturing) Color.parseColor("#FF6600") else Color.GREEN
    private val labelColor = if (isCapturing) Color.parseColor("#FF6600") else Color.GREEN

    private val boxPaint = Paint().apply {
        color       = rectColor
        style       = Paint.Style.STROKE
        strokeWidth = if (isCapturing) 6.0f else 4.0f   // slightly thicker at capture
        isAntiAlias = true
    }

    private val labelPaint = Paint().apply {
        color          = labelColor
        textSize       = 36f
        isFakeBoldText = true
        isAntiAlias    = true
    }

    override fun draw(canvas: Canvas) {
        val rect: RectF = CameraUtils.calculateRect(
            overlay     = overlay,
            frameWidth  = frameWidth.toFloat(),
            frameHeight = frameHeight.toFloat(),
            boundingBox = face.boundingBox
        )
        canvas.drawRect(rect, boxPaint)

        // Draw tracking ID above the box
        face.trackingId?.let { id ->
            val label = if (isCapturing) "📸 ID: $id" else "ID: $id"
            canvas.drawText(label, rect.left, rect.top - 8f, labelPaint)
        }
    }
}

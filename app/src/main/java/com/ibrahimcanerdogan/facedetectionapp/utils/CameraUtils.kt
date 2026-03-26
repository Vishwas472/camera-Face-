package com.ibrahimcanerdogan.facedetectionapp.utils

import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.RectF
import com.ibrahimcanerdogan.facedetectionapp.graphic.GraphicOverlay
import kotlin.math.ceil

/**
 * Utility helpers for mapping MLKit bounding-box coordinates (which are relative to the
 * camera frame) onto the [GraphicOverlay] view coordinate system.
 *
 * Works with natario1/CameraView — no CameraX or play-services-vision dependency needed.
 */
object CameraUtils {

    /**
     * Maps an MLKit [boundingBox] (expressed in camera-frame coordinates) to a [RectF]
     * expressed in [GraphicOverlay] view coordinates, accounting for:
     *  - Portrait / Landscape orientation swap
     *  - Aspect-ratio scaling with centering offset
     *  - Front-camera horizontal mirroring
     *
     * @param overlay      The overlay view (used for its width/height and facing flag).
     * @param frameWidth   Width of the raw camera frame in pixels.
     * @param frameHeight  Height of the raw camera frame in pixels.
     * @param boundingBox  The [Rect] returned by [com.google.mlkit.vision.face.Face.getBoundingBox].
     */
    fun calculateRect(
        overlay: GraphicOverlay<*>,
        frameWidth: Float,
        frameHeight: Float,
        boundingBox: Rect
    ): RectF {
        val isLandscape =
            overlay.context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // In portrait mode the frame is rotated 90°, so width/height are swapped.
        val effectiveWidth  = if (isLandscape) frameWidth  else frameHeight
        val effectiveHeight = if (isLandscape) frameHeight else frameWidth

        val scaleX = overlay.width.toFloat()  / effectiveWidth
        val scaleY = overlay.height.toFloat() / effectiveHeight
        val scale  = maxOf(scaleX, scaleY)

        val offsetX = (overlay.width.toFloat()  - ceil(effectiveWidth  * scale)) / 2f
        val offsetY = (overlay.height.toFloat() - ceil(effectiveHeight * scale)) / 2f

        val mappedBox = RectF(
            boundingBox.right  * scale + offsetX,   // left  (mirrored — corrected below)
            boundingBox.top    * scale + offsetY,   // top
            boundingBox.left   * scale + offsetX,   // right (mirrored — corrected below)
            boundingBox.bottom * scale + offsetY    // bottom
        )

        // Mirror horizontally for front-facing camera
        if (overlay.isFrontFacing) {
            val centerX = overlay.width / 2f
            mappedBox.apply {
                val newLeft  = centerX + (centerX - left)
                val newRight = centerX - (right - centerX)
                left  = newLeft
                right = newRight
            }
        }

        return mappedBox
    }
}

package com.ibrahimcanerdogan.facedetectionapp.graphic

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

/**
 * A view which renders a series of custom graphics overlaid on the camera preview.
 * Supports scaling and mirroring relative to the camera preview properties.
 *
 * This version is completely independent — no dependency on play-services-vision CameraSource.
 * Facing is managed via a simple Boolean (isFrontFacing).
 */
class GraphicOverlay<T : GraphicOverlay.Graphic>(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs) {

    private val lock = Any()
    private var previewWidth: Int = 0
    private var widthScaleFactor = 1.0f
    private var previewHeight: Int = 0
    private var heightScaleFactor = 1.0f

    /** true = front camera (mirror horizontally), false = back camera */
    var isFrontFacing: Boolean = true
        set(value) {
            synchronized(lock) { field = value }
            postInvalidate()
        }

    private val graphics = HashSet<T>()

    // ─── Graphic base class ───────────────────────────────────────────────────

    abstract class Graphic(private val overlay: GraphicOverlay<*>) {

        abstract fun draw(canvas: Canvas)

        fun scaleX(horizontal: Float): Float = horizontal * overlay.widthScaleFactor
        fun scaleY(vertical: Float): Float   = vertical  * overlay.heightScaleFactor

        fun translateX(x: Float): Float =
            if (overlay.isFrontFacing) overlay.width - scaleX(x) else scaleX(x)

        fun translateY(y: Float): Float = scaleY(y)

        fun postInvalidate() = overlay.postInvalidate()
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun clear() {
        synchronized(lock) { graphics.clear() }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        @Suppress("UNCHECKED_CAST")
        synchronized(lock) { graphics.add(graphic as T) }
        postInvalidate()
    }

    fun remove(graphic: T) {
        synchronized(lock) { graphics.remove(graphic) }
        postInvalidate()
    }

    /**
     * Call this whenever the camera frame size is known (e.g. from CameraView's onFrameProcessed
     * or from the Frame object dimensions).
     */
    fun setCameraInfo(previewWidth: Int, previewHeight: Int, frontFacing: Boolean) {
        synchronized(lock) {
            this.previewWidth  = previewWidth
            this.previewHeight = previewHeight
            this.isFrontFacing = frontFacing
        }
        postInvalidate()
    }

    // ─── Drawing ──────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            if (previewWidth != 0 && previewHeight != 0) {
                widthScaleFactor  = canvas.width.toFloat()  / previewWidth.toFloat()
                heightScaleFactor = canvas.height.toFloat() / previewHeight.toFloat()
            }
            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}

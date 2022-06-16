package com.example.mlkitqrandbarcodescanner

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View

class Box internal constructor(context: Context?, attr: AttributeSet) :
    View(context, attr, 0) {

    private var bitmap: Bitmap? = null
    private lateinit var square: Rect

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (bitmap == null) {
            createWindowFrame()
        }
        bitmap?.let { canvas?.drawBitmap(it, 0f, 0f, null) }
    }

    protected fun createWindowFrame() {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val osCanvas = Canvas(bitmap!!)
        val outerRectangle = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = resources.getColor(R.color.cameraBackgroundColor)
        paint.alpha = 99
        osCanvas.drawRect(outerRectangle, paint)
        paint.color = Color.TRANSPARENT
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)

        val canvasW = width
        val canvasH = height
        val centerOfCanvas = Point(canvasW / 2, canvasH / 2)
        val rectW = (width / 1.3).toFloat()
        val rectH = (width / 1.3).toFloat()
        MainActivity.X = centerOfCanvas.x.toFloat()
        MainActivity.Y = centerOfCanvas.y.toFloat()
        val left = centerOfCanvas.x - rectW / 2
        val top = centerOfCanvas.y - rectH / 2
        val right = centerOfCanvas.x + rectW / 2
        val bottom = centerOfCanvas.y + rectH / 2

        square = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
//        MainActivity.LEFT = square.left.toFloat()
//        MainActivity.TOP = square.top.toFloat()
//        MainActivity.RIGHT = square.width().toFloat()
//        MainActivity.BOTTOM = square.height().toFloat()
        MainActivity.LEFT = left
        MainActivity.TOP = top
        MainActivity.RIGHT = right
        MainActivity.BOTTOM = bottom
        val w = width
        val h = height
        osCanvas.drawRect(
            square,
            paint
        )
    }

    fun getRect(): Rect {
        return square
    }

}


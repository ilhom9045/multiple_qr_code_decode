package com.example.mlkitqrandbarcodescanner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Size
import android.view.View
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.example.mlkitqrandbarcodescanner.util.getEnum
import com.example.mlkitqrandbarcodescanner.util.isPortrait
import com.example.mlkitqrandbarcodescanner.util.px
import kotlin.math.min

class ScannerOverlayImpl @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), BarCodeAndQRCodeAnalyser.ScannerOverlay {

    private val transparentPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    private val strokePaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            strokeWidth = context.px(3f)
            style = Paint.Style.STROKE
        }
    }

    var drawBlueRect : Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    fun drawGraphicBlocks(graphicBlocks : List<GraphicBlock>) {
        this.graphicBlocks = graphicBlocks
        drawBlueRect = true
    }

    var type: Type
    private var graphicBlocks : List<GraphicBlock>? = null

    private val blueColor = Color.BLUE


    init {
        setWillNotDraw(false)

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScannerOverlayImpl, 0, 0)
        type = typedArray.getEnum(R.styleable.ScannerOverlayImpl_type, Type.SEPAQR)
        typedArray.recycle()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#88000000"))

        val radius = context.px(4f)
        val rectF = scanRect
        canvas.drawRoundRect(rectF, radius, radius, transparentPaint)
        strokePaint.color = if(drawBlueRect) blueColor else Color.RED
        canvas.drawRoundRect(rectF, radius, radius, strokePaint)

        graphicBlocks?.forEach { block ->
            val scaleX = scanRect.width() / block.bitmapSize.width
            val scaleY = scanRect.height() / block.bitmapSize.height

            canvas.withTranslation(scanRect.left, scanRect.top) {
                withScale(scaleX, scaleY) {
                    drawRoundRect(RectF(block.textBlock.boundingBox), radius, radius, strokePaint)
                }
            }
        }
        graphicBlocks = null
    }

    override val size: Size
        get() = Size(width, height)

    override val scanRect: RectF
        get() = when (type) {
            Type.IBAN -> {
                if(context.isPortrait()) {
                    val rectW = min(width * 0.95f, MAX_WIDTH_PORTRAIT)
                    val l = (width - rectW) / 2
                    val r = width - l
                    val t = height * 0.2f
                    val b = t + rectW / getIBANOverlayHeightFactor()
                    RectF(l, t, r, b)
                } else {
                    val rectW = min(width * 0.6f, MAX_WIDTH_LANDSCAPE)
                    val l = width * 0.05f
                    val r = l + rectW
                    val t = height * 0.15f
                    val b = t + rectW / getIBANOverlayHeightFactor()
                    RectF(l, t, r, b)
                }
            }
            Type.ID -> {
                if(context.isPortrait()) {
                    val rectW = min(width * 0.95f, MAX_WIDTH_PORTRAIT)
                    val l = (width - rectW) / 2
                    val r = width - l
                    val rectH = rectW / 1.5f
                    val t = height * 0.15f
                    val b = t + rectH
                    RectF(l, t, r, b)
                } else {
                    val rectW = min(width * 0.4f, MAX_WIDTH_LANDSCAPE)
                    val l = width * 0.05f
                    val r = l + rectW
                    val rectH = rectW / 1.5f
                    val t = height * 0.05f
                    val b = t + rectH
                    RectF(l, t, r, b)
                }
            }
            Type.SEPAQR -> {
                if(context.isPortrait()) {
                    val size = min(width * 0.72f, MAX_WIDTH_PORTRAIT)
                    val l = (width - size) / 2
                    val r = width - l
                    val t = height * 0.25f
                    val b = t + size
                    RectF(l, t, r, b)
                } else {
                    val size = min(width * 0.25f, MAX_WIDTH_LANDSCAPE)
                    val l = width * 0.05f
                    val r = l + size
                    val t = height * 0.05f
                    val b = t + size
                    RectF(l, t, r, b)
                }
            }
        }

    private fun getIBANOverlayHeightFactor(): Byte {
        return 7
//        return 10
    }


    enum class Type {
        IBAN,
        ID,
        SEPAQR
    }

    data class GraphicBlock(val textBlock: BlockWrapper, val bitmapSize: Size)

    companion object {
        const val MAX_WIDTH_PORTRAIT = 1200f
        const val MAX_WIDTH_LANDSCAPE = 1600f
    }
}
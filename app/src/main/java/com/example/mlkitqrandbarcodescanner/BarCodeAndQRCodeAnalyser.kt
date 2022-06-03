package com.example.mlkitqrandbarcodescanner

import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.ImageFormat.*
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class BarCodeAndQRCodeAnalyser(
    private val rect: Rect,
    private val listener: QRCodeFoundListener
) :
    ImageAnalysis.Analyzer {

    constructor(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        listener: QRCodeFoundListener
    ) : this(rect = Rect(x, y, width, height), listener = listener)

    private val hints = Hashtable<DecodeHintType, Any>()
    val decodeFormats = Vector<BarcodeFormat>().apply {
        add(BarcodeFormat.QR_CODE)
    }

    private val multiQRCodeReader = QRCodeMultiReader()

    init {
        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats;
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        if (image.format === YUV_420_888 || image.format === YUV_422_888 || image.format === YUV_444_888) {
            image.image?.toBitmap()?.let {
                    decode(it)
//                cropBitmap(bitmap = it, rect)?.let {
//                }
            }
        }
        image.close()
    }

    fun decode(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = PlanarYUVLuminanceSource(
            convertRGBToYuv(pixels, ByteArray(width * height), width, height),
            bitmap.width,
            bitmap.height,
            0,
            0,
            bitmap.width,
            bitmap.height,
            false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            val result: Array<Result> = multiQRCodeReader.decodeMultiple(binaryBitmap, hints)
            if (result.isNotEmpty()) {
                if (result.size == 1) {
                    listener.onQRCodeFound(result[0])
                } else {
                    listener.onManyQRCodeFound(result)
                }
            }
        } catch (e: FormatException) {
            listener.qrCodeNotFound()
        } catch (e: ChecksumException) {
            listener.qrCodeNotFound()
        } catch (e: NotFoundException) {
            listener.qrCodeNotFound()
        }
    }

    fun decodeFormRect(data: ByteArray, rect: Rect, width: Int, height: Int) {
        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            rect.width(),
            rect.height(),
            false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            val result: Array<Result> = multiQRCodeReader.decodeMultiple(binaryBitmap, hints)
            if (result.isNotEmpty()) {
                if (result.size == 1) {
                    listener.onQRCodeFound(result[0])
                } else {
                    listener.onManyQRCodeFound(result)
                }
            }
        } catch (e: FormatException) {
            listener.qrCodeNotFound()
        } catch (e: ChecksumException) {
            listener.qrCodeNotFound()
        } catch (e: NotFoundException) {
            listener.qrCodeNotFound()
        }
    }


    fun scaleCenterCrop(
        source: Bitmap, newHeight: Int,
        newWidth: Int
    ): Bitmap? {
        val sourceWidth = source.width
        val sourceHeight = source.height
        val xScale = newWidth.toFloat() / sourceWidth
        val yScale = newHeight.toFloat() / sourceHeight
        val scale = xScale.coerceAtLeast(yScale)

        // Now get the size of the source bitmap when scaled
        val scaledWidth = scale * sourceWidth
        val scaledHeight = scale * sourceHeight
        val left = (newWidth - scaledWidth) / 2
        val top = (newHeight - scaledHeight) / 2
        val targetRect = RectF(
            left, top, left + scaledWidth, top
                    + scaledHeight
        ) //from ww w  .j a va 2s. co m
        val dest = Bitmap.createBitmap(
            newWidth, newHeight,
            source.config
        )
        val canvas = Canvas(dest)
        canvas.drawBitmap(source, null, targetRect, null)
        return dest
    }

    private fun convertRGBToYuv(
        rgb: IntArray,
        yuv420sp: ByteArray,
        width: Int,
        height: Int
    ): ByteArray? {
        for (i in 0 until width * height) {
            val red = (rgb[i] shr 16 and 0xff).toFloat()
            val green = (rgb[i] shr 8 and 0xff).toFloat()
            val blue = (rgb[i] and 0xff).toFloat()
            val luminance = (0.257f * red + 0.504f * green + 0.098f * blue + 16).toInt()
            yuv420sp[i] = (0xff and luminance).toByte()
        }
        return yuv420sp
    }

    private fun decodeFromRGB(
        data: IntArray,
        width: Int,
        height: Int,
        hintTypeMap: Map<DecodeHintType, *>
    ): Result? {

        val source = RGBLuminanceSource(width, height, data)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            return MultiFormatReader().decode(binaryBitmap, hintTypeMap)
        } catch (e: NotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun ImageProxy.convertImageProxyToBitmap(): Bitmap? {
        return try {
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun Image.toBitmap(): Bitmap? {
        return try {
            val yBuffer = planes[0].buffer // Y
            val vuBuffer = planes[2].buffer // VU

            val ySize = yBuffer.remaining()
            val vuSize = vuBuffer.remaining()

            val nv21 = ByteArray(ySize + vuSize)

            yBuffer.get(nv21, 0, ySize)
            vuBuffer.get(nv21, ySize, vuSize)

            val yuvImage = YuvImage(nv21, NV21, this.width, this.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun decodeFrame(data: ByteArray, rect: Rect, width: Int, height: Int): LuminanceSource {
        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            rect.width(),
            rect.height(),
            false
        )
        return source
    }

    fun cropBitmap(bitmap: Bitmap, rect: Rect): Bitmap? {
        return try {
            val subImage = Bitmap.createBitmap(
                rect.width(),
                rect.height(), Bitmap.Config.ARGB_8888
            )
            val c = Canvas(subImage)
            c.drawBitmap(
                bitmap, rect,
                Rect(0, 0, rect.width(), rect.height()), null
            )

            val file = File("/data/data/${BuildConfig.APPLICATION_ID}/cache", "qrCode.png")

            try {
                FileOutputStream(file).use { out ->
                    subImage.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        out
                    )
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return subImage
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
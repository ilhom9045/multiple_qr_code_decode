package com.example.mlkitqrandbarcodescanner

import android.graphics.ImageFormat.*
import android.graphics.Point
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import java.nio.ByteBuffer
import java.util.*


class BarCodeAndQRCodeAnalyser(
    private val left: Int,
    private val top: Int,
    private val right: Int,
    private val bottom: Int,
    private val listener: QRCodeFoundListener
) :
    ImageAnalysis.Analyzer {

    private val hints = Hashtable<DecodeHintType, Any>()
    val decodeFormats = Vector<BarcodeFormat>().apply {
        add(BarcodeFormat.QR_CODE)
    }

    init {
        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats;
    }

    override fun analyze(image: ImageProxy) {
        if (image.format === YUV_420_888 || image.format === YUV_422_888 || image.format === YUV_444_888) {
            val byteBuffer: ByteBuffer = image.planes[0].buffer
            val imageData = ByteArray(byteBuffer.capacity())
            byteBuffer.get(imageData)

            val source = PlanarYUVLuminanceSource(
                imageData,
                image.width, image.width,
                left, top, right, bottom,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                val result: Array<Result> = QRCodeMultiReader().decodeMultiple(binaryBitmap, hints)
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
        image.close()
    }
}
package com.example.mlkitqrandbarcodescanner

import android.R.attr
import android.graphics.*
import android.util.Size
import androidx.lifecycle.MutableLiveData
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import java.nio.ByteBuffer
import java.util.*


class BarCodeAndQRCodeAnalyser(
    private val scannerOverlay: ScannerOverlay,
    private val listener: QRCodeFoundListener,
    private val lifeData: MutableLiveData<Bitmap>
) : BaseAnalyser(scannerOverlay) {

    private val hints = Hashtable<DecodeHintType, Any>()
    val decodeFormats = Vector<BarcodeFormat>().apply {
        add(BarcodeFormat.QR_CODE)
    }

    private val multiQRCodeReader = QRCodeMultiReader()

    init {
        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats;
    }

//    @SuppressLint("UnsafeOptInUsageError")
//    override fun analyze(imageProxy: ImageProxy) {
//        val mediaImage = imageProxy.image
//        val rotation = imageProxy.imageInfo.rotationDegrees
//        val scannerRect =
//            getScannerRectToPreviewViewRelation(Size(imageProxy.width, imageProxy.height), rotation)
//
//        val image = imageProxy.image!!
//        val cropRect = image.getCropRectAccordingToRotation(scannerRect, rotation)
//        image.cropRect = cropRect
//        val byteArray = YuvNV21Util.yuv420toNV21(image)
//        val bitmap = BitmapUtil.getBitmap(
//            byteArray,
//            FrameMetadata(cropRect.width(), cropRect.height(), rotation)
//        )
//        imageProxy.close()
////        val data = imageProxy.planes[0].buffer.toByteArray()
////        decode(data, imageProxy)
////        imageProxy.close()
//    }

//    fun decode(byteArray: ByteArray, imageProxy: ImageProxy) {
//        val source = PlanarYUVLuminanceSource(
//            byteArray,
//            imageProxy.width,
//            imageProxy.height,
//            0,
//            0,
//            imageProxy.width,
//            imageProxy.height,
//            false
//        )
//        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
//        try {
//            val result: Array<Result> = multiQRCodeReader.decodeMultiple(binaryBitmap, hints)
//            if (result.isNotEmpty()) {
//                if (result.size == 1) {
//                    listener.onQRCodeFound(result[0])
//                } else {
//                    listener.onManyQRCodeFound(result)
//                }
//            }
//        } catch (e: FormatException) {
//            listener.qrCodeNotFound()
//        } catch (e: ChecksumException) {
//            listener.qrCodeNotFound()
//        } catch (e: NotFoundException) {
//            listener.qrCodeNotFound()
//        }
//    }

    override fun onBitmapPrepared(bitmap: Bitmap) {
        lifeData.postValue(bitmap)
        decode(bitmap)
    }

    fun decode(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = PlanarYUVLuminanceSource(
            convertRGBToYuv(pixels, ByteArray(width * height), width, height),
            width,
            height,
            0,
            0,
            width,
            height,
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


    interface ScannerOverlay {

        val size: Size

        val scanRect: RectF
    }
}
package com.example.mlkitqrandbarcodescanner


import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mlkitqrandbarcodescanner.databinding.ActivityMainBinding
import com.google.zxing.Result
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


// This is an array of all the permission specified in the manifest.
val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
const val RATIO_4_3_VALUE = 4.0 / 3.0
const val RATIO_16_9_VALUE = 16.0 / 9.0

class MainActivity : AppCompatActivity(), QRCodeFoundListener {

    private companion object {
        var LEFT = 0f
        var TOP = 0f
        var RIGHT = 0f
        var BOTTOM = 0f
    }


    private val executor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private lateinit var box: Box

    private val multiPermissionCallback =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
            if (map.entries.size < 1) {
                Toast.makeText(this, "Please Accept all the permissions", Toast.LENGTH_SHORT).show()
            } else {
                binding.viewFinder.post {
                    startCamera()
                }
            }
        }

    private lateinit var rootView: View
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraInfo: CameraInfo
    private lateinit var cameraControl: CameraControl
    private lateinit var adapter: BarcodeRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        rootView = window.decorView.rootView
        binding.clearText.setOnClickListener {
//            val myClipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//            val myClip = ClipData.newPlainText("barcode data", adapter.items.toString())
//            myClipboard.setPrimaryClip(myClip)
//            Toast.makeText(this, "Text copied to cliboard", Toast.LENGTH_SHORT).show()
            adapter.clear()
        }
        adapter = BarcodeRecyclerViewAdapter()
        binding.BarcodeValue.layoutManager = LinearLayoutManager(this)
        binding.BarcodeValue.adapter = adapter
        // Request camera permissions
        multiPermissionCallback.launch(
            REQUIRED_PERMISSIONS
        )
        if (allPermissionsGranted()) {
            binding.viewFinder.post {
                //Initialize graphics overlay
                startCamera()
            }
        } else {
            requestAllPermissions()
        }
    }


    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { binding.viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = binding.viewFinder.display.rotation

        // Bind the CameraProvider to the LifeCycleOwner
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            // CameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

            draw_preview_rectangle()
            // ImageAnalysis
            val textBarcodeAnalyzer = initializeAnalyzer(screenAspectRatio, rotation)
            cameraProvider.unbindAll()

            try {
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, textBarcodeAnalyzer
                )
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
                cameraControl.setLinearZoom(0.5f)

            } catch (exc: Exception) {
                exc.printStackTrace()
                //Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun requestAllPermissions() {
        multiPermissionCallback.launch(
            REQUIRED_PERMISSIONS
        )
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun initializeAnalyzer(screenAspectRatio: Int, rotation: Int): UseCase {
        return ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .build()
            .also {

                it.setAnalyzer(
                    executor, BarCodeAndQRCodeAnalyser(
                        LEFT.toInt(), TOP.toInt(),
                        RIGHT.toInt(), BOTTOM.toInt(), this
                    )
                )
            }
    }

    override fun onQRCodeFound(qrCode: Result) {
        runOnUiThread {
            adapter.clear()
            adapter.addData(qrCode.text)
        }
    }

    override fun onManyQRCodeFound(qrCodes: Array<Result>) {
        Log.d("onBarcodeDetected", qrCodes.toString())
        runOnUiThread {
            adapter.clear()
            qrCodes.forEach { barcode ->
                barcode.toString().let { raw ->
                    adapter.addData(raw)
                }
            }
        }
    }

    override fun qrCodeNotFound() {
    }

    private fun draw_preview_rectangle() {
        box = Box(this)
        box.layoutParams = ViewGroup.LayoutParams(
            binding.viewFinder.width,
            binding.viewFinder.height
        )
        addContentView(
            box, ViewGroup.LayoutParams(
                binding.viewFinder.width,
                binding.viewFinder.height
            )
        )
    }

    class Box internal constructor(context: Context?) : LinearLayout(context) {
        private var bitmap: Bitmap? = null

        override fun dispatchDraw(canvas: Canvas) {
            super.dispatchDraw(canvas)
            if (bitmap == null) {
                createWindowFrame()
            }
            bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
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

            LEFT = centerOfCanvas.x - rectW / 2
            TOP = centerOfCanvas.y - rectH / 2
            RIGHT = centerOfCanvas.x + rectW / 2
            BOTTOM = centerOfCanvas.y + rectH / 2

            val square = Rect(LEFT.toInt(), TOP.toInt(), RIGHT.toInt(), BOTTOM.toInt())
            LEFT = square.left.toFloat()
            TOP = square.top.toFloat()
            RIGHT = square.width().toFloat()
            BOTTOM = square.height().toFloat()

            osCanvas.drawRect(
                square,
                paint
            )
        }
    }
}

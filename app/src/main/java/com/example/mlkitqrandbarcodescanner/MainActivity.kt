package com.example.mlkitqrandbarcodescanner


import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.util.*
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.Result
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


// This is an array of all the permission specified in the manifest.
val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
const val RATIO_4_3_VALUE = 4.0 / 3.0
const val RATIO_16_9_VALUE = 16.0 / 9.0
private const val TARGET_PREVIEW_WIDTH = 960
private const val TARGET_PREVIEW_HEIGHT = 1280

class MainActivity : AppCompatActivity(), QRCodeFoundListener {

    companion object {
        var X = 0f
        var Y = 0f
        var LEFT = 0f
        var TOP = 0f
        var RIGHT = 0f
        var BOTTOM = 0f
    }

    private val executor by lazy {
        Executors.newSingleThreadExecutor()
    }


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

    private val liveData = MutableLiveData<Bitmap>()

    private lateinit var rootView: View
    private lateinit var binding: com.example.mlkitqrandbarcodescanner.databinding.ActivityMainBinding
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
        binding.olActScanner.type = ScannerOverlayImpl.Type.SEPAQR
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
        liveData.observe(this) {
            if (it != null) {
                binding.preViewImageView.setImageBitmap(it)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetResolution(Size(TARGET_PREVIEW_WIDTH, TARGET_PREVIEW_HEIGHT))
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(TARGET_PREVIEW_WIDTH, TARGET_PREVIEW_HEIGHT))
                .build()
                .also {
                    it.setAnalyzer(executor, BarCodeAndQRCodeAnalyser(binding.olActScanner, this,liveData))
                }

            // Select back camera
            val cameraSelector = CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera =
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

            } catch (e: Exception) {
                e.printStackTrace()
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

    private fun initializeAnalyzer(size: Int): UseCase {
        binding.olActScanner.type = ScannerOverlayImpl.Type.SEPAQR
        return ImageAnalysis.Builder()
            .setTargetRotation(size)
            .build()
            .also {
                it.setAnalyzer(
                    executor, BarCodeAndQRCodeAnalyser(
                        binding.olActScanner,
                        this,
                        liveData
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
        Log.d("notFound", "")
    }
}

val Number.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

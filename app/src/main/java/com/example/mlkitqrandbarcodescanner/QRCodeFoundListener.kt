package com.example.mlkitqrandbarcodescanner

import com.google.zxing.Result

interface QRCodeFoundListener {
    fun onQRCodeFound(qrCode: Result)
    fun onManyQRCodeFound(qrCodes: Array<Result>)
    fun qrCodeNotFound()
}
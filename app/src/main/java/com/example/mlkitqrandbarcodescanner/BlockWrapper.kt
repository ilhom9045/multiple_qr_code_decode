package com.example.mlkitqrandbarcodescanner

import android.graphics.Rect

class BlockWrapper(gmsTextBLock: Box) {

    val boundingBox: Rect = gmsTextBLock.getRect()
    val text: String = ""
    val lines: List<String> = emptyList()
}
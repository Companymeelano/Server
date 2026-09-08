package com.meelano.builder.util

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

fun qrBitmap(text: String, size: Int = 512): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size) {
        bmp.setPixel(x, y, if (m[x, y]) 0xFF170B16.toInt() else 0xFFFFFFFF.toInt())
    }
    return bmp
}

@Composable
fun QrImage(text: String, modifier: Modifier = Modifier) {
    val bmp = remember(text) { runCatching { qrBitmap(text) }.getOrNull() }
    if (bmp != null) Image(bmp.asImageBitmap(), "QR", modifier)
}

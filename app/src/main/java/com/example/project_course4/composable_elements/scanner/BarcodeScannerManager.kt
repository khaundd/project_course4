package com.example.project_course4.composable_elements.scanner

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class BarcodeScannerManager(private val context: Context) {

    // опции: EAN_13 и автозум
    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_EAN_13)
        .enableAutoZoom()
        .build()

    private val scanner = GmsBarcodeScanning.getClient(context, options)

    fun startScanning(onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.displayValue?.let {
                    onResult(it)
                }
            }
            .addOnFailureListener { e ->
                Log.e("BarcodeScanner", "Ошибка сканирования: ${e.message}")
                val message = e.message ?: e.javaClass.simpleName
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                onError(e)
            }
            .addOnCanceledListener {
                Log.d("BarcodeScanner", "Сканирование отменено пользователем")
            }
    }
}
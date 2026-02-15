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
                    Toast.makeText(context, "Штрих-код - $it", Toast.LENGTH_LONG).show()
                    onResult(it)
                }
            }
            .addOnFailureListener { e ->
                Log.e("BarcodeScanner", "Ошибка сканирования: ${e.message}")
                Toast.makeText(context, "Ошибка сканера", Toast.LENGTH_SHORT).show()
            }
            .addOnCanceledListener {
                Log.d("BarcodeScanner", "Сканирование отменено пользователем")
            }
        //TODO onError
    }
}
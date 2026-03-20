package com.example.project_course4.composable_elements.scanner

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class BarcodeScannerManager(private val context: Context) {

    // enableAutoZoom() требует версию модуля 16.1.0+, которая может отсутствовать на Android 9
    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_EAN_13)
        .apply {
            // enableAutoZoom может вызывать сбой на старых версиях Google Play Services
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                enableAutoZoom()
            }
        }
        .build()

    private val scanner = GmsBarcodeScanning.getClient(context, options)

    fun startScanning(onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        val moduleInstallClient = ModuleInstall.getClient(context)

        moduleInstallClient.areModulesAvailable(scanner)
            .addOnSuccessListener { response ->
                if (response.areModulesAvailable()) {
                    doScan(onResult, onError)
                } else {
                    // Модуль не установлен — запрашиваем установку
                    Log.d("BarcodeScanner", "Модуль сканера не установлен, запрашиваем установку")
                    val installRequest = ModuleInstallRequest.newBuilder()
                        .addApi(scanner)
                        .build()
                    moduleInstallClient.installModules(installRequest)
                        .addOnSuccessListener {
                            Log.d("BarcodeScanner", "Модуль установлен, запускаем сканирование")
                            doScan(onResult, onError)
                        }
                        .addOnFailureListener { e ->
                            Log.e("BarcodeScanner", "Ошибка установки модуля: ${e.message}")
                            onError(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                // Не удалось проверить — пробуем сканировать напрямую
                Log.w("BarcodeScanner", "Не удалось проверить модуль, пробуем сканировать: ${e.message}")
                doScan(onResult, onError)
            }
    }

    private fun doScan(onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.displayValue?.let { onResult(it) }
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

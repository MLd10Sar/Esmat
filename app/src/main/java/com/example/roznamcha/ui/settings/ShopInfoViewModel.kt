package com.example.roznamcha.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roznamcha.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ShopInfoViewModel(application: Application) : AndroidViewModel(application) {

    fun saveShopInfo(name: String, address: String, phone: String) {
        val context = getApplication<Application>().applicationContext
        SettingsManager.saveShopName(context, name)
        SettingsManager.saveShopAddress(context, address)
        SettingsManager.saveShopPhone(context, phone)
    }

    fun saveShopLogo(imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val file = File(context.filesDir, "shop_logo.png")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            // The logo is now saved in the app's private storage
        }
    }
}
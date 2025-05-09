package com.example.roznamcha.ui.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.roznamcha.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _backupStatus = MutableLiveData<Boolean>()
    val backupStatus: LiveData<Boolean> = _backupStatus

    private val _restoreStatus = MutableLiveData<Boolean>()
    val restoreStatus: LiveData<Boolean> = _restoreStatus

    fun backupDatabase(destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            // Get the database instance
            val db = AppDatabase.getDatabase(context)
            // Get the actual path of the database file on the device
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME) // You need to expose DB_NAME from AppDatabase

            // Close the database to ensure all data is written to the main file (-wal checkpoint)
            db.close()

            var success = false
            try {
                // Use ContentResolver to write to the location the user chose
                context.contentResolver.openFileDescriptor(destinationUri, "w")?.use { descriptor ->
                    FileOutputStream(descriptor.fileDescriptor).use { outputStream ->
                        FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                            success = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Backup failed", e)
                success = false
            } finally {
                // Re-open the database for the app to continue working
                AppDatabase.getDatabase(context)
                _backupStatus.postValue(success)
            }
        }
    }

    fun restoreDatabase(backupUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val db = AppDatabase.getDatabase(context)
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)

            // Close the database to release file locks
            db.close()

            var success = false
            try {
                // Use ContentResolver to read from the backup file the user chose
                context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                    FileOutputStream(dbFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                        success = true
                    }
                }
                // Optional: Restore the -wal and -shm files if they exist in a more complex backup
                // For this simple copy, we are just replacing the main .db file.
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Restore failed", e)
                success = false
            } finally {
                // IMPORTANT: We do not re-open the database here.
                // We will restart the app, which will force Room to re-initialize
                // from the newly copied database file.
                _restoreStatus.postValue(success)
            }
        }
    }
}
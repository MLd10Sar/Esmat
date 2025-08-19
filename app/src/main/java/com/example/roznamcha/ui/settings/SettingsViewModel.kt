package com.example.roznamcha.ui.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.*
import com.example.roznamcha.AppDatabase
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.utils.CryptoManager
import com.example.roznamcha.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _encryptedBackupReadyEvent = MutableLiveData<Event<Uri>>()
    val encryptedBackupReadyEvent: LiveData<Event<Uri>> = _encryptedBackupReadyEvent

    private val _backupFailedEvent = MutableLiveData<Event<Unit>>()
    val backupFailedEvent: LiveData<Event<Unit>> = _backupFailedEvent

    private val _restoreStatusEvent = MutableLiveData<Event<Boolean>>()
    val restoreStatusEvent: LiveData<Event<Boolean>> = _restoreStatusEvent

    private val cryptoManager = CryptoManager()


    fun createEncryptedBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val encryptedFile = File(context.cacheDir, "roznamcha_backup.db.enc")

            try {
                // <<< STEP 1: CLOSE and NULLIFY the instance >>>
                AppDatabase.closeInstance()

                // Now that the DB is closed, we can safely copy and encrypt it
                cryptoManager.encrypt(dbFile, encryptedFile)

                val authority = "${context.packageName}.provider"
                val fileUri = FileProvider.getUriForFile(context, authority, encryptedFile)

                _encryptedBackupReadyEvent.postValue(Event(fileUri))
                SettingsManager.saveLastBackupTimestamp(context, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e("BackupRestore", "Backup creation failed", e)
                _backupFailedEvent.postValue(Event(Unit))
            }
        }
    }

    fun restoreDatabase(sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val tempEncryptedFile = File(context.cacheDir, "backup_to_restore.db.enc")

            try {
                // <<< STEP 1: CLOSE and NULLIFY the current database instance >>>
                // This is CRITICAL to release the file lock before overwriting.
                AppDatabase.closeInstance()

                // Step 2: Copy the user-selected file to a temporary location
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    tempEncryptedFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Step 3: Decrypt the temporary file, overwriting the original DB file
                cryptoManager.decrypt(tempEncryptedFile, dbFile)

                Log.d("BackupRestore", "Database file successfully restored.")
                tempEncryptedFile.delete() // Clean up
                _restoreStatusEvent.postValue(Event(true)) // Signal success
            } catch (e: Exception) {
                Log.e("BackupRestore", "Restore failed", e)
                tempEncryptedFile.delete()
                _restoreStatusEvent.postValue(Event(false))
            }
        }
    }
}
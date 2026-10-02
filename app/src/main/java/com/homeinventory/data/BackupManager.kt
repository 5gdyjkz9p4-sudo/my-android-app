package com.homeinventory.data
​import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
​data class BackupData(
val version: Int = 1,
val folders: List<FolderEntity>,
val items: List<ItemEntity>
)
​class BackupManager(
private val context: Context,
private val dao: InventoryDao
) {
private val gson = Gson()
​suspend fun exportData(uri: Uri): Boolean = withContext(Dispatchers.IO) {
try {
val data = BackupData(
folders = dao.getAllFolders(),
items = dao.getAllItems()
)
val json = gson.toJson(data)
​val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
outputStream.use {
it.write(json.toByteArray())
}
​true
} catch (e: Exception) {
e.printStackTrace()
false
}
}
​suspend fun importData(uri: Uri): Boolean = withContext(Dispatchers.IO) {
try {
val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
val json = inputStream.use { stream ->
stream.bufferedReader().readText()
}
val data = gson.fromJson(json, BackupData::class.java) ?: return@withContext false
​val database = AppDatabase.getDatabase(context)
database.withTransaction {
dao.clearFolders()
dao.clearItems()
dao.insertFolders(data.folders)
dao.insertItems(data.items)
}
​true
} catch (e: Exception) {
e.printStackTrace()
false
}
}
}
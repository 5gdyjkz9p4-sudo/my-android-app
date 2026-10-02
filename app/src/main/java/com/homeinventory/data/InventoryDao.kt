package com.homeinventory.data
​import androidx.room.*
import kotlinx.coroutines.flow.Flow
​@Dao
interface InventoryDao {
@Query("SELECT * FROM folders WHERE parentFolderId IS :parentId")
fun getFoldersIn(parentId: String?): Flow<List<FolderEntity>>
​@Query("SELECT * FROM folders WHERE parentFolderId = :parentId")
suspend fun getFoldersInSync(parentId: String): List<FolderEntity>
​@Query("SELECT * FROM folders WHERE parentFolderId IS NULL")
suspend fun getRootFoldersSync(): List<FolderEntity>
​@Query("SELECT * FROM items WHERE parentFolderId IS :parentId")
fun getItemsIn(parentId: String?): Flow<List<ItemEntity>>
​@Query("SELECT * FROM items WHERE parentFolderId = :parentId")
suspend fun getItemsInSync(parentId: String): List<ItemEntity>
​@Query("SELECT * FROM folders WHERE id = :id")
suspend fun getFolderById(id: String): FolderEntity?
​@Query("SELECT * FROM items WHERE id = :id")
suspend fun getItemById(id: String): ItemEntity?
​@Query("SELECT * FROM folders")
suspend fun getAllFolders(): List<FolderEntity>
​@Query("SELECT * FROM items")
suspend fun getAllItems(): List<ItemEntity>
​@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertFolder(folder: FolderEntity)
​@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertItem(item: ItemEntity)
​@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertFolders(folders: List<FolderEntity>)
​@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertItems(items: List<ItemEntity>)
​@Query("UPDATE items SET quantity = :qty, updatedTime = :time WHERE id = :id")
suspend fun updateQuantity(id: String, qty: Int, time: Long)
​@Query("DELETE FROM folders WHERE id = :id")
suspend fun deleteFolder(id: String)
​@Query("DELETE FROM items WHERE id = :id")
suspend fun deleteItem(id: String)
​@Query("DELETE FROM folders")
suspend fun clearFolders()
​@Query("DELETE FROM items")
suspend fun clearItems()
​@Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%'")
suspend fun searchItems(query: String): List<ItemEntity>
}
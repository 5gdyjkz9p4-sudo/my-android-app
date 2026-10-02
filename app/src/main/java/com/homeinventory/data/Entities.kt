package com.homeinventory.data
​import androidx.room.Entity
import androidx.room.PrimaryKey
​@Entity(tableName = "folders")
data class FolderEntity(
@PrimaryKey val id: String,
val name: String,
val parentFolderId: String?,
val createdTime: Long = System.currentTimeMillis(),
val updatedTime: Long = System.currentTimeMillis(),
val sortOrder: Int = 0
)
​@Entity(tableName = "items")
data class ItemEntity(
@PrimaryKey val id: String,
val name: String,
val parentFolderId: String?,
val quantity: Int = 0,
val unit: String = "个",
val lowStockThreshold: Int = 0,
val createdTime: Long = System.currentTimeMillis(),
val updatedTime: Long = System.currentTimeMillis(),
val sortOrder: Int = 0,
val note: String = ""
)
​data class FolderSummary(
val folder: FolderEntity,
val itemCount: Int,
val lowStockCount: Int,
val outOfStockCount: Int
)
​data class ItemWithFolderPath(
val item: ItemEntity,
val folderPath: String
)
​enum class SortType { CUSTOM, NAME, CREATE_TIME, UPDATE_TIME, QUANTITY, LOW_STOCK }
​data class SortOption(val type: SortType = SortType.NAME, val isAscending: Boolean = true)
​// 用于 UI 统一混合渲染和拖拽排序
sealed class DisplayNode(open val id: String, open val sortOrder: Int) {
data class FolderNode(val summary: FolderSummary) : DisplayNode(summary.folder.id, summary.folder.sortOrder)
data class ItemNode(val item: ItemEntity) : DisplayNode(item.id, item.sortOrder)
}
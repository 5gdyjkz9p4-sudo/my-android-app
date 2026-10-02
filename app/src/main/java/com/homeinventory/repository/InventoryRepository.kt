package com.homeinventory.repository

import androidx.room.withTransaction
import com.homeinventory.data.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class InventoryRepository(
    private val dao: InventoryDao,
    private val db: AppDatabase
) {

    fun getFoldersIn(parentId: String?): Flow<List<FolderEntity>> =
        dao.getFoldersIn(parentId)

    fun getItemsIn(parentId: String?): Flow<List<ItemEntity>> =
        dao.getItemsIn(parentId)

    suspend fun getItemById(id: String): ItemEntity? =
        dao.getItemById(id)

    suspend fun getFolderById(id: String): FolderEntity? =
        dao.getFolderById(id)

    suspend fun saveItem(item: ItemEntity) =
        dao.insertItem(item)

    suspend fun saveFolder(folder: FolderEntity) =
        dao.insertFolder(folder)

    suspend fun updateQuantity(id: String, delta: Int) {
        val item = dao.getItemById(id) ?: return
        val newQty = (item.quantity + delta).coerceAtLeast(0)
        dao.updateQuantity(id, newQty, System.currentTimeMillis())
    }

    suspend fun getBreadcrumbPath(folderId: String?): List<FolderEntity> {
        val path = mutableListOf<FolderEntity>()
        var currentId = folderId
        val visited = mutableSetOf<String>()

        while (currentId != null && visited.add(currentId)) {
            val folder = dao.getFolderById(currentId) ?: break
            path.add(0, folder)
            currentId = folder.parentFolderId
        }

        return path
    }

    suspend fun getFolderSummary(folder: FolderEntity): FolderSummary {
        val items = dao.getItemsInSync(folder.id)

        return FolderSummary(
            folder = folder,
            itemCount = items.size,
            lowStockCount = items.count {
                it.quantity > 0 && it.quantity <= it.lowStockThreshold
            },
            outOfStockCount = items.count { it.quantity == 0 }
        )
    }

    suspend fun deleteItem(id: String) =
        dao.deleteItem(id)

    suspend fun deleteFolderRecursively(id: String) {
        db.withTransaction {
            deleteFolderInternal(id)
        }
    }

    private suspend fun deleteFolderInternal(id: String) {
        // 1. 删除当前文件夹下的所有直属物品
        dao.getItemsInSync(id).forEach { item ->
            deleteItem(item.id)
        }

        // 2. 递归删除所有子文件夹及其内部物品
        dao.getFoldersInSync(id).forEach { subFolder ->
            deleteFolderInternal(subFolder.id)
        }

        // 3. 最后删除当前文件夹本身
        dao.deleteFolder(id)
    }

    suspend fun moveItem(itemId: String, targetFolderId: String?) {
        val item = dao.getItemById(itemId) ?: return

        if (targetFolderId != null) {
            dao.getFolderById(targetFolderId) ?: return
        }

        dao.insertItem(
            item.copy(
                parentFolderId = targetFolderId,
                updatedTime = System.currentTimeMillis()
            )
        )
    }

    suspend fun moveFolder(
        folderId: String,
        targetFolderId: String?
    ): Boolean {
        if (folderId == targetFolderId) return false

        val folder = dao.getFolderById(folderId) ?: return false

        if (targetFolderId != null) {
            dao.getFolderById(targetFolderId) ?: return false
            val targetPath = getBreadcrumbPath(targetFolderId)
            if (targetPath.any { it.id == folderId }) return false
        }

        dao.insertFolder(
            folder.copy(
                parentFolderId = targetFolderId,
                updatedTime = System.currentTimeMillis()
            )
        )

        return true
    }

    suspend fun copyItem(itemId: String, targetFolderId: String?) {
        val item = dao.getItemById(itemId) ?: return

        dao.insertItem(
            item.copy(
                id = UUID.randomUUID().toString(),
                parentFolderId = targetFolderId,
                createdTime = System.currentTimeMillis(),
                updatedTime = System.currentTimeMillis()
            )
        )
    }

    suspend fun copyFolderRecursively(
        folderId: String,
        targetFolderId: String?
    ) {
        db.withTransaction {
            copyFolderInternal(folderId, targetFolderId)
        }
    }

    private suspend fun copyFolderInternal(
        folderId: String,
        targetFolderId: String?
    ) {
        val folder = dao.getFolderById(folderId) ?: return

        // 1. 生成新文件夹的唯一 ID 并写入数据库
        val newFolderId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        dao.insertFolder(
            folder.copy(
                id = newFolderId,
                parentFolderId = targetFolderId,
                createdTime = now,
                updatedTime = now
            )
        )

        // 2. 将当前文件夹下的直属物品生成新 UUID 并放入新文件夹
        dao.getItemsInSync(folderId).forEach { item ->
            copyItem(item.id, newFolderId)
        }

        // 3. 递归复制子文件夹树，将其父文件夹 ID 正确指向 newFolderId
        dao.getFoldersInSync(folderId).forEach { subFolder ->
            copyFolderInternal(subFolder.id, newFolderId)
        }
    }

    suspend fun searchItemsWithPath(
        query: String
    ): List<ItemWithFolderPath> {
        val items = dao.searchItems(query)

        return items.map { item ->
            val pathStr = getBreadcrumbPath(item.parentFolderId)
                .joinToString(" / ") { it.name }
                .ifEmpty { "我的库存" }

            ItemWithFolderPath(item, pathStr)
        }
    }

    suspend fun getRootFoldersSync(): List<FolderEntity> =
        dao.getRootFoldersSync()

    suspend fun getFoldersInSync(parentId: String): List<FolderEntity> =
        dao.getFoldersInSync(parentId)
}

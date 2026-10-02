package com.homeinventory.viewmodel
​import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homeinventory.data.*
import com.homeinventory.repository.InventoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
​@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {
​private val _currentFolderId = MutableStateFlow<String?>(null)
val currentFolderId = _currentFolderId.asStateFlow()
​private val _breadcrumbs = MutableStateFlow<List<FolderEntity>>(emptyList())
val breadcrumbs = _breadcrumbs.asStateFlow()
​private val _sortOption = MutableStateFlow(SortOption())
val sortOption = _sortOption.asStateFlow()
​private val _moveResult = MutableStateFlow<String?>(null)
val moveResult = _moveResult.asStateFlow()
​// 统一混合渲染的流：监听当前目录及 Room Flow 的持续变化
val displayNodes = _currentFolderId
.flatMapLatest { parentId ->
combine(
repository.getFoldersIn(parentId),
repository.getItemsIn(parentId)
) { rawFolders, rawItems ->
val summaries = rawFolders.map { repository.getFolderSummary(it) }
val nodes = mutableListOf<DisplayNode>()
nodes.addAll(summaries.map { DisplayNode.FolderNode(it) })
nodes.addAll(rawItems.map { DisplayNode.ItemNode(it) })
nodes
}
}
.combine(_sortOption) { nodes, option ->
nodes.sortedWith(getNodeComparator(option))
}
.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
​private val _searchResults = MutableStateFlow<List<ItemWithFolderPath>>(emptyList())
val searchResults = _searchResults.asStateFlow()
​fun navigateTo(folderId: String?) {
_currentFolderId.value = folderId
viewModelScope.launch { _breadcrumbs.value = repository.getBreadcrumbPath(folderId) }
}
​fun adjustQuantity(itemId: String, delta: Int) {
viewModelScope.launch {
repository.updateQuantity(itemId, delta)
// 刷新搜索结果中展示的库存
val query = currentSearchQuery
if (query.isNotBlank()) search(query)
}
}
​fun createFolder(name: String) = viewModelScope.launch {
val maxOrder = displayNodes.value.maxOfOrNull { it.sortOrder } ?: -1
repository.saveFolder(
FolderEntity(
id = UUID.randomUUID().toString(),
name = name,
parentFolderId = _currentFolderId.value,
sortOrder = maxOrder + 1
)
)
}
​fun createItem(
name: String,
quantity: Int,
unit: String,
lowStockThreshold: Int,
note: String
) = viewModelScope.launch {
val maxOrder = displayNodes.value.maxOfOrNull { it.sortOrder } ?: -1
repository.saveItem(
ItemEntity(
id = UUID.randomUUID().toString(),
name = name,
parentFolderId = _currentFolderId.value,
quantity = quantity.coerceAtLeast(0),
unit = unit,
lowStockThreshold = lowStockThreshold.coerceAtLeast(0),
note = note,
sortOrder = maxOrder + 1
)
)
}
​fun updateItem(item: ItemEntity) = viewModelScope.launch {
repository.saveItem(
item.copy(
quantity = item.quantity.coerceAtLeast(0),
lowStockThreshold = item.lowStockThreshold.coerceAtLeast(0)
)
)
}
​fun deleteItem(id: String) = viewModelScope.launch { repository.deleteItem(id) }
fun deleteFolder(id: String) = viewModelScope.launch { repository.deleteFolderRecursively(id) }
​fun moveItem(itemId: String, targetId: String?) = viewModelScope.launch {
repository.moveItem(itemId, targetId)
_moveResult.value = "移动成功"
}
​fun moveFolder(folderId: String, targetId: String?) = viewModelScope.launch {
val success = repository.moveFolder(folderId, targetId)
if (success) {
_moveResult.value = "移动成功"
} else {
_moveResult.value = "移动失败：不能移动到自身或其子文件夹"
}
}
​fun clearMoveResult() {
_moveResult.value = null
}
​fun copyItem(itemId: String, targetId: String?) = viewModelScope.launch { repository.copyItem(itemId, targetId) }
fun copyFolder(folderId: String, targetId: String?) = viewModelScope.launch { repository.copyFolderRecursively(folderId, targetId) }
​private var sortUpdateJob: Job? = null
​fun updateMixedSortOrders(orderedNodes: List<DisplayNode>) {
sortUpdateJob?.cancel()
​sortUpdateJob = viewModelScope.launch {
delay(300)
​orderedNodes.forEachIndexed { index, node ->
when (node) {
is DisplayNode.FolderNode -> repository.saveFolder(
node.summary.folder.copy(sortOrder = index)
)
​is DisplayNode.ItemNode -> repository.saveItem(
node.item.copy(sortOrder = index)
)
}
}
}
}
​fun setSortType(type: SortType) {
val curr = _sortOption.value
_sortOption.value = if (curr.type == type) {
curr.copy(isAscending = !curr.isAscending)
} else {
SortOption(type, true)
}
}
​private var currentSearchQuery = ""
fun search(query: String) = viewModelScope.launch {
currentSearchQuery = query
if (query.isBlank()) {
_searchResults.value = emptyList()
} else {
_searchResults.value = repository.searchItemsWithPath(query)
}
}
​suspend fun getItemById(id: String): ItemEntity? = repository.getItemById(id)
suspend fun getRootFoldersSync(): List<FolderEntity> = repository.getRootFoldersSync()
suspend fun getFoldersInSync(id: String): List<FolderEntity> = repository.getFoldersInSync(id)
​private fun getNodeComparator(
opt: SortOption
): Comparator<DisplayNode> {
val cmp = Comparator<DisplayNode> { a, b ->
if (opt.type == SortType.CUSTOM) {
return@Comparator a.sortOrder.compareTo(b.sortOrder)
}
​val nameA = if (a is DisplayNode.FolderNode) a.summary.folder.name else (a as DisplayNode.ItemNode).item.name
val nameB = if (b is DisplayNode.FolderNode) b.summary.folder.name else (b as DisplayNode.ItemNode).item.name
​when (opt.type) {
SortType.NAME -> nameA.compareTo(nameB)
SortType.CREATE_TIME -> {
val tA = if (a is DisplayNode.FolderNode) a.summary.folder.createdTime else (a as DisplayNode.ItemNode).item.createdTime
val tB = if (b is DisplayNode.FolderNode) b.summary.folder.createdTime else (b as DisplayNode.ItemNode).item.createdTime
tA.compareTo(tB)
}
SortType.UPDATE_TIME -> {
val tA = if (a is DisplayNode.FolderNode) a.summary.folder.updatedTime else (a as DisplayNode.ItemNode).item.updatedTime
val tB = if (b is DisplayNode.FolderNode) b.summary.folder.updatedTime else (b as DisplayNode.ItemNode).item.updatedTime
tA.compareTo(tB)
}
SortType.QUANTITY -> {
val qA = if (a is DisplayNode.FolderNode) a.summary.itemCount else (a as DisplayNode.ItemNode).item.quantity
val qB = if (b is DisplayNode.FolderNode) b.summary.itemCount else (b as DisplayNode.ItemNode).item.quantity
qA.compareTo(qB)
}
SortType.LOW_STOCK -> {
val lowA = if (a is DisplayNode.FolderNode) {
a.summary.lowStockCount > 0
} else {
val i = (a as DisplayNode.ItemNode).item
i.quantity <= i.lowStockThreshold && i.quantity > 0
}
val lowB = if (b is DisplayNode.FolderNode) {
b.summary.lowStockCount > 0
} else {
val i = (b as DisplayNode.ItemNode).item
i.quantity <= i.lowStockThreshold && i.quantity > 0
}
lowA.compareTo(lowB)
}
else -> 0
}
}
return if (opt.isAscending) {
cmp
} else {
cmp.reversed()
}
}
}
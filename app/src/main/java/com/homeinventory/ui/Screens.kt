package com.homeinventory.ui
​import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homeinventory.data.DisplayNode
import com.homeinventory.data.ItemEntity
import com.homeinventory.data.SortType
import com.homeinventory.viewmodel.InventoryViewModel
​@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(viewModel: InventoryViewModel, onNavigateItem: (String) -> Unit, onNavigateSearch: () -> Unit) {
val displayNodes by viewModel.displayNodes.collectAsState()
val breadcrumbs by viewModel.breadcrumbs.collectAsState()
val sortOption by viewModel.sortOption.collectAsState()
​val moveResult by viewModel.moveResult.collectAsState()
val snackbarHostState = remember { SnackbarHostState() }
​LaunchedEffect(moveResult) {
moveResult?.let { msg ->
snackbarHostState.showSnackbar(msg)
viewModel.clearMoveResult()
}
}
​var showCreateDialog by remember { mutableStateOf(false) }
var createType by remember { mutableStateOf("Folder") }
var itemName by remember { mutableStateOf("") }
var itemQty by remember { mutableStateOf("0") }
​var selectedItemId by remember { mutableStateOf<String?>(null) }
var selectedFolderId by remember { mutableStateOf<String?>(null) }
var showActionSheet by remember { mutableStateOf(false) }
var showTreeSelector by remember { mutableStateOf(false) }
​val listState = rememberLazyListState()
val isSortMode = sortOption.type == SortType.CUSTOM
​// UI 层独立维护一份可拖拽列表，拖拽完成后同步回 DB
var localNodes by remember(displayNodes) { mutableStateOf(displayNodes) }
​val dragDropState = rememberDragDropState(listState) { from, to ->
if (isSortMode) {
val mutList = localNodes.toMutableList()
mutList.add(to, mutList.removeAt(from))
localNodes = mutList
viewModel.updateMixedSortOrders(mutList)
}
}
​Scaffold(
snackbarHost = { SnackbarHost(snackbarHostState) },
topBar = {
TopAppBar(
title = {
Row(modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
TextButton(onClick = { viewModel.navigateTo(null) }) { Text("我的库存", fontSize = 16.sp) }
breadcrumbs.forEach { b ->
Text("/")
TextButton(onClick = { viewModel.navigateTo(b.id) }) { Text(b.name, fontSize = 16.sp) }
}
}
},
actions = {
IconButton(onClick = onNavigateSearch) { Icon(Icons.Default.Search, "搜索") }
var menuExpanded by remember { mutableStateOf(false) }
IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.Sort, "排序") }
DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
SortType.values().forEach { t ->
DropdownMenuItem(
text = { Text(t.name) },
onClick = { viewModel.setSortType(t); menuExpanded = false }
)
}
}
}
)
},
floatingActionButton = {
var fabExpanded by remember { mutableStateOf(false) }
Column(horizontalAlignment = Alignment.End) {
if (fabExpanded) {
ExtendedFloatingActionButton(
onClick = { createType = "Folder"; showCreateDialog = true; fabExpanded = false },
modifier = Modifier.padding(bottom = 8.dp)
) { Text("新建文件夹") }
ExtendedFloatingActionButton(
onClick = { createType = "Item"; showCreateDialog = true; fabExpanded = false },
modifier = Modifier.padding(bottom = 8.dp)
) { Text("新建商品") }
}
FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) { Icon(Icons.Default.Add, "新建") }
}
}
) { padding ->
LazyColumn(
state = listState,
modifier = Modifier.fillMaxSize().padding(padding).then(if (isSortMode) Modifier.dragContainer(dragDropState) else Modifier)
) {
items(localNodes, key = { it.id }) { node ->
when (node) {
is DisplayNode.FolderNode -> {
FolderRow(
summary = node.summary,
onClick = { viewModel.navigateTo(node.summary.folder.id) },
onLongClick = {
selectedItemId = null
selectedFolderId = node.summary.folder.id
showActionSheet = true
}
)
}
is DisplayNode.ItemNode -> {
ItemRow(
item = node.item,
onClick = { onNavigateItem(node.item.id) },
onQtyChange = { delta -> viewModel.adjustQuantity(node.item.id, delta) },
onLongClick = {
selectedFolderId = null
selectedItemId = node.item.id
showActionSheet = true
}
)
}
}
}
}
}
​if (showCreateDialog) {
AlertDialog(
onDismissRequest = { showCreateDialog = false },
title = { Text("新建" + if (createType == "Folder") "文件夹" else "商品") },
text = {
Column {
OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("名称") })
if (createType == "Item") {
OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("数量") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}
}
},
confirmButton = {
TextButton(onClick = {
if (createType == "Folder") viewModel.createFolder(itemName)
else viewModel.createItem(itemName, itemQty.toIntOrNull() ?: 0, "个", 0, "")
itemName = ""; itemQty = "0"; showCreateDialog = false
}) { Text("保存") }
}
)
}
​if (showActionSheet) {
AlertDialog(
onDismissRequest = { showActionSheet = false; selectedItemId = null; selectedFolderId = null },
title = { Text("操作") },
text = {
Column {
TextButton(onClick = { showTreeSelector = true; showActionSheet = false }) { Text("移动到...") }
TextButton(onClick = {
if (selectedItemId != null) {
viewModel.copyItem(selectedItemId!!, viewModel.currentFolderId.value)
}
if (selectedFolderId != null) {
viewModel.copyFolder(selectedFolderId!!, viewModel.currentFolderId.value)
}
showActionSheet = false
selectedItemId = null
selectedFolderId = null
}) { Text("在此处复制一份") }
TextButton(onClick = {
if (selectedItemId != null) {
viewModel.deleteItem(selectedItemId!!)
}
if (selectedFolderId != null) {
viewModel.deleteFolder(selectedFolderId!!)
}
showActionSheet = false
selectedItemId = null
selectedFolderId = null
}) { Text("删除", color = Color.Red) }
}
},
confirmButton = {}
)
}
​if (showTreeSelector) {
TreeSelectorDialog(
viewModel,
onDismiss = {
showTreeSelector = false
selectedItemId = null
selectedFolderId = null
},
onSelect = { targetId ->
if (selectedItemId != null) {
viewModel.moveItem(selectedItemId!!, targetId)
}
if (selectedFolderId != null) {
viewModel.moveFolder(selectedFolderId!!, targetId)
}
showTreeSelector = false
selectedItemId = null
selectedFolderId = null
}
)
}
}
​@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(itemId: String, viewModel: InventoryViewModel, onBack: () -> Unit) {
var item by remember { mutableStateOf<ItemEntity?>(null) }
LaunchedEffect(itemId) { item = viewModel.getItemById(itemId) }
​if (item == null) return
​var name by remember { mutableStateOf(item!!.name) }
var qty by remember { mutableStateOf(item!!.quantity.toString()) }
var unit by remember { mutableStateOf(item!!.unit) }
var lowStock by remember { mutableStateOf(item!!.lowStockThreshold.toString()) }
var note by remember { mutableStateOf(item!!.note) }
​Scaffold(
topBar = { TopAppBar(title = { Text("编辑商品详情") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }) },
floatingActionButton = {
FloatingActionButton(onClick = {
viewModel.updateItem(item!!.copy(
name = name, quantity = qty.toIntOrNull() ?: 0, unit = unit,
lowStockThreshold = lowStock.toIntOrNull() ?: 0, note = note, updatedTime = System.currentTimeMillis()
))
onBack()
}) { Icon(Icons.Default.Save, "保存") }
}
) { padding ->
Column(modifier = Modifier.padding(padding).padding(16.dp)) {
OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("商品名称") }, modifier = Modifier.fillMaxWidth())
Spacer(Modifier.height(8.dp))
OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("当前库存数量") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
Spacer(Modifier.height(8.dp))
OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("单位 (如: 个, 瓶, kg)") }, modifier = Modifier.fillMaxWidth())
Spacer(Modifier.height(8.dp))
OutlinedTextField(value = lowStock, onValueChange = { lowStock = it }, label = { Text("最低库存警报线") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
Spacer(Modifier.height(8.dp))
OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注信息") }, modifier = Modifier.fillMaxWidth())
}
}
}
​@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: InventoryViewModel, onNavigateItem: (String) -> Unit, onBack: () -> Unit) {
var query by remember { mutableStateOf("") }
val results by viewModel.searchResults.collectAsState()
​LaunchedEffect(query) { viewModel.search(query) }
​Scaffold(
topBar = {
TopAppBar(
title = { TextField(value = query, onValueChange = { query = it }, placeholder = { Text("搜索商品名称或备注...") }, colors = TextFieldDefaults.textFieldColors(containerColor = Color.Transparent)) },
navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }
)
}
) { padding ->
LazyColumn(modifier = Modifier.padding(padding)) {
items(results, key = { it.item.id }) { res ->
Card(
modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onNavigateItem(res.item.id) },
colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
) {
Column(modifier = Modifier.padding(12.dp)) {
Row(verticalAlignment = Alignment.CenterVertically) {
Text(res.item.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 16.sp)
IconButton(onClick = { viewModel.adjustQuantity(res.item.id, -1) }) { Icon(Icons.Default.Remove, "减少") }
Text("${res.item.quantity}${res.item.unit}", fontWeight = FontWeight.Bold)
IconButton(onClick = { viewModel.adjustQuantity(res.item.id, 1) }) { Icon(Icons.Default.Add, "增加") }
}
Text("📂 位于: ${res.folderPath}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
}
}
}
}
}
}
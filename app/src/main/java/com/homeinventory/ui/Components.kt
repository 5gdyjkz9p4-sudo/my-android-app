package com.homeinventory.ui
​import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homeinventory.data.FolderEntity
import com.homeinventory.data.FolderSummary
import com.homeinventory.data.ItemEntity
import com.homeinventory.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch
​@Composable
fun FolderRow(summary: FolderSummary, onClick: () -> Unit, onLongClick: () -> Unit) {
Card(
modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
) {
Row(
modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
verticalAlignment = Alignment.CenterVertically
) {
Text("📁", fontSize = 24.sp)
Spacer(modifier = Modifier.width(16.dp))
Column(modifier = Modifier.weight(1f)) {
Text(summary.folder.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
val info = mutableListOf("${summary.itemCount} 项")
if (summary.lowStockCount > 0) info.add("⚠ ${summary.lowStockCount} 低库存")
if (summary.outOfStockCount > 0) info.add("❌ ${summary.outOfStockCount} 缺货")
Text(info.joinToString(" · "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
IconButton(onClick = onLongClick) { Icon(Icons.Default.MoreVert, "更多") }
}
}
}
​@Composable
fun ItemRow(item: ItemEntity, onClick: () -> Unit, onQtyChange: (Int) -> Unit, onLongClick: () -> Unit) {
val isOut = item.quantity == 0
val isLow = !isOut && item.quantity <= item.lowStockThreshold
Card(
modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
) {
Row(
modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
verticalAlignment = Alignment.CenterVertically
) {
Column(modifier = Modifier.weight(1f)) {
Row(verticalAlignment = Alignment.CenterVertically) {
Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
if (isOut) Text("  缺货", color = Color.Red, fontSize = 12.sp)
else if (isLow) Text("  低库存", color = Color(0xFFFB8C00), fontSize = 12.sp)
}
if (item.note.isNotBlank()) Text(item.note, fontSize = 12.sp, maxLines = 1)
}
Row(verticalAlignment = Alignment.CenterVertically) {
IconButton(onClick = { onQtyChange(-1) }) { Icon(Icons.Default.Remove, "减少") }
Text("${item.quantity}${item.unit}", fontWeight = FontWeight.Bold, color = if (isOut) Color.Red else Color.Unspecified)
IconButton(onClick = { onQtyChange(1) }) { Icon(Icons.Default.Add, "增加") }
IconButton(onClick = onLongClick) { Icon(Icons.Default.MoreVert, "更多") }
}
}
}
}
​@Composable
fun TreeSelectorDialog(viewModel: InventoryViewModel, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
var expandedFolders by remember { mutableStateOf(setOf<String>()) }
val coroutineScope = rememberCoroutineScope()
var rootFolders by remember { mutableStateOf<List<FolderEntity>>(emptyList()) }
​LaunchedEffect(Unit) { rootFolders = viewModel.getRootFoldersSync() }
​AlertDialog(
onDismissRequest = onDismiss,
title = { Text("选择目标位置") },
text = {
@Composable fun FolderNode(folder: FolderEntity, depth: Int) {
var children by remember(folder.id) { mutableStateOf<List<FolderEntity>>(emptyList()) }
val isExpanded = expandedFolders.contains(folder.id)
Row(
modifier = Modifier.fillMaxWidth().padding(start = (depth * 16).dp).clickable { onSelect(folder.id) },
verticalAlignment = Alignment.CenterVertically
) {
IconButton(onClick = {
if (isExpanded) expandedFolders -= folder.id
else {
expandedFolders += folder.id
coroutineScope.launch { children = viewModel.getFoldersInSync(folder.id) }
}
}) {
Icon(if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight, null)
}
Text("📁 ${folder.name}")
}
if (isExpanded) { children.forEach { FolderNode(it, depth + 1) } }
}
​LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
item {
Text("🏠 我的库存 (根目录)", modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onSelect(null) }, fontWeight = FontWeight.Bold)
}
items(rootFolders.size) { i -> FolderNode(rootFolders[i], 0) }
}
},
confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
)
}
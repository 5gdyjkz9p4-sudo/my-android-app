package com.homeinventory

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.homeinventory.data.AppDatabase
import com.homeinventory.data.BackupManager
import com.homeinventory.repository.InventoryRepository
import com.homeinventory.ui.FolderScreen
import com.homeinventory.ui.ItemDetailScreen
import com.homeinventory.ui.SearchScreen
import com.homeinventory.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var backupManager: BackupManager

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val success = backupManager.exportData(uri)
                val msg = if (success) "导出备份成功" else "导出失败"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val success = backupManager.importData(uri)
                val msg = if (success) "导入成功，完整结构已恢复" else "导入失败"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val repository = InventoryRepository(
            dao = database.inventoryDao(),
            db = database
        )
        backupManager = BackupManager(this, database.inventoryDao())

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InventoryViewModel(repository) as T
            }
        }

        setContent {
            AppTheme {
                val navController = rememberNavController()
                val viewModel: InventoryViewModel = viewModel(factory = factory)

                Scaffold(
                    bottomBar = {
                        BottomAppBar {
                            TextButton(onClick = { exportLauncher.launch("inventory_backup.json") }) { Text("导出备份") }
                            TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("导入备份") }
                        }
                    }
                ) { innerPadding ->
                    NavHost(navController, startDestination = "folder", modifier = Modifier.padding(innerPadding)) {
                        composable("folder") {
                            FolderScreen(
                                viewModel = viewModel,
                                onNavigateItem = { itemId -> navController.navigate("item/$itemId") },
                                onNavigateSearch = { navController.navigate("search") }
                            )
                        }
                        composable("item/{itemId}") { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getString("itemId")
                            if (itemId != null) {
                                ItemDetailScreen(itemId = itemId, viewModel = viewModel, onBack = { navController.popBackStack() })
                            }
                        }
                        composable("search") {
                            SearchScreen(
                                viewModel = viewModel,
                                onNavigateItem = { itemId -> navController.navigate("item/$itemId") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

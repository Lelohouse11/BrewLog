package com.example.brewlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.brewlog.data.BrewLog
import com.example.brewlog.ui.AddBrewScreen
import com.example.brewlog.ui.BrewViewModel
import com.example.brewlog.ui.FilterBottomSheet
import com.example.brewlog.ui.theme.BrewLogTheme
import kotlinx.coroutines.launch

/**
 * Main entry point for the BrewLog application.
 * This activity handles navigation and the root UI container.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize the Splash Screen API for a smooth startup transition
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Use Edge-to-Edge to allow content to draw behind system bars (Status/Navigation)
        enableEdgeToEdge()
        
        setContent {
            BrewLogTheme {
                val viewModel: BrewViewModel = viewModel()
                val navController = rememberNavController()

                // Surface ensures a consistent theme-based background color behind all navigation transitions.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToAddBrew = { navController.navigate("add_brew") },
                                onNavigateToEditBrew = { id -> navController.navigate("edit_brew/$id") }
                            )
                        }
                        composable("add_brew") {
                            AddBrewScreen(
                                onSave = { log ->
                                    viewModel.addLog(log)
                                    navController.popBackStack()
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "edit_brew/{logId}",
                            arguments = listOf(navArgument("logId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val logId = backStackEntry.arguments?.getInt("logId")
                            val logs by viewModel.allLogs.collectAsState()
                            val log = logs.find { it.id == logId }
                            if (log != null) {
                                AddBrewScreen(
                                    existingLog = log,
                                    onSave = { updatedLog ->
                                        viewModel.updateLog(updatedLog)
                                        navController.popBackStack()
                                    },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BrewViewModel,
    onNavigateToAddBrew: () -> Unit,
    onNavigateToEditBrew: (Int) -> Unit
) {
    val logs by viewModel.filteredLogs.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var expandedLogId by remember { mutableStateOf<Int?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // File selection for Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    // File creation for Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = viewModel.exportData()
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Brew Settings") },
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Import")
                    }
                    IconButton(onClick = { exportLauncher.launch("brew_settings.json") }) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Export")
                    }
                    IconButton(onClick = { showFilters = true }) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddBrew) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Brew Setting")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Persistent Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search coffee or roaster...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (logs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isEmpty()) "No brew settings logged yet." else "No matching results.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        BrewLogItem(
                            log = log,
                            isExpanded = expandedLogId == log.id,
                            onToggleExpand = {
                                expandedLogId = if (expandedLogId == log.id) null else log.id
                            },
                            onDelete = { viewModel.deleteLog(log) },
                            onEdit = { onNavigateToEditBrew(log.id) }
                        )
                    }
                }
            }
        }

        if (showFilters) {
            ModalBottomSheet(
                onDismissRequest = { showFilters = false },
                sheetState = sheetState
            ) {
                FilterBottomSheet(
                    sortOption = sortOption,
                    filterState = filterState,
                    onSortChange = { viewModel.updateSort(it) },
                    onFilterChange = { viewModel.updateFilter(it) },
                    onReset = { viewModel.resetFilters() }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrewLogItem(
    log: BrewLog,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Collapsed Header: Name, Roaster, Rating, Grams Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = log.coffeeName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (log.roaster.isNotEmpty()) {
                        Text(
                            text = log.roaster,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = buildString {
                            val settings = mutableListOf<String>()
                            // Only show basket info if grams are > 0
                            if (log.singleGrams > 0) settings.add("Single: ${log.singleGrams}g")
                            if (log.doubleGrams > 0) settings.add("Double: ${log.doubleGrams}g")
                            append(settings.joinToString(", "))
                            append(" | Grind: ${log.grindSize}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Only show rating bubble if it was entered
                if (log.hasRating) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "${log.rating}/10",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expanded Content
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    
                    // Roast Info
                    if (log.roastLevel.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            InfoBit("Roast Level", log.roastLevel)
                        }
                    }

                    // Conditional Blend Composition
                    if (log.hasBlendSettings) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Blend Composition", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = buildString {
                                val components = mutableListOf<String>()
                                if (log.arabicaPercentage > 0) components.add("Arabica: ${log.arabicaPercentage}%")
                                if (log.robustaPercentage > 0) components.add("Robusta: ${log.robustaPercentage}%")
                                if (log.excelsaPercentage > 0) components.add("Excelsa: ${log.excelsaPercentage}%")
                                if (log.libericaPercentage > 0) components.add("Liberica: ${log.libericaPercentage}%")
                                append(components.joinToString(", "))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Conditional Sensory Profile
                    if (log.hasSensoryProfile) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sensory Profile", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        SensoryBar("Sweetness", log.sweetness)
                        SensoryBar("Acidity", log.acidity)
                        SensoryBar("Body", log.body)
                        SensoryBar("Bitterness", log.bitterness)
                    }

                    // Conditional Flavor Tags
                    if (log.hasFlavorTags && log.flavorTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Flavor Tags", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        FlowRow(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            log.flavorTags.forEach { tag ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoBit(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SensoryBar(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { value / 5f },
            modifier = Modifier.weight(1f).height(6.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

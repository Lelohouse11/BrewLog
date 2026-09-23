package com.example.brewlog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.brewlog.R
import com.example.brewlog.data.BrewLog
import com.example.brewlog.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BrewViewModel,
    onNavigateToAddBrew: () -> Unit,
    onNavigateToEditBrew: (Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val logs by viewModel.filteredLogs.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var expandedLogId by remember { mutableStateOf<Int?>(null) }
    val showFilters = remember { mutableStateOf(false) }
    val showMenu = remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

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
            Column {
                SearchBar(
                    expanded = isSearchActive,
                    onExpandedChange = { isSearchActive = it },
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onSearch = { isSearchActive = false },
                            expanded = isSearchActive,
                            onExpandedChange = { isSearchActive = it },
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            leadingIcon = {
                                if (isSearchActive) {
                                    IconButton(onClick = { isSearchActive = false }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                }
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    if (!isSearchActive) {
                                        IconButton(onClick = { showFilters.value = true }) {
                                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                                        }
                                        IconButton(onClick = onNavigateToSettings) {
                                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                                        }
                                        Box {
                                            IconButton(onClick = { showMenu.value = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                                            }
                                            DropdownMenu(
                                                expanded = showMenu.value,
                                                onDismissRequest = { showMenu.value = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.import_json)) },
                                                    leadingIcon = { Icon(Icons.Default.FileDownload, null) },
                                                    onClick = {
                                                        showMenu.value = false
                                                        importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.export_json)) },
                                                    leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                                                    onClick = {
                                                        showMenu.value = false
                                                        exportLauncher.launch("brew_settings.json")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isSearchActive) 0.dp else 16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(logs, key = { "search_${it.id}" }) { log ->
                            BrewLogItem(
                                log = log,
                                isExpanded = false,
                                onToggleExpand = {
                                    viewModel.updateSearchQuery(log.coffeeName)
                                    isSearchActive = false
                                },
                                onDelete = { viewModel.deleteLog(log) },
                                onEdit = { onNavigateToEditBrew(log.id) }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddBrew,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Brew Setting")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CoffeeEmptyState(
                        title = stringResource(R.string.no_brews_found)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp)
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

        if (showFilters.value) {
            ModalBottomSheet(
                onDismissRequest = { showFilters.value = false },
                sheetState = sheetState
            ) {
                FilterBottomSheet(
                    sortOption = sortOption,
                    filterState = filterState,
                    onSortChange = { viewModel.updateSort(it) },
                    onFilterChange = { viewModel.updateFilter(it) },
                    onReset = { viewModel.resetFilters() },
                    onApply = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showFilters.value = false
                        }
                    }
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
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "rotation")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Collapsed Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.coffeeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (log.roaster.isNotEmpty()) {
                        Text(
                            text = log.roaster,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Quick Stats Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickStat(Icons.Default.Settings, "${stringResource(R.string.grind_size)} ${log.grindSize}")
                        if (log.singleGrams > 0 || log.doubleGrams > 0) {
                            val grams = if (log.doubleGrams > 0) log.doubleGrams else log.singleGrams
                            QuickStat(Icons.Default.Scale, "${grams}g")
                        }
                        if (log.roastLevel.isNotEmpty()) {
                            RoastLevelBadge(roastLevel = log.roastLevel)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (log.hasRating) {
                        CoffeeBeanRating(
                            rating = log.rating,
                            beanSize = 14.dp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.rotate(rotation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanded Content
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Conditional Blend Composition
                    if (log.hasBlendSettings) {
                        CardSection(title = "Blend Composition") {
                            BlendCompositionBar(
                                arabica = log.arabicaPercentage,
                                robusta = log.robustaPercentage,
                                excelsa = log.excelsaPercentage,
                                liberica = log.libericaPercentage
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Conditional Sensory Profile
                    if (log.hasSensoryProfile) {
                        CardSection(title = stringResource(R.string.sensory_profile)) {
                            SensoryRadarChart(
                                sweetness = log.sweetness,
                                acidity = log.acidity,
                                body = log.body,
                                bitterness = log.bitterness,
                                sweetnessLabel = stringResource(R.string.sweetness),
                                acidityLabel = stringResource(R.string.acidity),
                                bodyLabel = stringResource(R.string.body),
                                bitternessLabel = stringResource(R.string.bitterness)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Conditional Flavor Tags
                    if (log.hasFlavorTags && log.flavorTags.isNotEmpty()) {
                        CardSection(title = stringResource(R.string.flavor_tags)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                log.flavorTags.forEach { tag ->
                                    FlavorTagChip(tag = tag)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Notes
                    if (log.notes.isNotEmpty()) {
                        CardSection(title = stringResource(R.string.notes)) {
                            Text(
                                text = log.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.delete), style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.edit), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun QuickStat(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}



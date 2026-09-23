package com.example.brewlog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brewlog.R
import com.example.brewlog.data.BrewLog
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Coffee,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_brews_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
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
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 24.sp
                    )
                    if (log.roaster.isNotEmpty()) {
                        Text(
                            text = log.roaster,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Quick Stats Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickStat(Icons.Default.Settings, log.grindSize.toString())
                        if (log.singleGrams > 0 || log.doubleGrams > 0) {
                            val grams = if (log.doubleGrams > 0) log.doubleGrams else log.singleGrams
                            QuickStat(Icons.Default.Scale, "${grams}g")
                        }
                        if (log.roastLevel.isNotEmpty()) {
                            Surface(
                                color = when(log.roastLevel) {
                                    "Light" -> Color(0xFFF5E6D3)
                                    "Dark" -> Color(0xFF4E342E)
                                    else -> Color(0xFF8D6E63)
                                },
                                contentColor = if (log.roastLevel == "Dark") Color.White else Color.Black,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    log.roastLevel,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (log.hasRating) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "${log.rating}/10",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                }
            }

            // Expanded Content
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    // Conditional Blend Composition
                    if (log.hasBlendSettings) {
                        Text("Blend Composition", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = buildString {
                                val components = mutableListOf<String>()
                                if (log.arabicaPercentage > 0) components.add("Arabica: ${log.arabicaPercentage}%")
                                if (log.robustaPercentage > 0) components.add("Robusta: ${log.robustaPercentage}%")
                                if (log.excelsaPercentage > 0) components.add("Excelsa: ${log.excelsaPercentage}%")
                                if (log.libericaPercentage > 0) components.add("Liberica: ${log.libericaPercentage}%")
                                append(components.joinToString(", "))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Conditional Sensory Profile
                    if (log.hasSensoryProfile) {
                        Text(stringResource(R.string.sensory_profile), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        SensoryBar(stringResource(R.string.sweetness), log.sweetness)
                        SensoryBar(stringResource(R.string.acidity), log.acidity)
                        SensoryBar(stringResource(R.string.body), log.body)
                        SensoryBar(stringResource(R.string.bitterness), log.bitterness)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Conditional Flavor Tags
                    if (log.hasFlavorTags && log.flavorTags.isNotEmpty()) {
                        Text(stringResource(R.string.flavor_tags), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        FlowRow(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            log.flavorTags.forEach { tag ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Notes
                    if (log.notes.isNotEmpty()) {
                        Text(stringResource(R.string.notes), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = log.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.delete))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.edit))
                        }
                    }
                }
            }
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

@Composable
fun SensoryBar(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(90.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        LinearProgressIndicator(
            progress = { value / 5f },
            modifier = Modifier.weight(1f).height(8.dp),
            strokeCap = StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "$value/5",
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

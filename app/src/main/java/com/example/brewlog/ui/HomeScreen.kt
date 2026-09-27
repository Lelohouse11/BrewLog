package com.example.brewlog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brewlog.R
import com.example.brewlog.data.BrewLog
import com.example.brewlog.ui.components.*
import com.example.brewlog.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var quickEditLog by remember { mutableStateOf<BrewLog?>(null) }
    val showFilters = remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val isDark = isAppInDarkTheme()

    val glassBg = if (isDark) EspressoGlassBg else VellumGlassBg
    val glassBorder = if (isDark) CremaAmber.copy(alpha = 0.35f) else CremaAmberDark.copy(alpha = 0.35f)
    val glowColor = if (isDark) CremaAmber else CremaAmberDark

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 4.dp)
            ) {
                // Seamless Crema Glass Search Header with Radiant Glow
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = glassBg,
                    border = BorderStroke(1.2.dp, glassBorder),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .cremaGlow(color = glowColor, borderRadius = 24.dp, glowRadius = 5.dp, alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = {
                                Text(
                                    stringResource(R.string.search_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            maxLines = 1,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.weight(1f)
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }

                        IconButton(onClick = { showFilters.value = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Surface(
                onClick = onNavigateToAddBrew,
                shape = RoundedCornerShape(20.dp),
                color = glassBg,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(bottom = 76.dp)
                    .cremaGlow(color = glowColor, borderRadius = 20.dp, glowRadius = 6.dp, alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Brew Setting",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Bohne",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CoffeeEmptyState(
                        title = stringResource(R.string.no_brews_found),
                        description = "Füge deine erste Specialty Coffee Bohne hinzu"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 120.dp)
                ) {
                    itemsIndexed(logs, key = { _, item -> item.id }) { index, log ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay((index * 40L).coerceAtMost(300L).milliseconds)
                            visible = true
                        }
                        val alpha by animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            label = "alpha_$index"
                        )
                        val translateY by animateFloatAsState(
                            targetValue = if (visible) 0f else 24f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "translateY_$index"
                        )

                        BrewLogItem(
                            log = log,
                            isExpanded = expandedLogId == log.id,
                            onToggleExpand = {
                                expandedLogId = if (expandedLogId == log.id) null else log.id
                            },
                            onDelete = { viewModel.deleteLog(log) },
                            onEdit = { onNavigateToEditBrew(log.id) },
                            onQuickEditRoastDate = { quickEditLog = log },
                            modifier = Modifier
                                .animateItem()
                                .graphicsLayer {
                                    this.alpha = alpha
                                    this.translationY = translateY
                                    this.clip = false
                                }
                        )
                    }
                }
            }
        }

        if (showFilters.value) {
            val sheetBg = if (isDark) EspressoGlassBg else VellumGlassBg

            ModalBottomSheet(
                onDismissRequest = { showFilters.value = false },
                sheetState = sheetState,
                containerColor = sheetBg,
                scrimColor = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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

        if (quickEditLog != null) {
            QuickEditRoastDateDialog(
                currentLog = quickEditLog!!,
                onDismiss = { quickEditLog = null },
                onConfirm = { updatedDate ->
                    viewModel.updateLog(quickEditLog!!.copy(roastDate = updatedDate))
                    quickEditLog = null
                }
            )
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
    onEdit: () -> Unit,
    onQuickEditRoastDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "rotation"
    )

    CremaGlassCard(
        onClick = onToggleExpand,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        ) {
            // Specialty Bag Header: Roaster Upper case + Roast Level Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            Text(
                text = log.roaster.ifEmpty { "SPECIALTY ROASTER" }.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FreshnessBadge(
                    roastDateString = log.roastDate,
                    onQuickEditDate = onQuickEditRoastDate
                )
                if (log.roastLevel.isNotEmpty()) {
                    RoastLevelBadge(roastLevel = log.roastLevel)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Coffee Name in Serif Display Font
        Text(
            text = log.coffeeName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Collapsed Flavor Tags preview
        if (log.hasFlavorTags && log.flavorTags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                log.flavorTags.forEach { tag ->
                    FlavorTagChip(tag = tag)
                }
            }
        }

        // Barista Quick Metrics & Rating Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val grindFormatted = String.format(Locale.ROOT, "%.1f", log.grindSize)
                BaristaMetricBadge(label = "GRIND", value = grindFormatted)

                val grams = if (log.doubleGrams > 0) log.doubleGrams else log.singleGrams
                if (grams > 0) {
                    val gramsFormatted = String.format(Locale.ROOT, "%.1f", grams)
                    BaristaMetricBadge(label = "DOSE", value = gramsFormatted, unit = "g")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (log.hasRating) {
                    CoffeeBeanRating(
                        rating = log.rating,
                        beanSize = 14.dp,
                        modifier = Modifier.padding(end = 4.dp)
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
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // Blend Composition
                if (log.hasBlendSettings) {
                    CardSection(title = "Mischungs-Verhältnis") {
                        BlendCompositionBar(
                            arabica = log.arabicaPercentage,
                            robusta = log.robustaPercentage,
                            excelsa = log.excelsaPercentage,
                            liberica = log.libericaPercentage
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Sensory Profile Radar
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

                // Barista Notes
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
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditRoastDateDialog(
    currentLog: BrewLog,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var roastDateText by remember { mutableStateOf(currentLog.roastDate) }

    val isDark = isAppInDarkTheme()
    val glassBg = if (isDark) EspressoGlassBg else VellumGlassBg
    val glowColor = if (isDark) CremaAmber else CremaAmberDark

    val todayStr = remember { LocalDate.now().toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glassBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.cremaGlow(color = glowColor, borderRadius = 24.dp, glowRadius = 6.dp, alpha = 0.25f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.quick_edit_roast_date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "${currentLog.roaster} - ${currentLog.coffeeName}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.quick_edit_roast_date_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = roastDateText,
                        onValueChange = { roastDateText = it },
                        label = { Text(stringResource(R.string.roast_date)) },
                        placeholder = { Text(stringResource(R.string.roast_date_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = { roastDateText = todayStr },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = stringResource(R.string.today),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.today), fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.freshness_status),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FreshnessBadge(roastDateString = roastDateText)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(roastDateText.trim()) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

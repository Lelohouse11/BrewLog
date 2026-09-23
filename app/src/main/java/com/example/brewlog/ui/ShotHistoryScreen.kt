package com.example.brewlog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brewlog.R
import com.example.brewlog.data.ShotLogWithBean
import com.example.brewlog.ui.components.*
import com.example.brewlog.util.ExtractionEngine
import com.example.brewlog.util.getLocalizedDiagnosis
import com.example.brewlog.util.getLocalizedExplanation
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotHistoryScreen(
    viewModel: BrewViewModel = viewModel(),
    onNavigateToDialIn: (Int, String, Double, Float) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val shotLogs by viewModel.allShotLogs.collectAsState()
    val beans by viewModel.allLogs.collectAsState()
    
    var selectedBeanId by remember { mutableStateOf<Int?>(null) }

    val filteredLogs = if (selectedBeanId == null) {
        shotLogs
    } else {
        shotLogs.filter { it.shotLog.beanId == selectedBeanId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shot_history)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Filter Bar
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedBeanId == null,
                        onClick = { selectedBeanId = null },
                        label = { Text(stringResource(R.string.all_beans)) }
                    )
                }
                items(beans) { bean ->
                    FilterChip(
                        selected = selectedBeanId == bean.id,
                        onClick = { selectedBeanId = bean.id },
                        label = { Text(bean.coffeeName) }
                    )
                }
            }

            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CoffeeEmptyState(
                        title = stringResource(R.string.no_shots_logged)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredLogs, key = { it.shotLog.id }) { shotWithBean ->
                        ShotLogCard(
                            shotWithBean = shotWithBean,
                            onUseSettingsInDialIn = {
                                onNavigateToDialIn(
                                    shotWithBean.shotLog.beanId,
                                    shotWithBean.shotLog.basketType,
                                    shotWithBean.shotLog.doseIn.toDouble(),
                                    shotWithBean.shotLog.grindSize
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShotLogCard(
    shotWithBean: ShotLogWithBean,
    onUseSettingsInDialIn: () -> Unit
) {
    val shot = shotWithBean.shotLog
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("d. MMM, HH:mm", locale) }
    val dateString = dateFormat.format(Date(shot.timestamp))

    var isExpanded by remember { mutableStateOf(false) }

    val recommendation = remember(shot) {
        ExtractionEngine.getRecommendation(
            ExtractionEngine.ShotMetrics(
                doseIn = shot.doseIn,
                grindSize = shot.grindSize,
                timeSec = shot.extractionTimeSec,
                yieldOut = shot.yieldOut,
                acidity = when (shot.acidityEval) {
                    -1 -> ExtractionEngine.AcidityEval.TOOSOUR
                    1 -> ExtractionEngine.AcidityEval.FLAT
                    else -> ExtractionEngine.AcidityEval.BALANCED
                },
                bitterness = when (shot.bitternessEval) {
                    -1 -> ExtractionEngine.BitternessEval.UNDER
                    1 -> ExtractionEngine.BitternessEval.BITTER
                    else -> ExtractionEngine.BitternessEval.SWEET
                },
                body = when (shot.bodyEval) {
                    -1 -> ExtractionEngine.BodyEval.THIN
                    1 -> ExtractionEngine.BodyEval.HEAVY
                    else -> ExtractionEngine.BodyEval.OPTIMAL
                }
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Bean Name, Date, Basket Badge & Arrow Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shotWithBean.beanName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatioTag(doseIn = shot.doseIn, yieldOut = shot.yieldOut)

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = if (shot.basketType.lowercase() == "single") stringResource(R.string.single_basket).uppercase() else stringResource(R.string.double_basket).uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Primary Brew Ratio Bar
            BrewRatioBar(
                doseInGrams = shot.doseIn,
                yieldInGrams = shot.yieldOut,
                extractionTimeSec = shot.extractionTimeSec
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem(Icons.Default.Settings, "${stringResource(R.string.grind_size)}: ${"%.1f".format(shot.grindSize)}")
            }

            // Collapsed quick preview or expanded details
            if (!isExpanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))

                // Sensory Badges preview
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TasteDiagnosisBadge(
                        acidityEval = shot.acidityEval,
                        bitternessEval = shot.bitternessEval,
                        bodyEval = shot.bodyEval
                    )
                }
            }

            // Expanded Detail View
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))

                    // Calculated Extractions Stats (Ratio & Flow Rate)
                    Text(stringResource(R.string.extraction_analysis), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))

                    val ratio = if (shot.doseIn > 0) shot.yieldOut / shot.doseIn else 0f
                    val flowRate = if (shot.extractionTimeSec > 0) shot.yieldOut / shot.extractionTimeSec else 0f

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.ratio), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Text("1 : ${"%.1f".format(ratio)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.flow_rate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Text("${"%.1f".format(flowRate)} g/s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Sensory Delta Profile Details
                    Text(stringResource(R.string.sensory_profile), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SensoryDetailChip(stringResource(R.string.acidity), when(shot.acidityEval) { -1 -> stringResource(R.string.too_sour); 1 -> stringResource(R.string.flat); else -> stringResource(R.string.balanced) }, modifier = Modifier.weight(1f))
                        SensoryDetailChip(stringResource(R.string.bitterness), when(shot.bitternessEval) { -1 -> stringResource(R.string.under_extracted); 1 -> stringResource(R.string.bitter); else -> stringResource(R.string.sweet) }, modifier = Modifier.weight(1f))
                        SensoryDetailChip(stringResource(R.string.body), when(shot.bodyEval) { -1 -> stringResource(R.string.thin); 1 -> stringResource(R.string.heavy); else -> stringResource(R.string.optimal) }, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Recommendation Card
                    val cardColor = when {
                        recommendation.puckPrepWarning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        recommendation.type == ExtractionEngine.DiagnosisType.BALANCED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    }

                    Surface(
                        color = cardColor,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (recommendation.puckPrepWarning) Icons.Default.Warning else Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (recommendation.puckPrepWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    recommendation.getLocalizedDiagnosis(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (recommendation.puckPrepWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                recommendation.getLocalizedExplanation(),
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (recommendation.suggestedGrindChange != 0f || recommendation.suggestedYieldChange != 0f) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = buildString {
                                        if (recommendation.suggestedGrindChange != 0f) {
                                            append("${stringResource(R.string.grind_size)}: ${"%.1f".format(recommendation.recommendedGrindSize)}")
                                        }
                                        if (recommendation.suggestedYieldChange != 0f) {
                                            if (isNotEmpty()) append(" | ")
                                            append("Yield: ${"%.1f".format(recommendation.recommendedYieldOut)}g")
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Notes if present
                    if (shot.notes.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.notes), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = shot.notes,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Use Settings Button
                    OutlinedButton(
                        onClick = onUseSettingsInDialIn,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.use_as_dial_in_basis))
                    }
                }
            }
        }
    }
}

@Composable
private fun SensoryDetailChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetricItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RatioTag(doseIn: Float, yieldOut: Float, modifier: Modifier = Modifier) {
    val ratio = if (doseIn > 0f) yieldOut / doseIn else 0f
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "1 : ${"%.1f".format(ratio)}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TasteDiagnosisBadge(
    acidityEval: Int,
    bitternessEval: Int,
    bodyEval: Int,
    modifier: Modifier = Modifier
) {
    val isBalanced = acidityEval == 0 && bitternessEval == 0 && bodyEval == 0

    val badgeConfig = when {
        isBalanced -> BadgeConfig(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            stringResource(R.string.balanced),
            Icons.Default.CheckCircle
        )
        acidityEval == -1 -> BadgeConfig(
            Color(0xFFFFF8E1),
            Color(0xFFE65100),
            stringResource(R.string.too_sour),
            Icons.Default.WaterDrop
        )
        bitternessEval == 1 -> BadgeConfig(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            stringResource(R.string.bitter),
            Icons.Default.Warning
        )
        else -> BadgeConfig(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            if (acidityEval == 1) stringResource(R.string.flat) else stringResource(R.string.under_extracted),
            Icons.Default.Tune
        )
    }

    Surface(
        modifier = modifier,
        color = badgeConfig.bgColor,
        contentColor = badgeConfig.textColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, badgeConfig.textColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = badgeConfig.icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = badgeConfig.textColor)
            Text(
                text = badgeConfig.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class BadgeConfig(
    val bgColor: Color,
    val textColor: Color,
    val label: String,
    val icon: ImageVector
)


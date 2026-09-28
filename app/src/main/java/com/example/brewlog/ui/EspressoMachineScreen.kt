package com.example.brewlog.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.brewlog.R
import com.example.brewlog.data.EspressoMachine
import com.example.brewlog.ui.components.*
import com.example.brewlog.ui.theme.*
import com.example.brewlog.util.MaintenanceCalculator
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EspressoMachineScreen(
    viewModel: BrewViewModel,
    onNavigateToEdit: () -> Unit,
    geminiViewModel: GeminiViewModel = viewModel()
) {
    val machine by viewModel.espressoMachine.collectAsState()
    val scanResult by geminiViewModel.machineScanResult.collectAsState()
    val isScanning by geminiViewModel.isLoading.collectAsState()
    val scanError by geminiViewModel.errorMessage.collectAsState()

    var showSetupDialog by remember { mutableStateOf(false) }
    var showQuickEditDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Setup form state
    var setupBrand by remember { mutableStateOf("") }
    var setupModel by remember { mutableStateOf("") }
    var setupConsumption by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val isDark = isAppInDarkTheme()
    val glassBg = if (isDark) EspressoGlassBg else VellumGlassBg
    val glassBorder = if (isDark) CremaAmber.copy(alpha = 0.3f) else CremaAmberDark.copy(alpha = 0.3f)

    // Auto-save from AI when scan completes
    LaunchedEffect(scanResult) {
        val result = scanResult ?: return@LaunchedEffect
        
        if (!result.machineFound) {
            return@LaunchedEffect
        }
        
        val currentMachine = machine ?: EspressoMachine()
        val updatedMachine = currentMachine.copy(
            brand = setupBrand.ifEmpty { currentMachine.brand }.trim(),
            model = setupModel.ifEmpty { currentMachine.model }.trim(),
            weeklyConsumption = setupConsumption.toIntOrNull() ?: currentMachine.weeklyConsumption,
            portafilterDiameter = result.portafilterDiameter,
            hasIntegratedGrinder = result.hasIntegratedGrinder,
            hasSteamWand = result.hasSteamWand,
            waterFilterIntervalDays = result.waterFilterMaxDays,
            descaleIntervalDays = result.descaleMaxDays,
            backflushIntervalDays = result.backflushMaxDays,
            waterFilterLimitCycles = result.waterFilterLimitCycles,
            descaleLimitCycles = result.descaleLimitCycles,
            backflushLimitCycles = result.backflushLimitCycles
        )
        
        viewModel.updateEspressoMachine(updatedMachine)
        geminiViewModel.clearResult()
        showSetupDialog = false
        
        // Reset setup form state
        setupBrand = ""
        setupModel = ""
        setupConsumption = ""
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        if (machine == null) {
            // Empty State Centered
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                CoffeeEmptyState(
                    title = stringResource(R.string.no_machine_title),
                    description = stringResource(R.string.no_machine_sub),
                    actionButton = {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AiFeatureButton(
                                onClick = { showSetupDialog = true },
                                title = stringResource(R.string.ai_setup),
                                subtitle = stringResource(R.string.ai_setup_subtitle),
                                icon = Icons.Default.AutoAwesome
                            )

                            OutlinedButton(
                                onClick = { onNavigateToEdit() },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(stringResource(R.string.manual_setup), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                )
            }
        } else {
            // Machine Details
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Machine Base Information Hero Card with Integrated Options Menu
                CremaGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.machine_base_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = glassBg,
                                border = BorderStroke(1.dp, glassBorder)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_machine)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.reset_machine)) },
                                    leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.deleteEspressoMachine()
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    if (machine?.photoUri != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.2f)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            AsyncImage(
                                model = machine?.photoUri,
                                contentDescription = "Machine Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Text(
                        text = "${machine?.brand} ${machine?.model}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    InfoRow(Icons.Default.Straighten, stringResource(R.string.portafilter_diameter), "${machine?.portafilterDiameter} mm")
                    InfoRow(
                        if (machine?.hasIntegratedGrinder == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        stringResource(R.string.integrated_grinder),
                        if (machine?.hasIntegratedGrinder == true) stringResource(R.string.yes) else stringResource(R.string.no),
                        color = if (machine?.hasIntegratedGrinder == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    InfoRow(
                        if (machine?.hasSteamWand == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        stringResource(R.string.steam_wand),
                        if (machine?.hasSteamWand == true) stringResource(R.string.yes) else stringResource(R.string.no),
                        color = if (machine?.hasSteamWand == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                // Section 2: Maintenance Intervals
                CremaGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.maintenance_tracking), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.weekly_consumption, machine?.weeklyConsumption ?: 0),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        IconButton(
                            onClick = { showQuickEditDialog = true },
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.quick_edit_consumption),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    val weeklyCount = machine?.weeklyConsumption ?: 0
                    
                    MaintenanceProgressBar(
                        label = stringResource(R.string.water_filter_replacement),
                        maxDays = machine?.waterFilterIntervalDays ?: 90,
                        limitCycles = machine?.waterFilterLimitCycles ?: 0,
                        lastDone = machine?.lastWaterFilterChange,
                        weeklyConsumption = weeklyCount,
                        onMarkDone = { viewModel.updateEspressoMachine(machine!!.copy(lastWaterFilterChange = System.currentTimeMillis())) }
                    )
                    
                    MaintenanceProgressBar(
                        label = stringResource(R.string.descaling),
                        maxDays = machine?.descaleIntervalDays ?: 180,
                        limitCycles = machine?.descaleLimitCycles ?: 0,
                        lastDone = machine?.lastDescaling,
                        weeklyConsumption = weeklyCount,
                        onMarkDone = { viewModel.updateEspressoMachine(machine!!.copy(lastDescaling = System.currentTimeMillis())) }
                    )
                    
                    MaintenanceProgressBar(
                        label = stringResource(R.string.backflushing),
                        maxDays = machine?.backflushIntervalDays ?: 30,
                        limitCycles = machine?.backflushLimitCycles ?: 0,
                        lastDone = machine?.lastBackflushing,
                        weeklyConsumption = weeklyCount,
                        onMarkDone = { viewModel.updateEspressoMachine(machine!!.copy(lastBackflushing = System.currentTimeMillis())) }
                    )
                }
                
                Spacer(Modifier.height(110.dp))
            }
        }

        if (showSetupDialog) {
            AlertDialog(
                onDismissRequest = { if (!isScanning) showSetupDialog = false },
                containerColor = glassBg,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.cremaGlow(color = if (isDark) CremaAmber else CremaAmberDark, borderRadius = 24.dp, glowRadius = 6.dp, alpha = 0.25f),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.setup_machine_ai), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isScanning) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.5.dp
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        stringResource(R.string.ai_fetching_specs),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            if (scanError != null) {
                                Text(scanError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedTextField(
                                value = setupBrand,
                                onValueChange = { setupBrand = it },
                                label = { Text(stringResource(R.string.brand)) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = setupModel,
                                onValueChange = { setupModel = it },
                                label = { Text(stringResource(R.string.model)) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = setupConsumption,
                                onValueChange = { setupConsumption = it },
                                label = { Text(stringResource(R.string.weekly_consumption_cups)) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                },
                confirmButton = {
                    if (!isScanning) {
                        Surface(
                            onClick = { geminiViewModel.fetchMachineInfo(setupBrand, setupModel) },
                            enabled = setupBrand.isNotBlank() && setupModel.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, if (setupBrand.isNotBlank() && setupModel.isNotBlank()) CremaAmber else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = if (setupBrand.isNotBlank() && setupModel.isNotBlank()) {
                                            Brush.horizontalGradient(listOf(Color(0xFF8E44AD), Color(0xFFC88A58)))
                                        } else {
                                            SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (setupBrand.isNotBlank() && setupModel.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.find_with_ai),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (setupBrand.isNotBlank() && setupModel.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                dismissButton = {
                    if (!isScanning) {
                        TextButton(onClick = { showSetupDialog = false }) { Text(stringResource(R.string.cancel)) }
                    }
                }
            )
        }

        if (showQuickEditDialog && machine != null) {
            QuickEditConsumptionDialog(
                machine = machine!!,
                onDismiss = { showQuickEditDialog = false },
                onConfirm = { newConsumption ->
                    viewModel.updateWeeklyConsumption(newConsumption)
                    showQuickEditDialog = false
                }
            )
        }
    }
}
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = color)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun MaintenanceProgressBar(
    label: String,
    maxDays: Int,
    limitCycles: Int,
    lastDone: Long?,
    weeklyConsumption: Int,
    onMarkDone: () -> Unit
) {
    val daysSinceLast = if (lastDone != null) TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastDone).toInt() else 0

    val consumptionIntervalDays = if (weeklyConsumption > 0 && limitCycles > 0) {
        (limitCycles / (weeklyConsumption / 7.0)).toInt()
    } else {
        Int.MAX_VALUE
    }

    val actualIntervalDays = minOf(maxDays, consumptionIntervalDays)
    val remainingDays = actualIntervalDays - daysSinceLast
    val rawProgress = (remainingDays.coerceIn(0, actualIntervalDays).toFloat() / actualIntervalDays)
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, label = "maintenanceProgress")

    val errorColor = MaterialTheme.colorScheme.error
    val errorBg = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    val warningColor = CremaAmber
    val warningBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    val healthyColor = MaterialTheme.colorScheme.primary
    val healthyBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)

    val (statusColor, statusBg, statusLabel) = when {
        remainingDays <= 0 -> Triple(
            errorColor,
            errorBg,
            if (remainingDays < 0) stringResource(R.string.overdue_by_days, -remainingDays) else stringResource(R.string.due_today)
        )
        rawProgress <= 0.3f -> Triple(
            warningColor,
            warningBg,
            stringResource(R.string.days_remaining, remainingDays)
        )
        else -> Triple(
            healthyColor,
            healthyBg,
            stringResource(R.string.days_remaining, remainingDays)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = statusBg,
                        contentColor = statusColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onMarkDone,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = statusColor)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.mark_completed), modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun QuickEditConsumptionDialog(
    machine: EspressoMachine,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var consumptionText by remember { mutableStateOf(machine.weeklyConsumption.toString()) }
    val currentWeekly = machine.weeklyConsumption
    val newWeekly = consumptionText.toIntOrNull()?.coerceAtLeast(0) ?: 0

    val isDark = isAppInDarkTheme()
    val glassBg = if (isDark) EspressoGlassBg else VellumGlassBg
    val glowColor = if (isDark) CremaAmber else CremaAmberDark

    // Live recalculation preview
    val previewMachine = remember(machine, newWeekly) {
        MaintenanceCalculator.updateWeeklyConsumption(machine, newWeekly)
    }

    val currentWaterDays = MaintenanceCalculator.calculateEffectiveIntervalDays(
        machine.waterFilterIntervalDays, machine.waterFilterLimitCycles, currentWeekly
    )
    val newWaterDays = MaintenanceCalculator.calculateEffectiveIntervalDays(
        previewMachine.waterFilterIntervalDays, previewMachine.waterFilterLimitCycles, newWeekly
    )

    val currentDescaleDays = MaintenanceCalculator.calculateEffectiveIntervalDays(
        machine.descaleIntervalDays, machine.descaleLimitCycles, currentWeekly
    )
    val newDescaleDays = MaintenanceCalculator.calculateEffectiveIntervalDays(
        previewMachine.descaleIntervalDays, previewMachine.descaleLimitCycles, newWeekly
    )

    val currentBackflushDays = MaintenanceCalculator.calculateEffectiveIntervalDays(
        machine.backflushIntervalDays, machine.backflushLimitCycles, currentWeekly
    )
    val newBackflushDays = MaintenanceCalculator.calculateEffectiveIntervalDays(
        previewMachine.backflushIntervalDays, previewMachine.backflushLimitCycles, newWeekly
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glassBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.cremaGlow(color = glowColor, borderRadius = 24.dp, glowRadius = 6.dp, alpha = 0.25f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.quick_edit_consumption),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.quick_edit_consumption_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Stepper + Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = {
                            val current = consumptionText.toIntOrNull() ?: 0
                            if (current > 1) {
                                consumptionText = (current - 1).toString()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    OutlinedTextField(
                        value = consumptionText,
                        onValueChange = { consumptionText = it.filter { char -> char.isDigit() } },
                        label = { Text(stringResource(R.string.weekly_consumption_cups)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )

                    FilledIconButton(
                        onClick = {
                            val current = consumptionText.toIntOrNull() ?: 0
                            consumptionText = (current + 1).toString()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                // Recalculated Intervals Card Preview
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.recalculated_intervals),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        IntervalPreviewRow(
                            label = stringResource(R.string.water_filter_replacement),
                            oldDays = currentWaterDays,
                            newDays = newWaterDays
                        )

                        IntervalPreviewRow(
                            label = stringResource(R.string.descaling),
                            oldDays = currentDescaleDays,
                            newDays = newDescaleDays
                        )

                        IntervalPreviewRow(
                            label = stringResource(R.string.backflushing),
                            oldDays = currentBackflushDays,
                            newDays = newBackflushDays
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newWeekly) },
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

@Composable
private fun IntervalPreviewRow(
    label: String,
    oldDays: Int,
    newDays: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (oldDays != newDays) {
                Text(
                    text = stringResource(R.string.days_format, oldDays),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = stringResource(R.string.days_format, newDays),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (oldDays != newDays) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

package com.example.brewlog.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.brewlog.ui.components.CoffeeEmptyState
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EspressoMachineScreen(
    viewModel: BrewViewModel,
    onNavigateToEdit: () -> Unit,
    onNavigateToSettings: () -> Unit,
    geminiViewModel: GeminiViewModel = viewModel()
) {
    val machine by viewModel.espressoMachine.collectAsState()
    val scanResult by geminiViewModel.machineScanResult.collectAsState()
    val isScanning by geminiViewModel.isLoading.collectAsState()
    val scanError by geminiViewModel.errorMessage.collectAsState()

    var showSetupDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Setup form state
    var setupBrand by remember { mutableStateOf("") }
    var setupModel by remember { mutableStateOf("") }
    var setupConsumption by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.espresso_machine)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    if (machine != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_machine)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
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
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (machine == null) {
                // Empty State
                CoffeeEmptyState(
                    title = stringResource(R.string.no_machine_title),
                    description = stringResource(R.string.no_machine_sub),
                    actionButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { onNavigateToEdit() }) {
                                Text(stringResource(R.string.manual_setup))
                            }
                            Button(onClick = { showSetupDialog = true }) {
                                Icon(Icons.Default.AutoAwesome, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.ai_setup))
                            }
                        }
                    }
                )
            } else {
                // Machine Details
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Section 1: Machine Base Information
                    MachineSection(title = stringResource(R.string.machine_base_info), icon = Icons.Default.Info) {
                        // Photo
                        if (machine?.photoUri != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .padding(bottom = 16.dp),
                                shape = MaterialTheme.shapes.medium,
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
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
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
                    MachineSection(title = stringResource(R.string.maintenance_tracking), icon = Icons.Default.Build) {
                        Text(
                            stringResource(R.string.weekly_consumption, machine?.weeklyConsumption ?: 0),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(Modifier.height(16.dp))

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
                    
                    Spacer(Modifier.height(80.dp))
                }
            }

            if (showSetupDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isScanning) showSetupDialog = false },
                    title = { Text(stringResource(R.string.setup_machine_ai)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (isScanning) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(16.dp))
                                    Text(stringResource(R.string.ai_fetching_specs))
                                }
                            } else {
                                if (scanError != null) {
                                    Text(scanError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedTextField(value = setupBrand, onValueChange = { setupBrand = it }, label = { Text(stringResource(R.string.brand)) }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = setupModel, onValueChange = { setupModel = it }, label = { Text(stringResource(R.string.model)) }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = setupConsumption, onValueChange = { setupConsumption = it }, label = { Text("Wöchentliche Tassen") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                        }
                    },
                    confirmButton = {
                        if (!isScanning) {
                            Button(
                                onClick = { geminiViewModel.fetchMachineInfo(setupBrand, setupModel) },
                                enabled = setupBrand.isNotBlank() && setupModel.isNotBlank()
                            ) {
                                Text(stringResource(R.string.find_with_ai))
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
fun MachineSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            content()
        }
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

    val (statusColor, statusBg, statusLabel) = when {
        remainingDays <= 0 -> Triple(
            Color(0xFFC62828),
            Color(0xFFFFEBEE),
            if (remainingDays < 0) stringResource(R.string.overdue_by_days, -remainingDays) else "Heute fällig"
        )
        rawProgress <= 0.3f -> Triple(
            Color(0xFFE65100),
            Color(0xFFFFF8E1),
            stringResource(R.string.days_remaining, remainingDays)
        )
        else -> Triple(
            Color(0xFF2E7D32),
            Color(0xFFE8F5E9),
            stringResource(R.string.days_remaining, remainingDays)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
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
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        color = statusBg,
                        contentColor = statusColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f)
            )
        }
    }
}

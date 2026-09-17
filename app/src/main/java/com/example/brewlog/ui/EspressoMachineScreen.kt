package com.example.brewlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.brewlog.data.EspressoMachine
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
                title = { Text("My Espresso Machine") },
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
                                    text = { Text("Edit Machine") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset Machine") },
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
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CoffeeMaker, 
                        contentDescription = null, 
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "No machine added yet", 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Add your espresso machine to track maintenance and specs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(Modifier.height(32.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { onNavigateToEdit() }) {
                            Text("Manual Setup")
                        }
                        Button(onClick = { showSetupDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(Modifier.width(8.dp))
                            Text("AI Setup")
                        }
                    }
                }
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
                    MachineSection(title = "Machine Base Information", icon = Icons.Default.Info) {
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
                        
                        InfoRow(Icons.Default.Straighten, "Portafilter", "${machine?.portafilterDiameter} mm")
                        InfoRow(
                            if (machine?.hasIntegratedGrinder == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            "Integrated Grinder",
                            if (machine?.hasIntegratedGrinder == true) "Yes" else "No",
                            color = if (machine?.hasIntegratedGrinder == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        InfoRow(
                            if (machine?.hasSteamWand == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            "Steam Wand",
                            if (machine?.hasSteamWand == true) "Yes" else "No",
                            color = if (machine?.hasSteamWand == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    // Section 2: Maintenance Intervals
                    MachineSection(title = "Maintenance Tracking", icon = Icons.Default.Build) {
                        Text(
                            "Weekly Consumption: ${machine?.weeklyConsumption} cups",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(Modifier.height(16.dp))

                        val weeklyCount = machine?.weeklyConsumption ?: 0
                        
                        MaintenanceProgressBar(
                            label = "Water Filter Replacement",
                            maxDays = machine?.waterFilterIntervalDays ?: 90,
                            limitCycles = machine?.waterFilterLimitCycles ?: 0,
                            lastDone = machine?.lastWaterFilterChange,
                            weeklyConsumption = weeklyCount,
                            onMarkDone = { viewModel.updateEspressoMachine(machine!!.copy(lastWaterFilterChange = System.currentTimeMillis())) }
                        )
                        
                        MaintenanceProgressBar(
                            label = "Descaling",
                            maxDays = machine?.descaleIntervalDays ?: 180,
                            limitCycles = machine?.descaleLimitCycles ?: 0,
                            lastDone = machine?.lastDescaling,
                            weeklyConsumption = weeklyCount,
                            onMarkDone = { viewModel.updateEspressoMachine(machine!!.copy(lastDescaling = System.currentTimeMillis())) }
                        )
                        
                        MaintenanceProgressBar(
                            label = "Backflushing",
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
                    title = { Text("Setup Machine with AI") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (isScanning) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(16.dp))
                                    Text("AI is fetching machine specs...")
                                }
                            } else {
                                if (scanError != null) {
                                    Text(scanError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }
                                if (scanResult?.machineFound == false) {
                                    Text("Could not find this machine. Please check details or use Manual Setup.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedTextField(value = setupBrand, onValueChange = { setupBrand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = setupModel, onValueChange = { setupModel = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = setupConsumption, onValueChange = { setupConsumption = it }, label = { Text("Weekly Consumption") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                        }
                    },
                    confirmButton = {
                        if (!isScanning) {
                            Button(
                                onClick = { geminiViewModel.fetchMachineInfo(setupBrand, setupModel) },
                                enabled = setupBrand.isNotBlank() && setupModel.isNotBlank()
                            ) {
                                Text("Find with AI")
                            }
                        }
                    },
                    dismissButton = {
                        if (!isScanning) {
                            TextButton(onClick = { showSetupDialog = false }) { Text("Cancel") }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = color)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun MachineSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
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
    
    // Calculate interval days based on consumption vs max cycles
    val consumptionIntervalDays = if (weeklyConsumption > 0 && limitCycles > 0) {
        (limitCycles / (weeklyConsumption / 7.0)).toInt()
    } else {
        Int.MAX_VALUE
    }
    
    // The actual interval is whichever comes first (Max Days or Consumption Limit)
    val actualIntervalDays = minOf(maxDays, consumptionIntervalDays)
    
    val remainingDays = actualIntervalDays - daysSinceLast
    val progress = (remainingDays.coerceIn(0, actualIntervalDays).toFloat() / actualIntervalDays)

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            IconButton(onClick = onMarkDone) {
                Icon(Icons.Default.CheckCircle, "Mark Completed", tint = MaterialTheme.colorScheme.primary)
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(MaterialTheme.shapes.small),
            color = if (remainingDays < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = if (remainingDays < 0) "Overdue by ${-remainingDays} days" else "$remainingDays days remaining",
            style = MaterialTheme.typography.bodySmall,
            color = if (remainingDays < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

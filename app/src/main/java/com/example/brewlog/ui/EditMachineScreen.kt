package com.example.brewlog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.brewlog.data.EspressoMachine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMachineScreen(
    viewModel: BrewViewModel,
    onNavigateBack: () -> Unit
) {
    val machine by viewModel.espressoMachine.collectAsState()

    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var consumption by remember { mutableStateOf("") }
    var diameter by remember { mutableStateOf("") }
    var hasGrinder by remember { mutableStateOf(false) }
    var hasSteamWand by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<String?>(null) }
    
    var waterDays by remember { mutableStateOf("") }
    var descaleDays by remember { mutableStateOf("") }
    var backflushDays by remember { mutableStateOf("") }
    
    var waterCycles by remember { mutableStateOf("") }
    var descaleCycles by remember { mutableStateOf("") }
    var backflushCycles by remember { mutableStateOf("") }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.saveMachinePhoto(it)
        }
    }

    LaunchedEffect(machine) {
        machine?.let {
            brand = it.brand
            model = it.model
            consumption = it.weeklyConsumption.toString()
            diameter = it.portafilterDiameter.toString()
            hasGrinder = it.hasIntegratedGrinder
            hasSteamWand = it.hasSteamWand
            photoUri = it.photoUri
            
            waterDays = it.waterFilterIntervalDays.toString()
            descaleDays = it.descaleIntervalDays.toString()
            backflushDays = it.backflushIntervalDays.toString()
            
            waterCycles = it.waterFilterLimitCycles.toString()
            descaleCycles = it.descaleLimitCycles.toString()
            backflushCycles = it.backflushLimitCycles.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Machine Specifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val updated = (machine ?: EspressoMachine()).copy(
                    brand = brand,
                    model = model,
                    weeklyConsumption = consumption.toIntOrNull() ?: 0,
                    portafilterDiameter = diameter.toDoubleOrNull() ?: 0.0,
                    hasIntegratedGrinder = hasGrinder,
                    hasSteamWand = hasSteamWand,
                    photoUri = photoUri,
                    waterFilterIntervalDays = waterDays.toIntOrNull() ?: 90,
                    descaleIntervalDays = descaleDays.toIntOrNull() ?: 180,
                    backflushIntervalDays = backflushDays.toIntOrNull() ?: 30,
                    waterFilterLimitCycles = waterCycles.toIntOrNull() ?: 0,
                    descaleLimitCycles = descaleCycles.toIntOrNull() ?: 0,
                    backflushLimitCycles = backflushCycles.toIntOrNull() ?: 0
                )
                viewModel.updateEspressoMachine(updated)
                onNavigateBack()
            }) {
                Icon(Icons.Default.Done, "Save")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo Section
            Text("Machine Photo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Machine Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { photoLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        ) {
                            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                                Icon(Icons.Default.Edit, "Change Photo", modifier = Modifier.padding(8.dp))
                            }
                        }
                    } else {
                        OutlinedButton(onClick = { photoLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Default.AddAPhoto, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Photo")
                        }
                    }
                }
            }

            HorizontalDivider()
            Text("Identity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = consumption, 
                onValueChange = { consumption = it }, 
                label = { Text("Weekly Consumption") }, 
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            HorizontalDivider()
            Text("Hardware", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = diameter, 
                onValueChange = { diameter = it }, 
                label = { Text("Portafilter Diameter (mm)") }, 
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasGrinder, onCheckedChange = { hasGrinder = it })
                Text("Integrated Grinder")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasSteamWand, onCheckedChange = { hasSteamWand = it })
                Text("Steam Wand")
            }

            HorizontalDivider()
            Text("Maintenance Intervals (Days)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(value = waterDays, onValueChange = { waterDays = it }, label = { Text("Water Filter Days") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = descaleDays, onValueChange = { descaleDays = it }, label = { Text("Descale Days") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = backflushDays, onValueChange = { backflushDays = it }, label = { Text("Backflush Days") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            Text("Usage Limits (Cycles/Cups)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Optional. Overrides 'Days' if consumption is high.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = waterCycles, onValueChange = { waterCycles = it }, label = { Text("Water Filter Cycles") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = descaleCycles, onValueChange = { descaleCycles = it }, label = { Text("Descale Cycles") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = backflushCycles, onValueChange = { backflushCycles = it }, label = { Text("Backflush Cycles") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

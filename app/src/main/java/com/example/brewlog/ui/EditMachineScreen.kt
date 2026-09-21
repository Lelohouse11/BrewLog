package com.example.brewlog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.brewlog.R
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
                title = { Text(stringResource(R.string.machine_specs)) },
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
                Icon(Icons.Default.Done, stringResource(R.string.save))
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
            Text("Maschinen-Foto", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
                            onClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        ) {
                            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                                Icon(Icons.Default.Edit, "Foto ändern", modifier = Modifier.padding(8.dp))
                            }
                        }
                    } else {
                        OutlinedButton(onClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Default.AddAPhoto, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Foto hinzufügen")
                        }
                    }
                }
            }

            HorizontalDivider()
            Text("Spezifikationen", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text(stringResource(R.string.brand)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text(stringResource(R.string.model)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = consumption, 
                onValueChange = { consumption = it }, 
                label = { Text("Wöchentlicher Verbrauch (Tassen)") }, 
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            HorizontalDivider()
            Text("Hardware", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = diameter, 
                onValueChange = { diameter = it }, 
                label = { Text("${stringResource(R.string.portafilter_diameter)} (mm)") }, 
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasGrinder, onCheckedChange = { hasGrinder = it })
                Text(stringResource(R.string.integrated_grinder))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasSteamWand, onCheckedChange = { hasSteamWand = it })
                Text(stringResource(R.string.steam_wand))
            }

            HorizontalDivider()
            Text("Wartungs-Intervalle (Tage)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(value = waterDays, onValueChange = { waterDays = it }, label = { Text(stringResource(R.string.water_filter_replacement)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = descaleDays, onValueChange = { descaleDays = it }, label = { Text(stringResource(R.string.descaling)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = backflushDays, onValueChange = { backflushDays = it }, label = { Text(stringResource(R.string.backflushing)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            Spacer(Modifier.height(100.dp))
        }
    }
}

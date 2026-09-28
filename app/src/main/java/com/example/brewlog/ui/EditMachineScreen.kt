package com.example.brewlog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.brewlog.R
import com.example.brewlog.data.EspressoMachine
import com.example.brewlog.ui.components.CremaGlassCard
import com.example.brewlog.ui.components.cremaGlow
import com.example.brewlog.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMachineScreen(
    viewModel: BrewViewModel,
    onNavigateBack: () -> Unit,
    geminiViewModel: GeminiViewModel = viewModel()
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

    var waterGuide by remember { mutableStateOf("") }
    var descaleGuide by remember { mutableStateOf("") }
    var backflushGuide by remember { mutableStateOf("") }

    val isGeneratingAi by geminiViewModel.isLoading.collectAsState()
    val aiGeneratedGuide by geminiViewModel.generatedGuide.collectAsState()
    var activeAiTaskTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(aiGeneratedGuide) {
        val guide = aiGeneratedGuide
        if (!guide.isNullOrEmpty() && activeAiTaskTarget != null) {
            when (activeAiTaskTarget) {
                "water" -> waterGuide = guide
                "descale" -> descaleGuide = guide
                "backflush" -> backflushGuide = guide
            }
            activeAiTaskTarget = null
            geminiViewModel.clearGeneratedGuide()
        }
    }

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

            waterGuide = it.customWaterFilterGuide
            descaleGuide = it.customDescaleGuide
            backflushGuide = it.customBackflushGuide
        }
    }

    val isDark = isAppInDarkTheme()
    val glassBg = if (isDark) EspressoGlassBg else VellumGlassBg
    val glowColor = if (isDark) CremaAmber else CremaAmberDark

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.machine_specs), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Surface(
                onClick = {
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
                        backflushLimitCycles = backflushCycles.toIntOrNull() ?: 0,
                        customWaterFilterGuide = waterGuide,
                        customDescaleGuide = descaleGuide,
                        customBackflushGuide = backflushGuide
                    )
                    viewModel.updateEspressoMachine(updated)
                    onNavigateBack()
                },
                shape = RoundedCornerShape(18.dp),
                color = glassBg,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .cremaGlow(color = glowColor, borderRadius = 18.dp, glowRadius = 6.dp, alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = "Speichern",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
            Spacer(Modifier.height(8.dp))

            // Photo Section
            CremaGlassCard {
                Text(stringResource(R.string.machine_photo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.3f)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = stringResource(R.string.machine_photo),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Icon(Icons.Default.Edit, stringResource(R.string.change_photo), modifier = Modifier.padding(8.dp))
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.add_photo))
                            }
                        }
                    }
                }
            }

            // Specifications Section
            CremaGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.specifications), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text(stringResource(R.string.brand)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.model)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = consumption, 
                    onValueChange = { consumption = it }, 
                    label = { Text(stringResource(R.string.weekly_consumption_cups)) }, 
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Hardware Section
            CremaGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.hardware), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                OutlinedTextField(
                    value = diameter, 
                    onValueChange = { diameter = it }, 
                    label = { Text("${stringResource(R.string.portafilter_diameter)} (mm)") }, 
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasGrinder, onCheckedChange = { hasGrinder = it })
                    Text(stringResource(R.string.integrated_grinder), style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasSteamWand, onCheckedChange = { hasSteamWand = it })
                    Text(stringResource(R.string.steam_wand), style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Maintenance Intervals Section
            CremaGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.maintenance_intervals_days), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                OutlinedTextField(
                    value = waterDays,
                    onValueChange = { waterDays = it },
                    label = { Text(stringResource(R.string.water_filter_replacement)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = descaleDays,
                    onValueChange = { descaleDays = it },
                    label = { Text(stringResource(R.string.descaling)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = backflushDays,
                    onValueChange = { backflushDays = it },
                    label = { Text(stringResource(R.string.backflushing)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Custom Maintenance Guides (Optional)
            CremaGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.custom_guides_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Water Filter Guide
                Text(
                    stringResource(R.string.water_filter_guide_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = waterGuide,
                    onValueChange = { waterGuide = it },
                    placeholder = { Text(stringResource(R.string.custom_guide_hint)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            activeAiTaskTarget = "water"
                            geminiViewModel.generateMaintenanceGuide(brand, model, "water")
                        },
                        enabled = !isGeneratingAi && brand.isNotBlank() && model.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (isGeneratingAi && activeAiTaskTarget == "water") {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(4.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(stringResource(R.string.ai_generate_short), style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { waterGuide = "" },
                        enabled = waterGuide.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.clear_guide), style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Descaling Guide
                Text(
                    stringResource(R.string.descale_guide_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = descaleGuide,
                    onValueChange = { descaleGuide = it },
                    placeholder = { Text(stringResource(R.string.custom_guide_hint)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            activeAiTaskTarget = "descale"
                            geminiViewModel.generateMaintenanceGuide(brand, model, "descale")
                        },
                        enabled = !isGeneratingAi && brand.isNotBlank() && model.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (isGeneratingAi && activeAiTaskTarget == "descale") {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(4.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(stringResource(R.string.ai_generate_short), style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { descaleGuide = "" },
                        enabled = descaleGuide.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.clear_guide), style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Backflushing Guide
                Text(
                    stringResource(R.string.backflush_guide_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = backflushGuide,
                    onValueChange = { backflushGuide = it },
                    placeholder = { Text(stringResource(R.string.custom_guide_hint)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            activeAiTaskTarget = "backflush"
                            geminiViewModel.generateMaintenanceGuide(brand, model, "backflush")
                        },
                        enabled = !isGeneratingAi && brand.isNotBlank() && model.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (isGeneratingAi && activeAiTaskTarget == "backflush") {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(4.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(stringResource(R.string.ai_generate_short), style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { backflushGuide = "" },
                        enabled = backflushGuide.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.clear_guide), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}

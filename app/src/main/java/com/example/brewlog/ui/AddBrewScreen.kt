package com.example.brewlog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brewlog.R
import com.example.brewlog.data.BrewLog

val FLAVOR_TAG_OPTIONS = listOf(
    "Chocolate", "Nutty", "Fruity", "Floral", "Caramel",
    "Berry", "Citrus", "Spices", "Earthy", "Smoke"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBrewScreen(
    onSave: (BrewLog) -> Unit,
    onNavigateBack: () -> Unit,
    existingLog: BrewLog? = null,
    geminiViewModel: GeminiViewModel = viewModel()
) {
    // Basic Info
    var coffeeName by remember { mutableStateOf(existingLog?.coffeeName ?: "") }
    var roaster by remember { mutableStateOf(existingLog?.roaster ?: "") }
    var grindSize by remember { mutableStateOf(existingLog?.grindSize?.takeIf { it > 0 }?.toString() ?: "") }
    
    // Optional Sections Toggles
    var hasRoastLevel by remember { mutableStateOf(existingLog?.roastLevel?.isNotEmpty() ?: false) }
    var roastLevel by remember { mutableStateOf(existingLog?.roastLevel ?: "Medium") }

    // Validation State
    val showErrors = remember { mutableStateOf(false) }

    // Optional: Rating
    var hasRating by remember { mutableStateOf(existingLog?.hasRating ?: false) }
    var rating by remember { mutableStateOf(existingLog?.rating?.toFloat() ?: 5f) }
    
    // Optional: Blend Settings
    var hasBlendSettings by remember { mutableStateOf(existingLog?.hasBlendSettings ?: false) }
    var arabicaPercent by remember { mutableIntStateOf(existingLog?.arabicaPercentage ?: 0) }
    var robustaPercent by remember { mutableIntStateOf(existingLog?.robustaPercentage ?: 0) }
    var excelsaPercent by remember { mutableIntStateOf(existingLog?.excelsaPercentage ?: 0) }
    var libericaPercent by remember { mutableIntStateOf(existingLog?.libericaPercentage ?: 0) }

    val totalPercentage = arabicaPercent + robustaPercent + excelsaPercent + libericaPercent
    val isBlendValid = !hasBlendSettings || totalPercentage == 100

    // Basket Grams (Direct Input)
    var singleGrams by remember { mutableStateOf(existingLog?.singleGrams?.toString() ?: "9.0") }
    var doubleGrams by remember { mutableStateOf(existingLog?.doubleGrams?.toString() ?: "18.0") }
    
    // Optional: Sensory Profile (1-5)
    var hasSensoryProfile by remember { mutableStateOf(existingLog?.hasSensoryProfile ?: false) }
    var sweetness by remember { mutableStateOf(existingLog?.sweetness?.toFloat() ?: 3f) }
    var acidity by remember { mutableStateOf(existingLog?.acidity?.toFloat() ?: 3f) }
    var body by remember { mutableStateOf(existingLog?.body?.toFloat() ?: 3f) }
    var bitterness by remember { mutableStateOf(existingLog?.bitterness?.toFloat() ?: 3f) }
    
    // Optional: Flavor Tags
    var hasFlavorTags by remember { mutableStateOf(existingLog?.hasFlavorTags ?: false) }
    var selectedFlavorTags by remember { mutableStateOf(existingLog?.flavorTags?.toSet() ?: emptySet<String>()) }

    // Custom Notes
    var notes by remember { mutableStateOf(existingLog?.notes ?: "") }

    // Observation of AI Scan Results
    val scanResult by geminiViewModel.scanResult.collectAsState()
    val isScanning by geminiViewModel.isLoading.collectAsState()
    val scanError by geminiViewModel.errorMessage.collectAsState()

    // Auto-fill logic when scan completes
    LaunchedEffect(scanResult) {
        scanResult?.let {
            if (it.coffeeName.isNotEmpty()) {
                coffeeName = it.coffeeName
                showErrors.value = false
            }
            if (it.roaster.isNotEmpty()) {
                roaster = it.roaster
                showErrors.value = false
            }
            
            if (it.roastLevel.isNotEmpty()) {
                hasRoastLevel = true
                roastLevel = it.roastLevel
            }
            
            if (it.hasSensoryProfile) {
                hasSensoryProfile = true
                sweetness = it.sweetness.toFloat()
                acidity = it.acidity.toFloat()
                body = it.body.toFloat()
                bitterness = it.bitterness.toFloat()
            }
            if (it.hasFlavorTags) {
                hasFlavorTags = true
                selectedFlavorTags = it.flavorTags.toSet()
            }

            if (it.hasBlendSettings) {
                hasBlendSettings = true
                arabicaPercent = it.arabicaPercentage
                robustaPercent = it.robustaPercentage
                excelsaPercent = it.excelsaPercentage
                libericaPercent = it.libericaPercentage
            }

            geminiViewModel.clearResult()
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { geminiViewModel.scanLabel(it) }
    }

    val sGrams = singleGrams.toDoubleOrNull() ?: 0.0
    val dGrams = doubleGrams.toDoubleOrNull() ?: 0.0
    
    val isNameMissing = coffeeName.isBlank()
    val isRoasterMissing = roaster.isBlank()
    val isValid = !isNameMissing && !isRoasterMissing && isBlendValid

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existingLog == null) stringResource(R.string.add_brew) else stringResource(R.string.edit_brew)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isValid) {
                        onSave(
                            BrewLog(
                                id = existingLog?.id ?: 0,
                                coffeeName = coffeeName,
                                roaster = roaster,
                                grindSize = grindSize.toFloatOrNull() ?: 0f,
                                roastLevel = if (hasRoastLevel) roastLevel else "",
                                hasRating = hasRating,
                                rating = if (hasRating) rating.toInt() else 0,
                                hasBlendSettings = hasBlendSettings,
                                arabicaPercentage = if (hasBlendSettings) arabicaPercent else 0,
                                robustaPercentage = if (hasBlendSettings) robustaPercent else 0,
                                excelsaPercentage = if (hasBlendSettings) excelsaPercent else 0,
                                libericaPercentage = if (hasBlendSettings) libericaPercent else 0,
                                isSingleSelected = true,
                                singleGrams = sGrams,
                                isDoubleSelected = true,
                                doubleGrams = dGrams,
                                hasSensoryProfile = hasSensoryProfile,
                                sweetness = sweetness.toInt(),
                                acidity = acidity.toInt(),
                                body = body.toInt(),
                                bitterness = bitterness.toInt(),
                                hasFlavorTags = hasFlavorTags,
                                flavorTags = if (hasFlavorTags) selectedFlavorTags.toList() else emptyList(),
                                notes = notes
                            )
                        )
                    } else {
                        showErrors.value = true
                    }
                },
                containerColor = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (isValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // AI Scanner Section
            OutlinedButton(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Etikett scannen...")
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Kaffee-Etikett scannen (KI)")
                }
            }

            if (scanError != null) {
                Text(text = scanError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            // --- SECTION 1: COFFEE IDENTITY ---
            BrewSectionCard(title = stringResource(R.string.coffee_identity), icon = Icons.Default.Coffee) {
                OutlinedTextField(
                    value = coffeeName,
                    onValueChange = { 
                        coffeeName = it 
                        if (it.isNotBlank()) showErrors.value = false
                    },
                    label = { Text("${stringResource(R.string.coffee_name)} *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showErrors.value && isNameMissing
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = roaster,
                    onValueChange = { 
                        roaster = it 
                        if (it.isNotBlank()) showErrors.value = false
                    },
                    label = { Text("${stringResource(R.string.roaster)} *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showErrors.value && isRoasterMissing
                )

                Spacer(Modifier.height(12.dp))

                // Roast Level
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasRoastLevel, onCheckedChange = { hasRoastLevel = it })
                        Text("${stringResource(R.string.roast_level)} hinzufügen", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (hasRoastLevel) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp)) {
                            listOf("Light", "Medium", "Dark").forEach { level ->
                                FilterChip(
                                    selected = roastLevel == level,
                                    onClick = { roastLevel = level },
                                    label = { Text(level) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Blend Composition
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasBlendSettings, onCheckedChange = { hasBlendSettings = it })
                        Text("Blend-Mischung (%) hinzufügen", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (hasBlendSettings) {
                        Spacer(Modifier.height(8.dp))
                        
                        PercentageSlider(
                            label = "Arabica", 
                            value = arabicaPercent, 
                            maxAllowed = 100 - (robustaPercent + excelsaPercent + libericaPercent)
                        ) { arabicaPercent = it }
                        
                        PercentageSlider(
                            label = "Robusta", 
                            value = robustaPercent, 
                            maxAllowed = 100 - (arabicaPercent + excelsaPercent + libericaPercent)
                        ) { robustaPercent = it }
                        
                        PercentageSlider(
                            label = "Excelsa", 
                            value = excelsaPercent, 
                            maxAllowed = 100 - (arabicaPercent + robustaPercent + libericaPercent)
                        ) { excelsaPercent = it }
                        
                        PercentageSlider(
                            label = "Liberica", 
                            value = libericaPercent, 
                            maxAllowed = 100 - (arabicaPercent + robustaPercent + excelsaPercent)
                        ) { libericaPercent = it }

                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Gesamt-Prozent:", 
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$totalPercentage%",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (totalPercentage == 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        if (totalPercentage != 100) {
                            Text(
                                "Summe muss genau 100% ergeben",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // --- SECTION 2: PREPARATION SETTINGS ---
            BrewSectionCard(title = stringResource(R.string.preparation_settings), icon = Icons.Default.Settings) {
                OutlinedTextField(
                    value = grindSize,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                            grindSize = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.grind_size)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.basket_size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = singleGrams,
                        onValueChange = { singleGrams = it },
                        label = { Text(stringResource(R.string.single_basket)) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("9.0") },
                        suffix = { Text("g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = doubleGrams,
                        onValueChange = { doubleGrams = it },
                        label = { Text(stringResource(R.string.double_basket)) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("18.0") },
                        suffix = { Text("g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }

            // --- SECTION 3: EVALUATION ---
            BrewSectionCard(title = stringResource(R.string.sensory_evaluation), icon = Icons.Default.Stars) {
                // Rating
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasRating, onCheckedChange = { hasRating = it })
                        Text("${stringResource(R.string.rating)} hinzufügen", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (hasRating) {
                        Text("${stringResource(R.string.rating)}: ${rating.toInt()}/10", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 12.dp))
                        Slider(
                            value = rating,
                            onValueChange = { rating = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Sensory Profile
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasSensoryProfile, onCheckedChange = { hasSensoryProfile = it })
                        Text("${stringResource(R.string.sensory_profile)} hinzufügen", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    if (hasSensoryProfile) {
                        Spacer(Modifier.height(8.dp))
                        SensorySlider(stringResource(R.string.sweetness), sweetness) { sweetness = it }
                        SensorySlider(stringResource(R.string.acidity), acidity) { acidity = it }
                        SensorySlider(stringResource(R.string.body), body) { body = it }
                        SensorySlider(stringResource(R.string.bitterness), bitterness) { bitterness = it }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Flavor Tags
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasFlavorTags, onCheckedChange = { hasFlavorTags = it })
                        Text("${stringResource(R.string.flavor_tags)} hinzufügen", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (hasFlavorTags) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FLAVOR_TAG_OPTIONS.forEach { tag ->
                                FilterChip(
                                    selected = selectedFlavorTags.contains(tag),
                                    onClick = {
                                        selectedFlavorTags = if (selectedFlavorTags.contains(tag)) {
                                            selectedFlavorTags - tag
                                        } else {
                                            selectedFlavorTags + tag
                                        }
                                    },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 4: ADDITIONAL INFO ---
            BrewSectionCard(title = stringResource(R.string.additional_info), icon = Icons.Default.EditNote) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("z.B. Espresso Röstung, Röstdatum...") },
                    minLines = 3,
                    maxLines = 5
                )
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun BrewSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            content()
        }
    }
}

@Composable
fun SensorySlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${value.toInt()}/5", style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 1f..5f,
            steps = 3
        )
    }
}

@Composable
fun PercentageSlider(label: String, value: Int, maxAllowed: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$value%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { 
                val newValue = it.toInt()
                if (newValue <= maxAllowed) {
                    onValueChange(newValue)
                }
            },
            valueRange = 0f..100f
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddBrewScreenPreview() {
    MaterialTheme {
        AddBrewScreen(onSave = {}, onNavigateBack = {})
    }
}

package com.example.brewlog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brewlog.data.BrewLog

private val FLAVOR_TAG_OPTIONS = listOf(
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
    var showErrors by remember { mutableStateOf(false) }

    // Optional: Rating
    var hasRating by remember { mutableStateOf(existingLog?.hasRating ?: false) }
    var rating by remember { mutableStateOf(existingLog?.rating?.toFloat() ?: 5f) }
    
    // Optional: Blend Settings
    var hasBlendSettings by remember { mutableStateOf(existingLog?.hasBlendSettings ?: false) }
    var arabicaPercentage by remember { mutableStateOf(existingLog?.arabicaPercentage?.takeIf { it > 0 }?.toString() ?: "") }
    var robustaPercentage by remember { mutableStateOf(existingLog?.robustaPercentage?.takeIf { it > 0 }?.toString() ?: "") }
    var excelsaPercentage by remember { mutableStateOf(existingLog?.excelsaPercentage?.takeIf { it > 0 }?.toString() ?: "") }
    var libericaPercentage by remember { mutableStateOf(existingLog?.libericaPercentage?.takeIf { it > 0 }?.toString() ?: "") }

    // Basket Grams (Direct Input)
    var singleGrams by remember { mutableStateOf(existingLog?.singleGrams?.takeIf { it > 0 }?.toString() ?: "") }
    var doubleGrams by remember { mutableStateOf(existingLog?.doubleGrams?.takeIf { it > 0 }?.toString() ?: "") }
    
    // Optional: Sensory Profile (1-5)
    var hasSensoryProfile by remember { mutableStateOf(existingLog?.hasSensoryProfile ?: false) }
    var sweetness by remember { mutableStateOf(existingLog?.sweetness?.toFloat() ?: 3f) }
    var acidity by remember { mutableStateOf(existingLog?.acidity?.toFloat() ?: 3f) }
    var body by remember { mutableStateOf(existingLog?.body?.toFloat() ?: 3f) }
    var bitterness by remember { mutableStateOf(existingLog?.bitterness?.toFloat() ?: 3f) }
    
    // Optional: Flavor Tags
    var hasFlavorTags by remember { mutableStateOf(existingLog?.hasFlavorTags ?: false) }
    var selectedFlavorTags by remember { mutableStateOf(existingLog?.flavorTags?.toSet() ?: emptySet<String>()) }

    // Observation of AI Scan Results
    val scanResult by geminiViewModel.scanResult.collectAsState()
    val isScanning by geminiViewModel.isLoading.collectAsState()
    val scanError by geminiViewModel.errorMessage.collectAsState()

    // Auto-fill logic when scan completes
    LaunchedEffect(scanResult) {
        scanResult?.let {
            if (it.coffeeName.isNotEmpty()) {
                coffeeName = it.coffeeName
                showErrors = false
            }
            if (it.roaster.isNotEmpty()) {
                roaster = it.roaster
                showErrors = false
            }
            
            // Auto-fill Roast
            if (it.roastLevel.isNotEmpty()) {
                hasRoastLevel = true
                roastLevel = it.roastLevel
            }
            
            // New Auto-fill for Sensory and Tags
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

            // New Auto-fill for Blend Settings
            if (it.hasBlendSettings) {
                hasBlendSettings = true
                if (it.arabicaPercentage > 0) arabicaPercentage = it.arabicaPercentage.toString()
                if (it.robustaPercentage > 0) robustaPercentage = it.robustaPercentage.toString()
                if (it.excelsaPercentage > 0) excelsaPercentage = it.excelsaPercentage.toString()
                if (it.libericaPercentage > 0) libericaPercentage = it.libericaPercentage.toString()
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
    val isValid = !isNameMissing && !isRoasterMissing

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existingLog == null) "Add Brew Setting" else "Edit Brew Setting") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            // Truly floating button at the bottom right
            ExtendedFloatingActionButton(
                onClick = {
                    if (isValid) {
                        onSave(
                            BrewLog(
                                id = existingLog?.id ?: 0,
                                coffeeName = coffeeName,
                                roaster = roaster,
                                grindSize = grindSize.toIntOrNull() ?: 0,
                                roastLevel = if (hasRoastLevel) roastLevel else "",
                                hasRating = hasRating,
                                rating = if (hasRating) rating.toInt() else 0,
                                hasBlendSettings = hasBlendSettings,
                                arabicaPercentage = if (hasBlendSettings) arabicaPercentage.toIntOrNull() ?: 0 else 0,
                                robustaPercentage = if (hasBlendSettings) robustaPercentage.toIntOrNull() ?: 0 else 0,
                                excelsaPercentage = if (hasBlendSettings) excelsaPercentage.toIntOrNull() ?: 0 else 0,
                                libericaPercentage = if (hasBlendSettings) libericaPercentage.toIntOrNull() ?: 0 else 0,
                                isSingleSelected = sGrams > 0,
                                singleGrams = sGrams,
                                isDoubleSelected = dGrams > 0,
                                doubleGrams = dGrams,
                                hasSensoryProfile = hasSensoryProfile,
                                sweetness = sweetness.toInt(),
                                acidity = acidity.toInt(),
                                body = body.toInt(),
                                bitterness = bitterness.toInt(),
                                hasFlavorTags = hasFlavorTags,
                                flavorTags = if (hasFlavorTags) selectedFlavorTags.toList() else emptyList()
                            )
                        )
                    } else {
                        showErrors = true
                    }
                },
                containerColor = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (isValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer,
                icon = { Icon(Icons.Default.Done, contentDescription = null) },
                text = { Text(if (existingLog == null) "Save Setting" else "Update Setting") }
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Section 0: AI Label Scanner
            OutlinedButton(
                onClick = { cameraLauncher.launch() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Analyzing Label...")
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Scan Coffee Label (AI)")
                }
            }

            if (scanError != null) {
                Text(text = scanError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            // Section 1: Coffee Basics
            Text("Coffee Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = coffeeName,
                onValueChange = { 
                    coffeeName = it 
                    if (it.isNotBlank()) showErrors = false
                },
                label = { Text("Coffee Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = showErrors && isNameMissing,
                supportingText = {
                    if (showErrors && isNameMissing) {
                        Text("Name is required")
                    }
                }
            )

            OutlinedTextField(
                value = roaster,
                onValueChange = { 
                    roaster = it 
                    if (it.isNotBlank()) showErrors = false
                },
                label = { Text("Roaster / Company *") },
                modifier = Modifier.fillMaxWidth(),
                isError = showErrors && isRoasterMissing,
                supportingText = {
                    if (showErrors && isRoasterMissing) {
                        Text("Roaster is required")
                    }
                }
            )

            OutlinedTextField(
                value = grindSize,
                onValueChange = { grindSize = it },
                label = { Text("Grind Size (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Roast Level is now optional via checkbox
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasRoastLevel, onCheckedChange = { hasRoastLevel = it })
                    Text("Add Roast Level", style = MaterialTheme.typography.bodyLarge)
                }
                if (hasRoastLevel) {
                    Text("Roast Level: $roastLevel", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 12.dp, top = 4.dp)) {
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

            // Section 2: Optional Rating
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasRating, onCheckedChange = { hasRating = it })
                    Text("Add Overall Rating", style = MaterialTheme.typography.bodyLarge)
                }
                if (hasRating) {
                    Text("Rating: ${rating.toInt()}/10", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 12.dp))
                    Slider(
                        value = rating,
                        onValueChange = { rating = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            HorizontalDivider()

            // Section 3: Optional Blend Settings
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasBlendSettings, onCheckedChange = { hasBlendSettings = it })
                    Text("Add Blend Composition (%)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }

                if (hasBlendSettings) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = arabicaPercentage,
                            onValueChange = { arabicaPercentage = it },
                            label = { Text("Arabica") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = robustaPercentage,
                            onValueChange = { robustaPercentage = it },
                            label = { Text("Robusta") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = excelsaPercentage,
                            onValueChange = { excelsaPercentage = it },
                            label = { Text("Excelsa") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = libericaPercentage,
                            onValueChange = { libericaPercentage = it },
                            label = { Text("Liberica") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Section 4: Basket Settings
            Text("Basket Settings (Grams)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = singleGrams,
                    onValueChange = { singleGrams = it },
                    label = { Text("Single") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("0.0") },
                    suffix = { Text("g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = doubleGrams,
                    onValueChange = { doubleGrams = it },
                    label = { Text("Double") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("0.0") },
                    suffix = { Text("g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            HorizontalDivider()

            // Section 5: Optional Sensory Profile
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasSensoryProfile, onCheckedChange = { hasSensoryProfile = it })
                    Text("Add Sensory Profile", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                
                if (hasSensoryProfile) {
                    Spacer(Modifier.height(8.dp))
                    SensorySlider("Sweetness", sweetness) { sweetness = it }
                    SensorySlider("Acidity", acidity) { acidity = it }
                    SensorySlider("Body", body) { body = it }
                    SensorySlider("Bitterness", bitterness) { bitterness = it }
                }
            }

            HorizontalDivider()

            // Section 6: Optional Flavor Tags
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasFlavorTags, onCheckedChange = { hasFlavorTags = it })
                    Text("Add Flavor Tags", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
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
            
            // Add extra space at bottom so FAB doesn't cover content
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SensorySlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
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

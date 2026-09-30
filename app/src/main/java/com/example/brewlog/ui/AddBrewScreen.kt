package com.example.brewlog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brewlog.R
import com.example.brewlog.data.BrewLog
import com.example.brewlog.ui.components.*
import com.example.brewlog.ui.theme.*

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
    onNavigateToSettings: () -> Unit = {},
    geminiViewModel: GeminiViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val gramsStepSize by settingsViewModel.gramsStepSize.collectAsState()
    val grindStepSize by settingsViewModel.grindStepSize.collectAsState()
    val aiEnabled by settingsViewModel.aiEnabled.collectAsState()
    val isApiKeyConfigured by settingsViewModel.isApiKeyConfigured.collectAsState()
    var showAiSetupDialog by remember { mutableStateOf(false) }

    // Basic Info
    var coffeeName by remember { mutableStateOf(existingLog?.coffeeName ?: "") }
    var roaster by remember { mutableStateOf(existingLog?.roaster ?: "") }
    var roastDate by remember { mutableStateOf(existingLog?.roastDate ?: "") }
    var grindSize by remember { mutableStateOf(existingLog?.grindSize?.takeIf { it > 0 }?.toString() ?: "") }
    
    // Optional Sections Toggles
    var hasRoastLevel by remember { mutableStateOf(existingLog?.roastLevel?.isNotEmpty() ?: false) }
    var roastLevel by remember { mutableStateOf(existingLog?.roastLevel ?: "Medium") }

    // Validation State
    val showErrors = remember { mutableStateOf(false) }

    // Optional: Rating
    var hasRating by remember { mutableStateOf(existingLog?.hasRating ?: false) }
    var rating by remember { mutableFloatStateOf(existingLog?.rating?.toFloat() ?: 5f) }
    
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
    var sweetness by remember { mutableFloatStateOf(existingLog?.sweetness?.toFloat() ?: 3f) }
    var acidity by remember { mutableFloatStateOf(existingLog?.acidity?.toFloat() ?: 3f) }
    var body by remember { mutableFloatStateOf(existingLog?.body?.toFloat() ?: 3f) }
    var bitterness by remember { mutableFloatStateOf(existingLog?.bitterness?.toFloat() ?: 3f) }
    
    // Optional: Flavor Tags
    var hasFlavorTags by remember { mutableStateOf(existingLog?.hasFlavorTags ?: false) }
    var selectedFlavorTags by remember { mutableStateOf(existingLog?.flavorTags?.toSet() ?: emptySet<String>()) }

    // Custom Notes
    var notes by remember { mutableStateOf(existingLog?.notes ?: "") }

    // Population Effect when existingLog is loaded or updated
    LaunchedEffect(existingLog) {
        existingLog?.let { log ->
            coffeeName = log.coffeeName
            roaster = log.roaster
            roastDate = log.roastDate
            grindSize = log.grindSize.takeIf { it > 0 }?.toString() ?: ""
            hasRoastLevel = log.roastLevel.isNotEmpty()
            roastLevel = if (log.roastLevel.isNotEmpty()) log.roastLevel else "Medium"
            hasRating = log.hasRating
            rating = log.rating.toFloat()
            hasBlendSettings = log.hasBlendSettings
            arabicaPercent = log.arabicaPercentage
            robustaPercent = log.robustaPercentage
            excelsaPercent = log.excelsaPercentage
            libericaPercent = log.libericaPercentage
            singleGrams = log.singleGrams.toString()
            doubleGrams = log.doubleGrams.toString()
            hasSensoryProfile = log.hasSensoryProfile
            sweetness = log.sweetness.toFloat()
            acidity = log.acidity.toFloat()
            body = log.body.toFloat()
            bitterness = log.bitterness.toFloat()
            hasFlavorTags = log.hasFlavorTags
            selectedFlavorTags = log.flavorTags.toSet()
            notes = log.notes
        }
    }

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

            if (it.roastDate.isNotEmpty()) {
                roastDate = it.roastDate
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
                title = { Text(if (existingLog == null) stringResource(R.string.add_brew) else stringResource(R.string.edit_brew), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            val isDark = isAppInDarkTheme()
            val glassBg = if (isDark) EspressoGlassBg else VellumGlassBg

            Surface(
                onClick = {
                    if (isValid) {
                        onSave(
                            BrewLog(
                                id = existingLog?.id ?: 0,
                                coffeeName = coffeeName,
                                roaster = roaster,
                                grindSize = grindSize.toFloatOrNull() ?: 0f,
                                roastLevel = if (hasRoastLevel) roastLevel else "",
                                roastDate = roastDate,
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
                shape = RoundedCornerShape(18.dp),
                color = glassBg,
                border = BorderStroke(1.5.dp, if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = stringResource(R.string.save),
                        tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
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

            if (aiEnabled) {
                // AI Scanner Section
                AiFeatureButton(
                    onClick = {
                        if (isApiKeyConfigured) {
                            cameraLauncher.launch(null)
                        } else {
                            showAiSetupDialog = true
                        }
                    },
                    title = stringResource(R.string.scan_label_ai),
                    subtitle = stringResource(R.string.scan_label_ai_subtitle),
                    icon = Icons.Default.CameraAlt,
                    isLoading = isScanning,
                    loadingText = stringResource(R.string.scanning_label)
                )

                if (scanError != null) {
                    Text(text = scanError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }

            if (showAiSetupDialog) {
                AiSetupPromptDialog(
                    onDismiss = { showAiSetupDialog = false },
                    onGoToSettings = onNavigateToSettings
                )
            }

            // --- SECTION 1: COFFEE IDENTITY ---
            CremaGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Coffee, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.coffee_identity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                OutlinedTextField(
                    value = coffeeName,
                    onValueChange = { 
                        coffeeName = it 
                        if (it.isNotBlank()) showErrors.value = false
                    },
                    label = { Text("${stringResource(R.string.coffee_name)} *") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    isError = showErrors.value && isNameMissing,
                    supportingText = if (showErrors.value && isNameMissing) { { Text(stringResource(R.string.error_field_required)) } } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = roaster,
                    onValueChange = { 
                        roaster = it 
                        if (it.isNotBlank()) showErrors.value = false
                    },
                    label = { Text("${stringResource(R.string.roaster)} *") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    isError = showErrors.value && isRoasterMissing,
                    supportingText = if (showErrors.value && isRoasterMissing) { { Text(stringResource(R.string.error_field_required)) } } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                RoastDatePickerField(
                    value = roastDate,
                    onValueChange = { roastDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Roast Level
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { hasRoastLevel = !hasRoastLevel }
                    ) {
                        Checkbox(checked = hasRoastLevel, onCheckedChange = { hasRoastLevel = it })
                        Text(stringResource(R.string.add_roast_level), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (hasRoastLevel) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp)) {
                            listOf("Light", "Medium", "Dark").forEach { level ->
                                FilterChip(
                                    selected = roastLevel == level,
                                    onClick = { roastLevel = level },
                                    shape = RoundedCornerShape(12.dp),
                                    label = { Text(level) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Blend Composition
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { hasBlendSettings = !hasBlendSettings }
                    ) {
                        Checkbox(checked = hasBlendSettings, onCheckedChange = { hasBlendSettings = it })
                        Text(stringResource(R.string.blend_composition_pct), style = MaterialTheme.typography.bodyMedium)
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
                                stringResource(R.string.total_percentage), 
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
                                stringResource(R.string.blend_must_be_100),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // --- SECTION 2: PREPARATION SETTINGS ---
            CremaGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.preparation_settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                GrindSizeStepper(
                    value = grindSize,
                    onValueChange = { grindSize = it },
                    stepSize = grindStepSize,
                    label = stringResource(R.string.grind_size),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.basket_size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GramsStepper(
                        value = singleGrams,
                        onValueChange = { singleGrams = it },
                        stepSize = gramsStepSize,
                        label = stringResource(R.string.single_basket),
                        modifier = Modifier.fillMaxWidth()
                    )
                    GramsStepper(
                        value = doubleGrams,
                        onValueChange = { doubleGrams = it },
                        stepSize = gramsStepSize,
                        label = stringResource(R.string.double_basket),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- SECTION 3: EVALUATION ---
            CremaGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.sensory_evaluation),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Rating
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { hasRating = !hasRating }
                    ) {
                        Checkbox(checked = hasRating, onCheckedChange = { hasRating = it })
                        Text(stringResource(R.string.add_rating), style = MaterialTheme.typography.bodyMedium)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { hasSensoryProfile = !hasSensoryProfile }
                    ) {
                        Checkbox(checked = hasSensoryProfile, onCheckedChange = { hasSensoryProfile = it })
                        Text(stringResource(R.string.add_sensory_profile), style = MaterialTheme.typography.bodyMedium)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { hasFlavorTags = !hasFlavorTags }
                    ) {
                        Checkbox(checked = hasFlavorTags, onCheckedChange = { hasFlavorTags = it })
                        Text(stringResource(R.string.add_flavor_tags), style = MaterialTheme.typography.bodyMedium)
                    }

                    if (hasFlavorTags) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FLAVOR_TAG_OPTIONS.forEach { tag ->
                                FlavorTagChip(
                                    tag = tag,
                                    isSelected = selectedFlavorTags.contains(tag),
                                    onClick = {
                                        selectedFlavorTags = if (selectedFlavorTags.contains(tag)) {
                                            selectedFlavorTags - tag
                                        } else {
                                            selectedFlavorTags + tag
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 4: ADDITIONAL INFO ---
            CremaGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.additional_info),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.notes_placeholder)) },
                    minLines = 3,
                    maxLines = 5
                )
            }
            
            Spacer(modifier = Modifier.height(96.dp))
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

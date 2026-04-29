package com.example.brewlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    existingLog: BrewLog? = null
) {
    // Basic Info
    var coffeeName by remember { mutableStateOf(existingLog?.coffeeName ?: "") }
    var roaster by remember { mutableStateOf(existingLog?.roaster ?: "") }
    var grindSize by remember { mutableStateOf(existingLog?.grindSize?.toString() ?: "") }
    var roastLevel by remember { mutableStateOf(existingLog?.roastLevel ?: "Medium") }

    // Optional: Rating
    var hasRating by remember { mutableStateOf(existingLog?.hasRating ?: false) }
    var rating by remember { mutableStateOf(existingLog?.rating?.toFloat() ?: 5f) }
    
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

    val sGrams = singleGrams.toDoubleOrNull() ?: 0.0
    val dGrams = doubleGrams.toDoubleOrNull() ?: 0.0
    val isSaveEnabled = coffeeName.isNotBlank() && (sGrams > 0 || dGrams > 0)

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
                    if (isSaveEnabled) {
                        onSave(
                            BrewLog(
                                id = existingLog?.id ?: 0,
                                coffeeName = coffeeName,
                                roaster = roaster,
                                grindSize = grindSize.toIntOrNull() ?: 0,
                                roastLevel = roastLevel,
                                hasRating = hasRating,
                                rating = if (hasRating) rating.toInt() else 0,
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
                    }
                },
                containerColor = if (isSaveEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSaveEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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

            // Section 1: Coffee Basics
            Text("Coffee Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = coffeeName,
                onValueChange = { coffeeName = it },
                label = { Text("Coffee Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = roaster,
                onValueChange = { roaster = it },
                label = { Text("Roaster / Company") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = grindSize,
                onValueChange = { grindSize = it },
                label = { Text("Grind Size") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Column {
                Text("Roast Level: $roastLevel", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Light", "Medium", "Dark").forEach { level ->
                        FilterChip(
                            selected = roastLevel == level,
                            onClick = { roastLevel = level },
                            label = { Text(level) }
                        )
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

            // Section 3: Basket Settings
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

            // Section 4: Optional Sensory Profile
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

            // Section 5: Optional Flavor Tags
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

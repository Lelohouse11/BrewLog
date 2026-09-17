package com.example.brewlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brewlog.data.ShotLog
import com.example.brewlog.util.ExtractionEngine
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialInScreen(
    viewModel: BrewViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    initialBeanId: Int? = null,
    initialBasketType: String? = null,
    initialDose: Double? = null,
    initialGrindSize: Float? = null
) {
    val beans by viewModel.allLogs.collectAsState()

    var selectedBeanId by remember { mutableStateOf(initialBeanId ?: -1) }
    var basketType by remember { mutableStateOf(initialBasketType ?: "double") } // "single" | "double"
    var doseIn by remember { mutableStateOf(initialDose?.toString() ?: "") }
    var grindSize by remember { mutableStateOf(initialGrindSize?.toString() ?: "") }

    val selectedBean = beans.find { it.id == selectedBeanId }

    // Auto-fill when bean or basket changes
    LaunchedEffect(selectedBeanId, basketType) {
        if (selectedBean != null && initialDose == null) {
            doseIn = if (basketType == "single") selectedBean.singleGrams.toString() else selectedBean.doubleGrams.toString()
            grindSize = selectedBean.grindSize.toString()
        }
    }

    var target by remember { mutableStateOf<ExtractionEngine.ShotTarget?>(null) }
    LaunchedEffect(selectedBeanId, basketType, doseIn) {
        val dose = doseIn.toFloatOrNull() ?: 0f
        if (selectedBeanId != -1) {
            val lastBalanced = viewModel.getLatestBalancedShot(selectedBeanId, basketType)
            target = ExtractionEngine.calculateTarget(lastBalanced, dose)
        }
    }

    // Timer State
    var isRunning by remember { mutableStateOf(false) }
    var timeElapsed by remember { mutableLongStateOf(0L) } // Milliseconds

    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis() - timeElapsed
            while (isRunning) {
                timeElapsed = System.currentTimeMillis() - startTime
                delay(10)
            }
        }
    }

    // Post-Shot State
    var yieldOut by remember { mutableStateOf("") }
    var acidityEval by remember { mutableIntStateOf(0) }
    var bitternessEval by remember { mutableIntStateOf(0) }
    var bodyEval by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dial-In & Shot-Tracking") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Phase A: Setup ---
            BrewSectionCard(title = "Setup", icon = Icons.Default.Coffee) {
                // Bean Picker
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isRunning) expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedBean?.coffeeName ?: "Select Bean",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isRunning,
                        label = { Text("Bean") },
                        trailingIcon = { if (!isRunning) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    if (!isRunning) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            beans.forEach { bean ->
                                DropdownMenuItem(
                                    text = { Text(bean.coffeeName) },
                                    onClick = {
                                        selectedBeanId = bean.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Basket Toggle
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = basketType == "single",
                        onClick = { if (!isRunning) basketType = "single" },
                        enabled = !isRunning,
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Single")
                    }
                    SegmentedButton(
                        selected = basketType == "double",
                        onClick = { if (!isRunning) basketType = "double" },
                        enabled = !isRunning,
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Double")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = doseIn,
                        onValueChange = { doseIn = it },
                        enabled = !isRunning,
                        label = { Text("Dose (g)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = grindSize,
                        onValueChange = { grindSize = it },
                        enabled = !isRunning,
                        label = { Text("Grind Size") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                target?.let {
                    Text(
                        text = "Target: ${"%.1f".format(it.yieldOut)}g in ${it.timeSecRange.start.toInt()}-${it.timeSecRange.endInclusive.toInt()}s" +
                                if (it.isFromHistory) " (from history)" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // --- Phase B: Timer ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val seconds = timeElapsed / 1000f
                    Text(
                        text = "%.1f s".format(seconds),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { isRunning = !isRunning },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isRunning) "STOP" else "START", fontSize = 20.sp)
                    }
                    if (!isRunning && timeElapsed > 0) {
                        TextButton(onClick = { timeElapsed = 0L }) {
                            Text("Reset")
                        }
                    }
                }
            }

            // --- Phase C: Results & Logging ---
                if (!isRunning && timeElapsed > 0) {
                BrewSectionCard(title = "Results", icon = Icons.AutoMirrored.Filled.Assignment) {
                    OutlinedTextField(
                        value = yieldOut,
                        onValueChange = { yieldOut = it },
                        label = { Text("Yield Out (g)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    val y = yieldOut.toFloatOrNull() ?: 0f
                    val d = doseIn.toFloatOrNull() ?: 0f
                    val t = timeElapsed / 1000f
                    if (y > 0 && d > 0 && t > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ratio: 1:${"%.1f".format(y / d)}", style = MaterialTheme.typography.labelLarge)
                            Text("Flow: ${"%.1f".format(y / t)} g/s", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Sensory Delta Evaluation", style = MaterialTheme.typography.labelMedium)
                    
                    EvaluationRow("Acidity", acidityEval, listOf("Too Sour", "Balanced", "Flat")) { acidityEval = it }
                    EvaluationRow("Bitterness", bitternessEval, listOf("Under-ex.", "Sweet", "Bitter")) { bitternessEval = it }
                    EvaluationRow("Body", bodyEval, listOf("Thin", "Optimal", "Heavy")) { bodyEval = it }

                    val recommendation = ExtractionEngine.getRecommendation(
                        ExtractionEngine.ShotMetrics(
                            doseIn = d,
                            grindSize = grindSize.toFloatOrNull() ?: 0f,
                            timeSec = t,
                            yieldOut = y,
                            acidity = when(acidityEval) {
                                -1 -> ExtractionEngine.AcidityEval.TOOSOUR
                                1 -> ExtractionEngine.AcidityEval.FLAT
                                else -> ExtractionEngine.AcidityEval.BALANCED
                            },
                            bitterness = when(bitternessEval) {
                                -1 -> ExtractionEngine.BitternessEval.UNDER
                                1 -> ExtractionEngine.BitternessEval.BITTER
                                else -> ExtractionEngine.BitternessEval.SWEET
                            },
                            body = when(bodyEval) {
                                -1 -> ExtractionEngine.BodyEval.THIN
                                1 -> ExtractionEngine.BodyEval.HEAVY
                                else -> ExtractionEngine.BodyEval.OPTIMAL
                            }
                        )
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        color = if (recommendation.puckPrepWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) 
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(recommendation.diagnosis, fontWeight = FontWeight.Bold, color = if (recommendation.puckPrepWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            Text(recommendation.explanation, style = MaterialTheme.typography.bodySmall)
                            
                            if (recommendation.suggestedGrindChange != 0f || recommendation.suggestedYieldChange != 0f) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = buildString {
                                        if (recommendation.suggestedGrindChange != 0f) {
                                            append("Recommended Grind: ${"%.1f".format(recommendation.recommendedGrindSize)}")
                                        }
                                        if (recommendation.suggestedYieldChange != 0f) {
                                            if (isNotEmpty()) append(" | ")
                                            append("Recommended Yield: ${"%.1f".format(recommendation.recommendedYieldOut)}g")
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )

                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            saveShot(viewModel, selectedBeanId, basketType, doseIn, grindSize, yieldOut, t, acidityEval, bitternessEval, bodyEval, notes, false, recommendation)
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log Shot")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            saveShot(viewModel, selectedBeanId, basketType, doseIn, grindSize, yieldOut, t, acidityEval, bitternessEval, bodyEval, notes, true, recommendation)
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val label = if (recommendation.diagnosis == "Balanced Extraction") "Log & Save Current Settings" else "Log & Save Recommended Settings"
                        Text(label)
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun EvaluationRow(label: String, value: Int, labels: List<String>, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(-1, 0, 1).forEachIndexed { index, i ->
                SegmentedButton(
                    selected = value == i,
                    onClick = { onValueChange(i) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                    label = { Text(labels[index], fontSize = 10.sp) }
                )
            }
        }
    }
}

private fun saveShot(
    viewModel: BrewViewModel,
    beanId: Int,
    basketType: String,
    doseIn: String,
    grindSize: String,
    yieldOut: String,
    time: Float,
    acidity: Int,
    bitterness: Int,
    body: Int,
    notes: String,
    updateBean: Boolean,
    recommendation: ExtractionEngine.DialInRecommendation
) {
    val shot = ShotLog(
        beanId = beanId,
        basketType = basketType,
        grindSize = grindSize.toFloatOrNull() ?: 0f,
        doseIn = doseIn.toFloatOrNull() ?: 0f,
        yieldOut = yieldOut.toFloatOrNull() ?: 0f,
        extractionTimeSec = time,
        acidityEval = acidity,
        bitternessEval = bitterness,
        bodyEval = body,
        notes = notes
    )

    if (updateBean) {
        viewModel.addShotLog(shot, false) // Log with current values
        // Update bean with RECOMMENDED values
        viewModel.updateBeanFromRecommendation(
            beanId = beanId,
            basketType = basketType,
            recommendedGrind = recommendation.recommendedGrindSize,
            recommendedDose = shot.doseIn.toDouble(), // Dose usually stays the same in this logic
            recommendedYield = recommendation.recommendedYieldOut.toDouble()
        )
    } else {
        viewModel.addShotLog(shot, false)
    }
}

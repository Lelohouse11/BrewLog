package com.example.brewlog.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brewlog.data.BrewLog
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

    var selectedBeanId by remember { mutableIntStateOf(initialBeanId ?: -1) }
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

    // Step state: 0 = Setup, 1 = During Shot, 2 = Result
    var currentStep by remember { mutableIntStateOf(0) }

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

    // Calculate recommendation based on current inputs
    val recommendation = remember(doseIn, grindSize, timeElapsed, yieldOut, acidityEval, bitternessEval, bodyEval) {
        val d = doseIn.toFloatOrNull() ?: 0f
        val g = grindSize.toFloatOrNull() ?: 0f
        val t = timeElapsed / 1000f
        val y = yieldOut.toFloatOrNull() ?: 0f

        ExtractionEngine.getRecommendation(
            ExtractionEngine.ShotMetrics(
                doseIn = d,
                grindSize = g,
                timeSec = t,
                yieldOut = y,
                acidity = when (acidityEval) {
                    -1 -> ExtractionEngine.AcidityEval.TOOSOUR
                    1 -> ExtractionEngine.AcidityEval.FLAT
                    else -> ExtractionEngine.AcidityEval.BALANCED
                },
                bitterness = when (bitternessEval) {
                    -1 -> ExtractionEngine.BitternessEval.UNDER
                    1 -> ExtractionEngine.BitternessEval.BITTER
                    else -> ExtractionEngine.BitternessEval.SWEET
                },
                body = when (bodyEval) {
                    -1 -> ExtractionEngine.BodyEval.THIN
                    1 -> ExtractionEngine.BodyEval.HEAVY
                    else -> ExtractionEngine.BodyEval.OPTIMAL
                }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dial-In & Extraktion") },
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
                .fillMaxSize()
        ) {
            // Stepper Header
            DialInStepHeader(
                currentStep = currentStep,
                onStepSelected = { step ->
                    if (step < currentStep || isStepUnlocked(step, selectedBeanId, doseIn, grindSize, timeElapsed, yieldOut)) {
                        currentStep = step
                    }
                }
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Step Content with Animation
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                    } else {
                        slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                    }
                },
                label = "StepTransition",
                modifier = Modifier.weight(1f)
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(8.dp))
                    when (step) {
                        0 -> SetupStep(
                            beans = beans,
                            selectedBean = selectedBean,
                            selectedBeanId = selectedBeanId,
                            onBeanSelected = { selectedBeanId = it },
                            basketType = basketType,
                            onBasketTypeChange = { basketType = it },
                            doseIn = doseIn,
                            onDoseChange = { doseIn = it },
                            grindSize = grindSize,
                            onGrindChange = { grindSize = it },
                            target = target,
                            isRunning = isRunning,
                            onNextClick = { currentStep = 1 }
                        )

                        1 -> ShotStep(
                            selectedBeanName = selectedBean?.coffeeName ?: "Keine Bohne gewählt",
                            basketType = basketType,
                            doseIn = doseIn,
                            grindSize = grindSize,
                            target = target,
                            isRunning = isRunning,
                            timeElapsed = timeElapsed,
                            onToggleTimer = { isRunning = !isRunning },
                            onResetTimer = { timeElapsed = 0L },
                            yieldOut = yieldOut,
                            onYieldChange = { yieldOut = it },
                            onBackClick = { currentStep = 0 },
                            onNextClick = { currentStep = 2 }
                        )

                        2 -> ResultStep(
                            doseIn = doseIn,
                            grindSize = grindSize,
                            timeElapsed = timeElapsed,
                            yieldOut = yieldOut,
                            acidityEval = acidityEval,
                            onAcidityChange = { acidityEval = it },
                            bitternessEval = bitternessEval,
                            onBitternessChange = { bitternessEval = it },
                            bodyEval = bodyEval,
                            onBodyChange = { bodyEval = it },
                            recommendation = recommendation,
                            notes = notes,
                            onNotesChange = { notes = it },
                            onSaveOnly = {
                                val t = timeElapsed / 1000f
                                saveShot(viewModel, selectedBeanId, basketType, doseIn, grindSize, yieldOut, t, acidityEval, bitternessEval, bodyEval, notes, false, recommendation)
                                onNavigateBack()
                            },
                            onSaveAndApplySettings = {
                                val t = timeElapsed / 1000f
                                saveShot(viewModel, selectedBeanId, basketType, doseIn, grindSize, yieldOut, t, acidityEval, bitternessEval, bodyEval, notes, true, recommendation)
                                onNavigateBack()
                            },
                            onStartNewShot = {
                                val t = timeElapsed / 1000f
                                saveShot(viewModel, selectedBeanId, basketType, doseIn, grindSize, yieldOut, t, acidityEval, bitternessEval, bodyEval, notes, true, recommendation)
                                // Apply recommendation to current inputs & restart at step 1
                                grindSize = recommendation.recommendedGrindSize.toString()
                                yieldOut = ""
                                timeElapsed = 0L
                                isRunning = false
                                acidityEval = 0
                                bitternessEval = 0
                                bodyEval = 0
                                notes = ""
                                currentStep = 0
                            },
                            onBackClick = { currentStep = 1 }
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun isStepUnlocked(
    step: Int,
    selectedBeanId: Int,
    doseIn: String,
    grindSize: String,
    timeElapsed: Long,
    yieldOut: String
): Boolean {
    return when (step) {
        0 -> true
        1 -> selectedBeanId != -1 && doseIn.toFloatOrNull() != null && grindSize.toFloatOrNull() != null
        2 -> timeElapsed > 0 || yieldOut.toFloatOrNull() != null
        else -> false
    }
}

@Composable
private fun DialInStepHeader(
    currentStep: Int,
    onStepSelected: (Int) -> Unit
) {
    val steps = listOf(
        StepData("Setup", "Vorbereitung", Icons.Default.Coffee),
        StepData("Shot", "Extraktion", Icons.Default.Timer),
        StepData("Ergebnis", "Bewertung", Icons.Default.CheckCircle)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = index == currentStep
            val isCompleted = index < currentStep

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onStepSelected(index) }
                    .padding(vertical = 4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isActive -> MaterialTheme.colorScheme.primary
                        isCompleted -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = when {
                        isActive -> MaterialTheme.colorScheme.onPrimary
                        isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = step.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (index < steps.size - 1) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    thickness = 1.5.dp,
                    color = if (index < currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

private data class StepData(val title: String, val subtitle: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupStep(
    beans: List<BrewLog>,
    selectedBean: BrewLog?,
    selectedBeanId: Int,
    onBeanSelected: (Int) -> Unit,
    basketType: String,
    onBasketTypeChange: (String) -> Unit,
    doseIn: String,
    onDoseChange: (String) -> Unit,
    grindSize: String,
    onGrindChange: (String) -> Unit,
    target: ExtractionEngine.ShotTarget?,
    isRunning: Boolean,
    onNextClick: () -> Unit
) {
    val canProceed = selectedBeanId != -1 && doseIn.toFloatOrNull() != null && grindSize.toFloatOrNull() != null

    BrewSectionCard(title = "1. Bohnen & Siebträger Wahl", icon = Icons.Default.Coffee) {
        // Bean Dropdown
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (!isRunning) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedBean?.let { "${it.coffeeName} (${it.roaster})" } ?: "Kaffeebohne wählen",
                onValueChange = {},
                readOnly = true,
                enabled = !isRunning,
                label = { Text("Kaffeebohne") },
                trailingIcon = { if (!isRunning) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                    .fillMaxWidth()
            )
            if (!isRunning) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (beans.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Keine Bohnen im Log vorhanden") },
                            onClick = { expanded = false }
                        )
                    } else {
                        beans.forEach { bean ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(bean.coffeeName, fontWeight = FontWeight.Bold)
                                        Text(bean.roaster, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                },
                                onClick = {
                                    onBeanSelected(bean.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Basket Toggle
        Text("Sieb-Größe (Basket)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = basketType == "single",
                onClick = { if (!isRunning) onBasketTypeChange("single") },
                enabled = !isRunning,
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Einer (Single)")
            }
            SegmentedButton(
                selected = basketType == "double",
                onClick = { if (!isRunning) onBasketTypeChange("double") },
                enabled = !isRunning,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Zweier (Double)")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Inputs for Dose & Grind
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = doseIn,
                onValueChange = onDoseChange,
                enabled = !isRunning,
                label = { Text("Dosis in g (In)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text("g") }
            )
            OutlinedTextField(
                value = grindSize,
                onValueChange = onGrindChange,
                enabled = !isRunning,
                label = { Text("Mahlgrad") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }

    // Target Info Box
    target?.let { t ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Ziel-Parameter (Target)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Ziel-Menge (Yield)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text("${"%.1f".format(t.yieldOut)} g", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Ziel-Zeit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text("${t.timeSecRange.start.toInt()} - ${t.timeSecRange.endInclusive.toInt()} s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Ratio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        val doseVal = doseIn.toFloatOrNull() ?: 1f
                        val ratioVal = if (doseVal > 0) t.yieldOut / doseVal else 2f
                        Text("1 : ${"%.1f".format(ratioVal)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (t.isFromHistory) "• Basierend auf deinen früheren ausgewogenen Shots" else "• Standard 1:2 Ziel-Empfehlung",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Next Button
    Button(
        onClick = onNextClick,
        enabled = canProceed,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text("Weiter zum Shot-Timer", fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }
}

@Composable
private fun ShotStep(
    selectedBeanName: String,
    basketType: String,
    doseIn: String,
    grindSize: String,
    target: ExtractionEngine.ShotTarget?,
    isRunning: Boolean,
    timeElapsed: Long,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    yieldOut: String,
    onYieldChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    // Active Setup Info Banner
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    selectedBeanName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${if (basketType == "single") "Single" else "Double"} • Dosis: ${doseIn}g • Mahlgrad: $grindSize",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            OutlinedButton(
                onClick = onBackClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                enabled = !isRunning
            ) {
                Text("Setup bearbeiten", fontSize = 12.sp)
            }
        }
    }

    // Timer Card
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
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            target?.let { t ->
                val timeSec = timeElapsed / 1000f
                val statusText = when {
                    timeSec == 0f -> "Ziel: ${t.timeSecRange.start.toInt()}-${t.timeSecRange.endInclusive.toInt()} s"
                    timeSec in t.timeSecRange -> "✓ Im Zielbereich"
                    timeSec < t.timeSecRange.start -> "Unter Zielzeit (<${t.timeSecRange.start.toInt()}s)"
                    else -> "Über Zielzeit (>${t.timeSecRange.endInclusive.toInt()}s)"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (timeSec in t.timeSecRange) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onToggleTimer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isRunning) "STOPP" else "START", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            if (!isRunning && timeElapsed > 0) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onResetTimer) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Timer zurücksetzen")
                }
            }
        }
    }

    // Yield Output Input
    BrewSectionCard(title = "2. Espresso Ausbeute (Yield)", icon = Icons.AutoMirrored.Filled.Assignment) {
        OutlinedTextField(
            value = yieldOut,
            onValueChange = onYieldChange,
            label = { Text("Erhaltene Menge in g (Yield Out)") },
            placeholder = { Text("z.B. 36.0") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = { Text("g") }
        )

        val y = yieldOut.toFloatOrNull() ?: 0f
        val d = doseIn.toFloatOrNull() ?: 0f
        val t = timeElapsed / 1000f

        if (y > 0 && d > 0) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ratio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text("1 : ${"%.1f".format(y / d)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (t > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Flussrate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text("${"%.1f".format(y / t)} g/s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Navigation Buttons
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Text("Zurück")
        }

        Button(
            onClick = onNextClick,
            enabled = timeElapsed > 0 || yieldOut.toFloatOrNull() != null,
            modifier = Modifier
                .weight(1.5f)
                .height(52.dp)
        ) {
            Text("Weiter zu Ergebnis", fontSize = 15.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun ResultStep(
    doseIn: String,
    grindSize: String,
    timeElapsed: Long,
    yieldOut: String,
    acidityEval: Int,
    onAcidityChange: (Int) -> Unit,
    bitternessEval: Int,
    onBitternessChange: (Int) -> Unit,
    bodyEval: Int,
    onBodyChange: (Int) -> Unit,
    recommendation: ExtractionEngine.DialInRecommendation,
    notes: String,
    onNotesChange: (String) -> Unit,
    onSaveOnly: () -> Unit,
    onSaveAndApplySettings: () -> Unit,
    onStartNewShot: () -> Unit,
    onBackClick: () -> Unit
) {
    val d = doseIn.toFloatOrNull() ?: 0f
    val y = yieldOut.toFloatOrNull() ?: 0f
    val t = timeElapsed / 1000f

    // Shot Summary Card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Extraktions-Zusammenfassung", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("In / Out", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("${"%.1f".format(d)}g / ${"%.1f".format(y)}g", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Zeit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("${"%.1f".format(t)} s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Ratio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    val ratio = if (d > 0) y / d else 0f
                    Text("1 : ${"%.1f".format(ratio)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Flussrate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    val flow = if (t > 0) y / t else 0f
                    Text("${"%.1f".format(flow)} g/s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Sensory Evaluation
    BrewSectionCard(title = "3. Geschmacksprofil (Sensory Delta)", icon = Icons.Default.ThumbUp) {
        EvaluationRow("Säure (Acidity)", acidityEval, listOf("Zu Sauer", "Ausgewogen", "Flach")) { onAcidityChange(it) }
        EvaluationRow("Bitterkeit (Bitterness)", bitternessEval, listOf("Unterextr.", "Süß", "Bitter")) { onBitternessChange(it) }
        EvaluationRow("Körper (Body)", bodyEval, listOf("Dünn", "Optimal", "Schwer")) { onBodyChange(it) }
    }

    // Recommendation Card
    val cardColor = when {
        recommendation.puckPrepWarning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        recommendation.diagnosis == "Balanced Extraction" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    }

    val iconVector = when {
        recommendation.puckPrepWarning -> Icons.Default.Warning
        recommendation.diagnosis == "Balanced Extraction" -> Icons.Default.CheckCircle
        else -> Icons.Default.Tune
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    iconVector,
                    contentDescription = null,
                    tint = if (recommendation.puckPrepWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    recommendation.diagnosis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (recommendation.puckPrepWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                recommendation.explanation,
                style = MaterialTheme.typography.bodyMedium
            )

            if (recommendation.suggestedGrindChange != 0f || recommendation.suggestedYieldChange != 0f) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))

                Text("Empfohlene Anpassungen:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))

                if (recommendation.suggestedGrindChange != 0f) {
                    val direction = if (recommendation.suggestedGrindChange > 0) "gröber" else "feiner"
                    Text(
                        "• Mahlgrad $direction stellen: ${"%.1f".format(recommendation.recommendedGrindSize)} (aktuell $grindSize)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (recommendation.suggestedYieldChange != 0f) {
                    Text(
                        "• Yield anpassen: ${"%.1f".format(recommendation.recommendedYieldOut)}g (aktuell $yieldOut g)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Notes Field
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("Notizen (Optional)") },
        placeholder = { Text("Puck prep, Crema, Aroma-Noten...") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    // Save and Flow Actions
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onSaveAndApplySettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            val label = if (recommendation.diagnosis == "Balanced Extraction")
                "Shot loggen & Einstellungen behalten"
            else
                "Shot loggen & Empfehlungen speichern"
            Text(label, fontSize = 15.sp)
        }

        OutlinedButton(
            onClick = onSaveOnly,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Nur Shot loggen (Bohnen-Setup unverändert)")
        }

        TextButton(
            onClick = onStartNewShot,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Shot loggen & nächsten Test-Shot starten")
        }

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zurück zum Shot-Timer")
        }
    }
}

@Composable
fun EvaluationRow(label: String, value: Int, labels: List<String>, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(-1, 0, 1).forEachIndexed { index, i ->
                SegmentedButton(
                    selected = value == i,
                    onClick = { onValueChange(i) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                    label = { Text(labels[index], fontSize = 11.sp) }
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

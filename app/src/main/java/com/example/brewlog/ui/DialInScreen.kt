package com.example.brewlog.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brewlog.R
import com.example.brewlog.data.BrewLog
import com.example.brewlog.data.ShotLog
import com.example.brewlog.ui.components.*
import com.example.brewlog.ui.theme.*
import com.example.brewlog.util.ExtractionEngine
import com.example.brewlog.util.getLocalizedDiagnosis
import com.example.brewlog.util.getLocalizedExplanation
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialInScreen(
    viewModel: BrewViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onShotLogged: () -> Unit = {},
    initialBeanId: Int? = null,
    initialBasketType: String? = null,
    initialDose: Double? = null,
    initialGrindSize: Float? = null
) {
    val beans by viewModel.allLogs.collectAsState()

    var selectedBeanId by remember { mutableIntStateOf(initialBeanId ?: -1) }
    var basketType by remember { mutableStateOf(initialBasketType ?: "double") }
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
    var timeElapsed by remember { mutableLongStateOf(0L) }

    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis() - timeElapsed
            var hapticFired = false
            while (isRunning) {
                timeElapsed = System.currentTimeMillis() - startTime
                if (!hapticFired && timeElapsed >= 25000L) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    hapticFired = true
                }
                delay(10.milliseconds)
            }
        }
    }

    // Post-Shot State
    var yieldOut by remember { mutableStateOf("") }
    var acidityEval by remember { mutableIntStateOf(0) }
    var bitternessEval by remember { mutableIntStateOf(0) }
    var bodyEval by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }

    val settingsViewModel: SettingsViewModel = viewModel()
    val gramsStepSize by settingsViewModel.gramsStepSize.collectAsState()
    val grindStepSize by settingsViewModel.grindStepSize.collectAsState()

    val recommendation = remember(doseIn, grindSize, timeElapsed, yieldOut, acidityEval, bitternessEval, bodyEval, grindStepSize, gramsStepSize) {
        val d = doseIn.toFloatOrNull() ?: 0f
        val g = grindSize.toFloatOrNull() ?: 0f
        val t = timeElapsed / 1000f
        val y = yieldOut.toFloatOrNull() ?: 0f

        ExtractionEngine.getRecommendation(
            metrics = ExtractionEngine.ShotMetrics(
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
            ),
            grindStepSize = grindStepSize,
            gramsStepSize = gramsStepSize
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
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

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

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
                            onBackClick = onNavigateBack,
                            onNextClick = { currentStep = 1 }
                        )

                        1 -> ShotStep(
                            selectedBeanName = selectedBean?.coffeeName ?: stringResource(R.string.select_bean),
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
                                onShotLogged()
                            },
                            onSaveAndApplySettings = {
                                val t = timeElapsed / 1000f
                                saveShot(viewModel, selectedBeanId, basketType, doseIn, grindSize, yieldOut, t, acidityEval, bitternessEval, bodyEval, notes, true, recommendation)
                                onShotLogged()
                            },
                            onStartNewShot = {
                                val t = timeElapsed / 1000f
                                saveShot(viewModel, selectedBeanId, basketType, doseIn, grindSize, yieldOut, t, acidityEval, bitternessEval, bodyEval, notes, true, recommendation)
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
                    Spacer(Modifier.height(130.dp))
                }
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
        StepData(stringResource(R.string.step_setup_title), stringResource(R.string.step_setup_subtitle), Icons.Default.Coffee),
        StepData(stringResource(R.string.step_shot_title), stringResource(R.string.step_shot_subtitle), Icons.Default.Timer),
        StepData(stringResource(R.string.step_result_title), stringResource(R.string.step_result_subtitle), Icons.Default.CheckCircle)
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
                    color = if (index < currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
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
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val canProceed = selectedBeanId != -1 && doseIn.toFloatOrNull() != null && grindSize.toFloatOrNull() != null

    CremaGlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Coffee, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.beans_basket_choice),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
        }

        Spacer(Modifier.height(14.dp))

        // Bean Dropdown
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (!isRunning) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedBean?.let { "${it.coffeeName} (${it.roaster})" } ?: stringResource(R.string.select_bean),
                onValueChange = {},
                readOnly = true,
                enabled = !isRunning,
                label = { Text(stringResource(R.string.select_bean)) },
                trailingIcon = { if (!isRunning) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                    .fillMaxWidth()
            )
            if (!isRunning) {
                val isDark = isAppInDarkTheme()
                val dropdownBg = if (isDark) EspressoGlassBg else VellumGlassBg
                val dropdownBorder = if (isDark) CremaAmber.copy(alpha = 0.3f) else CremaAmberDark.copy(alpha = 0.3f)

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = dropdownBg,
                    border = BorderStroke(1.dp, dropdownBorder)
                ) {
                    if (beans.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_beans_available)) },
                            onClick = { expanded = false }
                        )
                    } else {
                        beans.forEach { bean ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(bean.coffeeName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        if (bean.roaster.isNotEmpty()) {
                                            Text(bean.roaster, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                onClick = {
                                    onBeanSelected(bean.id)
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Basket Toggle
        Text(stringResource(R.string.basket_size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = basketType == "single",
                onClick = { if (!isRunning) onBasketTypeChange("single") },
                enabled = !isRunning,
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.single_basket),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (basketType == "single") FontWeight.Bold else FontWeight.Medium
                )
            }
            SegmentedButton(
                selected = basketType == "double",
                onClick = { if (!isRunning) onBasketTypeChange("double") },
                enabled = !isRunning,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.double_basket),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (basketType == "double") FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val settingsViewModel: SettingsViewModel = viewModel()
        val gramsStepSize by settingsViewModel.gramsStepSize.collectAsState()
        val grindStepSize by settingsViewModel.grindStepSize.collectAsState()

        // Inputs for Dose & Grind
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GramsStepper(
                value = doseIn,
                onValueChange = onDoseChange,
                stepSize = gramsStepSize,
                label = stringResource(R.string.dose_in),
                modifier = Modifier.fillMaxWidth()
            )
            GrindSizeStepper(
                value = grindSize,
                onValueChange = onGrindChange,
                stepSize = grindStepSize,
                label = stringResource(R.string.grind_size),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Target Info Box with Visual Ratio Bar
    target?.let { t ->
        val doseVal = doseIn.toFloatOrNull() ?: 1f

        CremaGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.target_params),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = if (t.isFromHistory) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (t.isFromHistory) Icons.Default.Star else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (t.isFromHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (t.isFromHistory) stringResource(R.string.history_basis) else stringResource(R.string.standard_recipe),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (t.isFromHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Visual Brew Ratio Bar for Target
            BrewRatioBar(
                doseInGrams = doseVal,
                yieldInGrams = t.yieldOut,
                extractionTimeSec = (t.timeSecRange.start + t.timeSecRange.endInclusive) / 2f
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (t.isFromHistory) stringResource(R.string.target_history_hint) else stringResource(R.string.target_default_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    // Navigation Buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBackClick,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Text(stringResource(R.string.back), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onNextClick,
            enabled = canProceed,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Text(stringResource(R.string.next_to_shot), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
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
    CremaGlassCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    selectedBeanName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val basketLabel = if (basketType == "single") stringResource(R.string.single_basket) else stringResource(R.string.double_basket)
                Text(
                    "$basketLabel • ${doseIn}g • Grind $grindSize",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            OutlinedButton(
                onClick = onBackClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = !isRunning
            ) {
                Text(stringResource(R.string.edit_setup), fontSize = 12.sp)
            }
        }
    }

    val haptic = LocalHapticFeedback.current

    // Timer Card with Liquid Crema Gauge
    CremaGlassCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val seconds = timeElapsed / 1000f
            val targetMaxSec = target?.timeSecRange?.endInclusive ?: 30f
            val progressProgress = (seconds / targetMaxSec).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(targetValue = progressProgress, label = "timerProgress")

            val targetOk = target != null && seconds in target.timeSecRange
            val targetOver = target != null && seconds > target.timeSecRange.endInclusive

            // Pulsing animation when running
            val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
            val pulseScale by if (isRunning) {
                infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )
            } else {
                remember { mutableFloatStateOf(1f) }
            }

            val pulseAlpha by if (isRunning) {
                infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
            } else {
                remember { mutableFloatStateOf(0f) }
            }

            val targetScale by animateFloatAsState(
                targetValue = if (targetOk) 1.06f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "targetScale"
            )

            val indicatorColor by animateColorAsState(
                targetValue = when {
                    targetOk -> MaterialTheme.colorScheme.primary
                    targetOver -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                },
                animationSpec = tween(400),
                label = "indicatorColor"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            ) {
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = pulseAlpha
                            }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    )
                }

                if (targetOk) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    )
                }

                CircularProgressIndicator(
                    progress = { if (timeElapsed > 0) animatedProgress else 0f },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .graphicsLayer {
                            scaleX = targetScale
                            scaleY = targetScale
                        },
                    strokeWidth = 10.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    color = indicatorColor
                )

                val secondsFormatted = String.format(Locale.ROOT, "%.1f", seconds)
                val secondsLabel = stringResource(R.string.seconds_short)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = targetScale
                            scaleY = targetScale
                        }
                        .semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "$secondsFormatted $secondsLabel"
                        }
                ) {
                    Text(
                        text = String.format(Locale.ROOT, "%.1f", seconds),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 46.sp),
                        fontFamily = BaristaMonospaceFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (targetOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.seconds_short),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            target?.let { t ->
                val statusText = when {
                    seconds == 0f -> "${stringResource(R.string.target_time)}: ${t.timeSecRange.start.toInt()}-${t.timeSecRange.endInclusive.toInt()} s"
                    seconds in t.timeSecRange -> stringResource(R.string.target_range_ok)
                    seconds < t.timeSecRange.start -> stringResource(R.string.target_range_fast)
                    else -> stringResource(R.string.target_range_slow)
                }
                Surface(
                    color = if (targetOk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (targetOk) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleTimer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isRunning) stringResource(R.string.timer_stop) else stringResource(R.string.timer_start), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (!isRunning && timeElapsed > 0) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onResetTimer()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.reset_timer))
                }
            }
        }
    }

    // Yield Output Input
    CremaGlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.espresso_yield),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = yieldOut,
            onValueChange = onYieldChange,
            label = { Text(stringResource(R.string.yield_out)) },
            placeholder = { Text("36.0") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = { Text("g") }
        )

        val y = yieldOut.toFloatOrNull() ?: 0f
        val d = doseIn.toFloatOrNull() ?: 0f
        val t = timeElapsed / 1000f

        if (y > 0 && d > 0) {
            Spacer(Modifier.height(12.dp))
            BrewRatioBar(doseInGrams = d, yieldInGrams = y, extractionTimeSec = t)
        }
    }

    Spacer(Modifier.height(8.dp))

    // Navigation Buttons
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onBackClick,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Text(stringResource(R.string.back))
        }

        Button(
            onClick = onNextClick,
            enabled = timeElapsed > 0 || yieldOut.toFloatOrNull() != null,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1.5f)
                .height(52.dp)
        ) {
            Text(stringResource(R.string.next_to_result), fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

    // Shot Summary Card with Visual Ratio & Flow Bar
    BrewRatioBar(
        doseInGrams = d,
        yieldInGrams = y,
        extractionTimeSec = t
    )

    // Sensory Evaluation (Text / Buttons kept intact)
    CremaGlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.sensory_evaluation),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
        }

        Spacer(Modifier.height(8.dp))

        EvaluationRow(
            stringResource(R.string.acidity),
            acidityEval,
            listOf(stringResource(R.string.too_sour), stringResource(R.string.balanced), stringResource(R.string.flat))
        ) { onAcidityChange(it) }

        EvaluationRow(
            stringResource(R.string.bitterness),
            bitternessEval,
            listOf(stringResource(R.string.under_extracted), stringResource(R.string.sweet), stringResource(R.string.bitter))
        ) { onBitternessChange(it) }

        EvaluationRow(
            stringResource(R.string.body),
            bodyEval,
            listOf(stringResource(R.string.thin), stringResource(R.string.optimal), stringResource(R.string.heavy))
        ) { onBodyChange(it) }
    }

    // Recommendation Card
    val iconVector = when {
        recommendation.puckPrepWarning -> Icons.Default.Warning
        recommendation.type == ExtractionEngine.DiagnosisType.BALANCED -> Icons.Default.CheckCircle
        else -> Icons.Default.Tune
    }

    CremaGlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                iconVector,
                contentDescription = null,
                tint = if (recommendation.puckPrepWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                recommendation.getLocalizedDiagnosis(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (recommendation.puckPrepWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            recommendation.getLocalizedExplanation(),
            style = MaterialTheme.typography.bodyMedium
        )

        if (recommendation.suggestedGrindChange != 0f || recommendation.suggestedYieldChange != 0f) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            Text(
                stringResource(R.string.recommended_adjustments),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (recommendation.suggestedGrindChange != 0f) {
                    val direction = if (recommendation.suggestedGrindChange > 0)
                        stringResource(R.string.grind_direction_coarser)
                    else
                        stringResource(R.string.grind_direction_finer)

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(stringResource(R.string.grind_size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$grindSize ➔ ${recommendation.recommendedGrindSize} ($direction)",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = BaristaMonospaceFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (recommendation.suggestedYieldChange != 0f) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(stringResource(R.string.yield_out), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$yieldOut g ➔ ${recommendation.recommendedYieldOut} g",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = BaristaMonospaceFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Notes Field
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text(stringResource(R.string.notes)) },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    // Save and Flow Actions
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onSaveAndApplySettings,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            val label = if (recommendation.type == ExtractionEngine.DiagnosisType.BALANCED)
                stringResource(R.string.log_and_keep)
            else
                stringResource(R.string.log_and_apply)
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onSaveOnly,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.log_only), fontWeight = FontWeight.Medium)
        }

        TextButton(
            onClick = onStartNewShot,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.log_and_next_shot), fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onBackClick,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.back))
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
        viewModel.addShotLog(shot, false)
        viewModel.updateBeanFromRecommendation(
            beanId = beanId,
            basketType = basketType,
            recommendedGrind = recommendation.recommendedGrindSize,
            recommendedDose = shot.doseIn.toDouble(),
            recommendedYield = recommendation.recommendedYieldOut.toDouble()
        )
    } else {
        viewModel.addShotLog(shot, false)
    }
}

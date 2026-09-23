package com.example.brewlog.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brewlog.ui.theme.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimalist Coffee Bean vector icon drawn programmatically via Canvas.
 */
@Composable
fun CoffeeBeanIcon(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    isFilled: Boolean = true
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Outer bean ellipse path
        val beanPath = Path().apply {
            moveTo(width * 0.5f, 0f)
            cubicTo(width * 0.95f, height * 0.05f, width, height * 0.7f, width * 0.65f, height)
            cubicTo(width * 0.35f, height * 1.15f, 0f, height * 0.85f, 0f, height * 0.45f)
            cubicTo(0f, height * 0.15f, width * 0.15f, 0f, width * 0.5f, 0f)
            close()
        }

        // Center S-curve split line
        val centerLinePath = Path().apply {
            moveTo(width * 0.5f, height * 0.15f)
            cubicTo(
                width * 0.25f, height * 0.38f,
                width * 0.75f, height * 0.62f,
                width * 0.5f, height * 0.85f
            )
        }

        if (isFilled) {
            drawPath(path = beanPath, color = color)
            drawPath(
                path = centerLinePath,
                color = if (color == CremaAmber || color == DarkRoastBrown) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.4f),
                style = Stroke(
                    width = width * 0.1f,
                    cap = StrokeCap.Round
                )
            )
        } else {
            drawPath(
                path = beanPath,
                color = color,
                style = Stroke(width = width * 0.12f)
            )
            drawPath(
                path = centerLinePath,
                color = color,
                style = Stroke(
                    width = width * 0.08f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * Custom Rating Bar using 1 to 5 minimalist Coffee Beans.
 * Supports rating scales 1..10 (divided by 2) or 1..5 directly.
 */
@Composable
fun CoffeeBeanRating(
    rating: Int,
    modifier: Modifier = Modifier,
    maxRating: Int = 10,
    beanSize: Dp = 16.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
) {
    // Map maxRating (e.g., 10) to 5 beans scale
    val activeBeans = if (maxRating == 10) (rating + 1) / 2 else rating

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val isFilled = i <= activeBeans
            val beanScale by animateFloatAsState(
                targetValue = if (isFilled) 1.15f else 0.95f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "beanScale_$i"
            )
            CoffeeBeanIcon(
                modifier = Modifier
                    .size(beanSize)
                    .graphicsLayer {
                        scaleX = beanScale
                        scaleY = beanScale
                    },
                color = if (isFilled) activeColor else inactiveColor,
                isFilled = isFilled
            )
        }
    }
}

/**
 * Minimalist Roast Level Badge (Light, Medium, Dark).
 */
@Composable
fun RoastLevelBadge(
    roastLevel: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, labelText) = when (roastLevel.lowercase(Locale.ROOT)) {
        "light", "hell" -> Triple(
            LightRoastBadgeBg,
            LightRoastBadgeText,
            roastLevel.ifEmpty { "Light" }
        )
        "medium", "mittel" -> Triple(
            MediumRoastBadgeBg,
            MediumRoastBadgeText,
            roastLevel.ifEmpty { "Medium" }
        )
        "dark", "dunkel" -> Triple(
            DarkRoastBadgeBg,
            DarkRoastBadgeText,
            roastLevel.ifEmpty { "Dark" }
        )
        else -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            roastLevel
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Visual Brew Ratio Indicator Bar displaying dose in, yield out, and calculated ratio.
 */
@Composable
fun BrewRatioBar(
    doseInGrams: Float,
    yieldInGrams: Float,
    modifier: Modifier = Modifier,
    extractionTimeSec: Float = 0f
) {
    val ratio = if (doseInGrams > 0f) yieldInGrams / doseInGrams else 0f
    val ratioFormatted = String.format(Locale.ROOT, "%.1f", ratio)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dose In
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "IN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format(Locale.ROOT, "%.1f", doseInGrams)}g",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Ratio Divider / Icon
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "RATIO 1:$ratioFormatted",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (extractionTimeSec > 0f) {
                    Text(
                        text = "${extractionTimeSec.toInt()}s",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Yield Out
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "OUT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format(Locale.ROOT, "%.1f", yieldInGrams)}g",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Minimalist Line-Art Coffee Cup for Empty States.
 */
@Composable
fun CoffeeCupLineArt(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Cup Body
        val cupPath = Path().apply {
            moveTo(width * 0.2f, height * 0.25f)
            lineTo(width * 0.25f, height * 0.7f)
            cubicTo(
                width * 0.25f, height * 0.85f,
                width * 0.75f, height * 0.85f,
                width * 0.75f, height * 0.7f
            )
            lineTo(width * 0.8f, height * 0.25f)
            close()
        }

        // Cup Handle
        val handlePath = Path().apply {
            moveTo(width * 0.78f, height * 0.35f)
            cubicTo(
                width * 0.95f, height * 0.35f,
                width * 0.95f, height * 0.6f,
                width * 0.76f, height * 0.6f
            )
        }

        // Saucer
        val saucerPath = Path().apply {
            moveTo(width * 0.15f, height * 0.88f)
            lineTo(width * 0.85f, height * 0.88f)
        }

        // Steam line 1
        val steam1 = Path().apply {
            moveTo(width * 0.4f, height * 0.18f)
            cubicTo(width * 0.35f, height * 0.12f, width * 0.45f, height * 0.08f, width * 0.4f, 0f)
        }

        // Steam line 2
        val steam2 = Path().apply {
            moveTo(width * 0.6f, height * 0.18f)
            cubicTo(width * 0.55f, height * 0.12f, width * 0.65f, height * 0.08f, width * 0.6f, 0f)
        }

        val strokeWidth = width * 0.04f

        drawPath(cupPath, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
        drawPath(handlePath, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        drawPath(saucerPath, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        drawPath(steam1, color = color, style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round))
        drawPath(steam2, color = color, style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round))
    }
}

/**
 * Reusable Minimalist Coffee Empty State Component.
 */
@Composable
fun CoffeeEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String = "",
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CoffeeCupLineArt(
            modifier = Modifier.size(100.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(20.dp))
            actionButton()
        }
    }
}

/**
 * Custom Canvas-drawn 4-axis Sensory Radar Chart (Sweetness, Acidity, Body, Bitterness).
 * Subtle, clean level indicators 1..5 and axis labels at outer corners.
 */
@Composable
fun SensoryRadarChart(
    sweetness: Int,
    acidity: Int,
    body: Int,
    bitterness: Int,
    modifier: Modifier = Modifier,
    sweetnessLabel: String = "Sweetness",
    acidityLabel: String = "Acidity",
    bodyLabel: String = "Body",
    bitternessLabel: String = "Bitterness",
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val sweetTarget = (sweetness.coerceIn(1, 5) / 5.0f)
    val acidTarget = (acidity.coerceIn(1, 5) / 5.0f)
    val bodyTarget = (body.coerceIn(1, 5) / 5.0f)
    val bitterTarget = (bitterness.coerceIn(1, 5) / 5.0f)

    val sweetVal by animateFloatAsState(
        targetValue = sweetTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "sweetAnim"
    )
    val acidVal by animateFloatAsState(
        targetValue = acidTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "acidAnim"
    )
    val bodyVal by animateFloatAsState(
        targetValue = bodyTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bodyAnim"
    )
    val bitterVal by animateFloatAsState(
        targetValue = bitterTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bitterAnim"
    )

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
    val gridNumberStyle = TextStyle(
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = minOf(cx, cy) * 0.65f

            val angles = listOf(-PI / 2, 0.0, PI / 2, PI)
            val values = listOf(sweetVal, acidVal, bodyVal, bitterVal)
            val labels = listOf(sweetnessLabel, acidityLabel, bodyLabel, bitternessLabel)

            // 1. Grid Webs
            for (level in 1..5) {
                val scale = level / 5.0f
                val gridPath = Path()
                angles.forEachIndexed { index, angle ->
                    val r = radius * scale
                    val x = (cx + r * cos(angle)).toFloat()
                    val y = (cy + r * sin(angle)).toFloat()
                    if (index == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                }
                gridPath.close()

                drawPath(
                    path = gridPath,
                    color = if (level == 5) gridColor else gridColor.copy(alpha = 0.25f),
                    style = Stroke(width = if (level == 5) 1.dp.toPx() else 0.75.dp.toPx())
                )
            }

            // 2. Axis Lines
            angles.forEach { angle ->
                val x = (cx + radius * cos(angle)).toFloat()
                val y = (cy + radius * sin(angle)).toFloat()
                drawLine(
                    color = gridColor,
                    start = Offset(cx, cy),
                    end = Offset(x, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 3. Level Numbers 1..5
            for (level in 1..5) {
                val scale = level / 5.0f
                val numLayout = textMeasurer.measure(level.toString(), gridNumberStyle)
                val nh = numLayout.size.height.toFloat()

                val numX = cx + 3.dp.toPx()
                val numY = cy - (radius * scale) - (nh / 2f)

                drawText(
                    textLayoutResult = numLayout,
                    topLeft = Offset(numX, numY)
                )
            }

            // 4. Data Polygon
            val dataPath = Path()
            angles.forEachIndexed { index, angle ->
                val r = radius * values[index]
                val x = (cx + r * cos(angle)).toFloat()
                val y = (cy + r * sin(angle)).toFloat()
                if (index == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            drawPath(
                path = dataPath,
                color = primaryColor.copy(alpha = 0.25f)
            )
            drawPath(
                path = dataPath,
                color = primaryColor,
                style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
            )

            // Data Points
            angles.forEachIndexed { index, angle ->
                val r = radius * values[index]
                val x = (cx + r * cos(angle)).toFloat()
                val y = (cy + r * sin(angle)).toFloat()
                drawCircle(
                    color = primaryColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // 5. Corner Attribute Labels
            angles.forEachIndexed { index, _ ->
                val labelLayout = textMeasurer.measure(labels[index], labelStyle)
                val w = labelLayout.size.width.toFloat()
                val h = labelLayout.size.height.toFloat()

                val x: Float
                val y: Float

                when (index) {
                    0 -> { // Top (Sweetness)
                        x = cx - w / 2f
                        y = cy - radius - h - 4.dp.toPx()
                    }
                    1 -> { // Right (Acidity)
                        x = cx + radius + 6.dp.toPx()
                        y = cy - h / 2f
                    }
                    2 -> { // Bottom (Body)
                        x = cx - w / 2f
                        y = cy + radius + 4.dp.toPx()
                    }
                    else -> { // Left (Bitterness)
                        x = cx - radius - w - 6.dp.toPx()
                        y = cy - h / 2f
                    }
                }

                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Segmented Progress Bar displaying bean blend variety composition.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlendCompositionBar(
    arabica: Int,
    robusta: Int,
    modifier: Modifier = Modifier,
    excelsa: Int = 0,
    liberica: Int = 0
) {
    val total = (arabica + robusta + excelsa + liberica).coerceAtLeast(1)

    val items = listOf(
        BlendVariety("Arabica", arabica, CremaAmber),
        BlendVariety("Robusta", robusta, DarkRoastBrown),
        BlendVariety("Excelsa", excelsa, MediumRoastBrown),
        BlendVariety("Liberica", liberica, CremaAmberDark)
    ).filter { it.percentage > 0 }

    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            items.forEach { variety ->
                val weight = variety.percentage.toFloat() / total.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(variety.color)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { variety ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(variety.color)
                    )
                    Text(
                        text = "${variety.name} ${variety.percentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private data class BlendVariety(
    val name: String,
    val percentage: Int,
    val color: Color
)



/**
 * SCA Flavor Wheel Color Coded Chip for Flavor Notes.
 */
@Composable
fun FlavorTagChip(
    tag: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val tagLower = tag.lowercase(Locale.ROOT)
    val (bgColor, textColor) = when {
        tagLower.contains("fruch") || tagLower.contains("beere") || tagLower.contains("fruit") || tagLower.contains("berry") ->
            Color(0xFFF8BBD0) to Color(0xFF880E4F) // Berry Pink
        tagLower.contains("schoko") || tagLower.contains("kakao") || tagLower.contains("choc") || tagLower.contains("cocoa") ->
            Color(0xFFD7CCC8) to Color(0xFF3E2723) // Cocoa Brown
        tagLower.contains("nuss") || tagLower.contains("hazel") || tagLower.contains("nut") || tagLower.contains("almond") ->
            Color(0xFFEFEBE9) to Color(0xFF4E342E) // Chestnut Brown
        tagLower.contains("zitr") || tagLower.contains("citrus") || tagLower.contains("lemon") || tagLower.contains("lime") ->
            Color(0xFFFFF59D) to Color(0xFFF57F17) // Citrus Yellow
        tagLower.contains("flor") || tagLower.contains("blum") || tagLower.contains("jasmine") || tagLower.contains("rose") ->
            Color(0xFFE1BEE7) to Color(0xFF4A148C) // Lavender Pink
        tagLower.contains("süß") || tagLower.contains("sweet") || tagLower.contains("karam") || tagLower.contains("caramel") || tagLower.contains("honig") ->
            Color(0xFFFFE082) to Color(0xFFE65100) // Golden Caramel
        tagLower.contains("würz") || tagLower.contains("spice") || tagLower.contains("zimt") || tagLower.contains("cinnamon") ->
            Color(0xFFFFCCBC) to Color(0xFFBF360C) // Cinnamon Spice
        else ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) textColor else bgColor,
        border = BorderStroke(
            1.dp,
            if (isSelected) textColor else textColor.copy(alpha = 0.3f)
        ),
        modifier = modifier
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) bgColor else textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/**
 * Generic Reusable Number Stepper component with configurable step size.
 */
@Composable
fun NumberStepper(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    stepSize: Float = 0.5f,
    suffix: String = "",
    label: String = ""
) {
    val currentVal = value.toFloatOrNull() ?: 0f

    val minusInteractionSource = remember { MutableInteractionSource() }
    val minusIsPressed by minusInteractionSource.collectIsPressedAsState()
    val minusScale by animateFloatAsState(
        targetValue = if (minusIsPressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "minusBtnScale"
    )

    val plusInteractionSource = remember { MutableInteractionSource() }
    val plusIsPressed by plusInteractionSource.collectIsPressedAsState()
    val plusScale by animateFloatAsState(
        targetValue = if (plusIsPressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "plusBtnScale"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedIconButton(
            onClick = {
                val newVal = (currentVal - stepSize).coerceAtLeast(0f)
                val formatted = if (stepSize == 1f) String.format(Locale.ROOT, "%.0f", newVal) else String.format(Locale.ROOT, "%.1f", newVal)
                onValueChange(formatted)
            },
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = minusScale
                    scaleY = minusScale
                },
            shape = RoundedCornerShape(12.dp),
            interactionSource = minusInteractionSource
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease $stepSize")
        }

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            label = if (label.isNotEmpty()) { { Text(label) } } else null,
            suffix = if (suffix.isNotEmpty()) { { Text(suffix) } } else null,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.weight(1f)
        )

        OutlinedIconButton(
            onClick = {
                val newVal = currentVal + stepSize
                val formatted = if (stepSize == 1f) String.format(Locale.ROOT, "%.0f", newVal) else String.format(Locale.ROOT, "%.1f", newVal)
                onValueChange(formatted)
            },
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = plusScale
                    scaleY = plusScale
                },
            shape = RoundedCornerShape(12.dp),
            interactionSource = plusInteractionSource
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase $stepSize")
        }
    }
}

@Composable
fun GramsStepper(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    stepSize: Float = 0.5f,
    label: String = "Gramm"
) {
    NumberStepper(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        stepSize = stepSize,
        suffix = "g",
        label = label
    )
}

@Composable
fun GrindSizeStepper(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    stepSize: Float = 0.5f,
    label: String = "Mahlgrad"
) {
    NumberStepper(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        stepSize = stepSize,
        suffix = "",
        label = label
    )
}

package com.example.brewlog.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.brewlog.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CoffeeMaker
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brewlog.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Crema Amber Radiant Outer Glow modifier.
 * Draws a soft, outward-blooming ambient glow around rounded surfaces.
 */
fun Modifier.cremaGlow(
    color: Color = CremaAmber,
    borderRadius: Dp = 24.dp,
    glowRadius: Dp = 10.dp,
    alpha: Float = 0.45f
): Modifier = this.drawBehind {
    val cornerRadiusPx = borderRadius.toPx()
    val glowRadiusPx = glowRadius.toPx()
    val strokeWidthPx = 2.dp.toPx()

    for (i in 1..6) {
        val offset = (glowRadiusPx * (i / 6f)) + (strokeWidthPx / 2f)
        val layerAlpha = (alpha * (1f - (i - 1) / 6f) * 0.45f).coerceIn(0f, 1f)

        val glowPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        left = -offset,
                        top = -offset,
                        right = size.width + offset,
                        bottom = size.height + offset
                    ),
                    cornerRadius = CornerRadius((cornerRadiusPx + offset).coerceAtLeast(0f))
                )
            )
        }
        drawPath(
            path = glowPath,
            color = color.copy(alpha = layerAlpha),
            style = Stroke(width = strokeWidthPx)
        )
    }
}

/**
 * Reusable Crema Glass & Vellum Translucent Card Surface.
 * Automatically adapts between Dark Mode ("Espresso Glass") and Light Mode ("Oat Milk Vellum").
 */
@Composable
fun CremaGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isAppInDarkTheme()
    val bgColor = if (isDark) EspressoGlassBg else VellumGlassBg
    val borderColor = if (isDark) EspressoGlassBorder else VellumGlassBorder
    val glowColor = if (isDark) CremaAmber else CremaAmberDark

    val baseModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .cremaGlow(color = glowColor.copy(alpha = 0.35f), borderRadius = 24.dp, glowRadius = 6.dp, alpha = 0.25f)
    } else {
        modifier
            .fillMaxWidth()
            .cremaGlow(color = glowColor.copy(alpha = 0.35f), borderRadius = 24.dp, glowRadius = 6.dp, alpha = 0.25f)
    }

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = shape,
        color = bgColor,
        border = BorderStroke(1.2.dp, borderColor),
        shadowElevation = 6.dp,
        modifier = baseModifier
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Specialty Coffee Degassing / Freshness Window Badge.
 * Calculates days since roast date and displays optimal flavor window.
 */
@Composable
fun FreshnessBadge(
    roastDateString: String,
    modifier: Modifier = Modifier,
    onQuickEditDate: (() -> Unit)? = null
) {
    val daysOld = remember(roastDateString) {
        try {
            if (roastDateString.isBlank()) return@remember null
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val roastDate = LocalDate.parse(roastDateString.trim(), formatter)
            ChronoUnit.DAYS.between(roastDate, LocalDate.now())
        } catch (_: Exception) {
            null
        }
    }

    val (badgeBg, badgeText, label) = when {
        daysOld == null -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.no_roast_date)
        )
        daysOld < 0 -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.planned_roast)
        )
        daysOld in 0..6 -> Triple(
            Color(0x33FFB74D),
            Color(0xFFFFB74D),
            stringResource(R.string.degassing, daysOld)
        )
        daysOld in 7..30 -> Triple(
            Color(0x3381C784),
            Color(0xFF81C784),
            stringResource(R.string.peak_flavor, daysOld)
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.aged, daysOld)
        )
    }

    Surface(
        onClick = { onQuickEditDate?.invoke() },
        enabled = onQuickEditDate != null,
        shape = RoundedCornerShape(12.dp),
        color = badgeBg,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeText)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = badgeText,
                fontWeight = FontWeight.Bold
            )
            if (onQuickEditDate != null) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.quick_edit_roast_date),
                    tint = badgeText,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

/**
 * Barista Metric Badge for displaying Dose, Yield, Ratio, or Time in tabular monospace.
 */
@Composable
fun BaristaMetricBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (unit.isNotEmpty()) "$value$unit" else value,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = BaristaMonospaceFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Floating Glass Navigation Bar.
 * Modern floating bottom bar with glassmorphism, blur effect, and Crema glow indicator.
 */
@Composable
fun FloatingGlassNavigationBar(
    currentDestination: String?,
    onNavigate: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val navBg = if (isDark) Color(0xF218120F) else Color(0xF8F0EAE0)
    val navBorder = if (isDark) CremaAmber.copy(alpha = 0.55f) else CremaAmberDark.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = navBg,
            border = BorderStroke(1.2.dp, navBorder),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavGlassItem(
                    selected = currentDestination == "home",
                    onClick = { onNavigate("home") },
                    icon = { CoffeeBeanIcon(modifier = Modifier.size(20.dp)) },
                    label = "Bohnen"
                )
                NavGlassItem(
                    selected = currentDestination == "dial_in",
                    onClick = { onNavigate("dial_in") },
                    icon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = "Dial-In"
                )
                NavGlassItem(
                    selected = currentDestination == "shot_history",
                    onClick = { onNavigate("shot_history") },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = "Verlauf"
                )
                NavGlassItem(
                    selected = currentDestination == "espresso_machine",
                    onClick = { onNavigate("espresso_machine") },
                    icon = { Icon(Icons.Default.CoffeeMaker, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = "Maschine"
                )
                NavGlassItem(
                    selected = currentDestination == "settings",
                    onClick = onNavigateToSettings,
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), modifier = Modifier.size(20.dp)) },
                    label = "Optionen"
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavGlassItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String
) {
    val isDark = isAppInDarkTheme()

    val selectedColor = if (isDark) CremaAmber else DarkRoastBrown
    val unselectedColor = if (isDark) Color(0xFFE8DFC8) else Color(0xFF5C524A)
    val contentColor = if (selected) selectedColor else unselectedColor

    val alphaAnim by animateFloatAsState(
        targetValue = if (selected) 1f else 0.85f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "navItemAlpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navItemScale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
                alpha = alphaAnim
            }
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    icon()
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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
    val activeBeans = if (maxRating == 10) (rating + 1) / 2 else rating
    val cdText = stringResource(R.string.cd_rating, rating, maxRating)

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = cdText
        },
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
 * Visual Brew Ratio Indicator Bar displaying dose in, yield out, calculated ratio,
 * a visual ratio spectrum bar, and flow rate meter (g/s).
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

    val (ratioCategory, ratioColor) = when {
        ratio <= 0f -> Pair("-", MaterialTheme.colorScheme.onSurfaceVariant)
        ratio < 1.5f -> Pair("Ristretto", Color(0xFFFFB74D))
        ratio in 1.5f..2.5f -> Pair("Espresso (Sweet Spot)", Color(0xFF81C784))
        else -> Pair("Lungo", Color(0xFF64B5F6))
    }

    val flowRate = if (extractionTimeSec > 0f) yieldInGrams / extractionTimeSec else 0f
    val (flowStatus, flowColor) = when {
        flowRate <= 0f -> Pair("-", MaterialTheme.colorScheme.onSurfaceVariant)
        flowRate < 1.0f -> Pair("Langsam / Choked", Color(0xFFFFB74D))
        flowRate in 1.0f..1.9f -> Pair("Ideal", Color(0xFF81C784))
        else -> Pair("Schnell / Channeling", Color(0xFFE53935))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: IN / RATIO / OUT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "IN",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.ROOT, "%.1f", doseInGrams)}g",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = BaristaMonospaceFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "RATIO 1:$ratioFormatted",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = BaristaMonospaceFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = ratioCategory,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ratioColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "OUT",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.ROOT, "%.1f", yieldInGrams)}g",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = BaristaMonospaceFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Visual Ratio Spectrum Bar
            val normalizedRatioProgress = ((ratio - 1.0f) / 2.0f).coerceIn(0f, 1f)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val totalWidth = maxWidth
                val markerX = totalWidth * normalizedRatioProgress

                // Background Spectrum
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(Color(0xFFFFB74D).copy(alpha = 0.35f)))
                    Box(modifier = Modifier.weight(0.50f).fillMaxHeight().background(Color(0xFF81C784).copy(alpha = 0.45f)))
                    Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(Color(0xFF64B5F6).copy(alpha = 0.35f)))
                }
                // Marker Pin
                if (ratio > 0f) {
                    Box(
                        modifier = Modifier
                            .offset(x = markerX - 3.dp)
                            .width(6.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Flow Rate Indicator (if time available)
            if (extractionTimeSec > 0f) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${extractionTimeSec.toInt()}s",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = BaristaMonospaceFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "FLUSS:",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = flowColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, flowColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${String.format(Locale.ROOT, "%.1f", flowRate)} g/s • $flowStatus",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = BaristaMonospaceFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = flowColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
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

        val handlePath = Path().apply {
            moveTo(width * 0.78f, height * 0.35f)
            cubicTo(
                width * 0.95f, height * 0.35f,
                width * 0.95f, height * 0.6f,
                width * 0.76f, height * 0.6f
            )
        }

        val saucerPath = Path().apply {
            moveTo(width * 0.15f, height * 0.88f)
            lineTo(width * 0.85f, height * 0.88f)
        }

        val steam1 = Path().apply {
            moveTo(width * 0.4f, height * 0.18f)
            cubicTo(width * 0.35f, height * 0.12f, width * 0.45f, height * 0.08f, width * 0.4f, 0f)
        }

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
        fontFamily = BaristaMonospaceFontFamily,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )

    val cdText = stringResource(
        R.string.cd_sensory_profile,
        sweetness.coerceIn(1, 5),
        acidity.coerceIn(1, 5),
        body.coerceIn(1, 5),
        bitterness.coerceIn(1, 5)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = cdText
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = minOf(cx, cy) * 0.65f

            val angles = listOf(-PI / 2, 0.0, PI / 2, PI)
            val values = listOf(sweetVal, acidVal, bodyVal, bitterVal)
            val labels = listOf(sweetnessLabel, acidityLabel, bodyLabel, bitternessLabel)

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

            angles.forEachIndexed { index, _ ->
                val labelLayout = textMeasurer.measure(labels[index], labelStyle)
                val w = labelLayout.size.width.toFloat()
                val h = labelLayout.size.height.toFloat()

                val x: Float
                val y: Float

                when (index) {
                    0 -> {
                        x = cx - w / 2f
                        y = cy - radius - h - 4.dp.toPx()
                    }
                    1 -> {
                        x = cx + radius + 6.dp.toPx()
                        y = cy - h / 2f
                    }
                    2 -> {
                        x = cx - w / 2f
                        y = cy + radius + 4.dp.toPx()
                    }
                    else -> {
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
            Color(0xFFF8BBD0) to Color(0xFF880E4F)
        tagLower.contains("schoko") || tagLower.contains("kakao") || tagLower.contains("choc") || tagLower.contains("cocoa") ->
            Color(0xFFD7CCC8) to Color(0xFF3E2723)
        tagLower.contains("nuss") || tagLower.contains("hazel") || tagLower.contains("nut") || tagLower.contains("almond") ->
            Color(0xFFEFEBE9) to Color(0xFF4E342E)
        tagLower.contains("zitr") || tagLower.contains("citrus") || tagLower.contains("lemon") || tagLower.contains("lime") ->
            Color(0xFFFFF59D) to Color(0xFFF57F17)
        tagLower.contains("flor") || tagLower.contains("blum") || tagLower.contains("jasmine") || tagLower.contains("rose") ->
            Color(0xFFE1BEE7) to Color(0xFF4A148C)
        tagLower.contains("süß") || tagLower.contains("sweet") || tagLower.contains("karam") || tagLower.contains("caramel") || tagLower.contains("honig") ->
            Color(0xFFFFE082) to Color(0xFFE65100)
        tagLower.contains("würz") || tagLower.contains("spice") || tagLower.contains("zimt") || tagLower.contains("cinnamon") ->
            Color(0xFFFFCCBC) to Color(0xFFBF360C)
        else ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) textColor else bgColor.copy(alpha = 0.85f),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.4f)
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = bgColor
                )
            }
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) bgColor else textColor,
                maxLines = 1,
                softWrap = false
            )
        }
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

    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.cd_decrease, label.ifEmpty { stringResource(R.string.dose_metric) })
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            label = if (label.isNotEmpty()) { { Text(label) } } else null,
            suffix = if (suffix.isNotEmpty()) { { Text(suffix) } } else null,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            textStyle = TextStyle(
                fontFamily = BaristaMonospaceFontFamily,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.weight(1f)
        )

        OutlinedIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_increase, label.ifEmpty { stringResource(R.string.dose_metric) })
            )
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
                        fontFamily = BaristaMonospaceFontFamily,
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
 * Premium AI Feature Hero Button with glowing radiant aura and AI Feature badge.
 * Designed to make unique AI capabilities (such as label scanning and machine auto-detection)
 * stand out as key features.
 */
@Composable
fun AiFeatureButton(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.AutoAwesome,
    isLoading: Boolean = false,
    loadingText: String? = null,
    enabled: Boolean = true
) {
    val isDark = isAppInDarkTheme()
    val haptic = LocalHapticFeedback.current

    val gradientBrush = if (isDark) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF281C28),
                Color(0xFF5E2750),
                Color(0xFF8E44AD),
                Color(0xFFC88A58)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFE2A86B),
                Color(0xFFBA68C8),
                Color(0xFF8E44AD),
                Color(0xFFC88A58)
            )
        )
    }

    val glowColor = if (isDark) Color(0xFFBA68C8) else Color(0xFFE2A86B)

    Surface(
        onClick = {
            if (!isLoading && enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, glowColor.copy(alpha = 0.7f)),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .cremaGlow(color = glowColor, borderRadius = 20.dp, glowRadius = 8.dp, alpha = 0.4f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = gradientBrush)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top Icon & Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Box(modifier = Modifier.padding(6.dp)) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = loadingText ?: stringResource(R.string.scanning_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                if (!isLoading && !subtitle.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Calendar DatePicker Dialog for selecting roast date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeDatePickerDialog(
    initialDateString: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialDateString) {
        try {
            if (initialDateString.isNotEmpty()) {
                val localDate = LocalDate.parse(initialDateString)
                localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            } else {
                System.currentTimeMillis()
            }
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Allow today or past dates up to today
                return utcTimeMillis <= System.currentTimeMillis() + 86400000L
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onDateSelected(date.toString())
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = stringResource(R.string.roast_date),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        )
    }
}

/**
 * Interactive Roast Date Selection Field with a Calendar Icon and Quick Date Presets.
 * Tapping the field or calendar icon opens a Material 3 Calendar DatePickerDialog.
 */
@Composable
fun RoastDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.roast_date)
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedDisplayDate = remember(value) {
        try {
            if (value.isNotEmpty()) {
                val parsed = LocalDate.parse(value)
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                parsed.format(formatter)
            } else ""
        } catch (_: Exception) {
            value
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = formattedDisplayDate,
            onValueChange = {},
            readOnly = true,
            enabled = true,
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.roast_date_placeholder)) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.cd_calendar),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay to open datepicker when clicking anywhere on the textfield
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .clickable { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        CoffeeDatePickerDialog(
            initialDateString = value,
            onDateSelected = { selectedDate -> onValueChange(selectedDate) },
            onDismiss = { showDatePicker = false }
        )
    }
}




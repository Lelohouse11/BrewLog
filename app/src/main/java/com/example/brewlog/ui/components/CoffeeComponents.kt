package com.example.brewlog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.brewlog.ui.theme.*
import java.util.Locale

/**
 * Minimalist Coffee Bean vector icon drawn programmatically via Canvas.
 */
@Composable
fun CoffeeBeanIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
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
            CoffeeBeanIcon(
                modifier = Modifier.size(beanSize),
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

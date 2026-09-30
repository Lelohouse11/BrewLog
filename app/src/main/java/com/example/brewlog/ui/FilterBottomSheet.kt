package com.example.brewlog.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brewlog.R
import com.example.brewlog.ui.components.*
import com.example.brewlog.ui.theme.*

private val ROAST_OPTIONS = listOf("Light", "Medium", "Dark")
private val FLAVOR_OPTIONS = listOf(
    "Chocolate", "Nutty", "Fruity", "Floral", "Caramel", 
    "Berry", "Citrus", "Spices", "Earthy", "Smoke"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    sortOption: SortOption,
    filterState: FilterState,
    onSortChange: (SortOption) -> Unit,
    onFilterChange: (FilterState) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    val activeFilterCount = (if (filterState.selectedRoasts.isNotEmpty()) 1 else 0) +
            (if (filterState.selectedFlavorTags.isNotEmpty()) 1 else 0) +
            (if (filterState.ratingRange != 1f..10f) 1 else 0) +
            (if (filterState.sweetnessRange != 1f..5f || filterState.acidityRange != 1f..5f || filterState.bodyRange != 1f..5f || filterState.bitternessRange != 1f..5f) 1 else 0) +
            (if (filterState.showArchived) 1 else 0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    stringResource(R.string.filters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (activeFilterCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "$activeFilterCount",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            TextButton(
                onClick = onReset,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.reset_all), style = MaterialTheme.typography.labelMedium)
            }
        }

        // Sorting Section
        CremaGlassFilterCard(title = stringResource(R.string.sort_by)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CremaFilterChip(
                    selected = sortOption == SortOption.NAME,
                    onClick = { onSortChange(SortOption.NAME) },
                    label = stringResource(R.string.sort_name)
                )
                CremaFilterChip(
                    selected = sortOption == SortOption.RATING,
                    onClick = { onSortChange(SortOption.RATING) },
                    label = stringResource(R.string.sort_rating)
                )
                CremaFilterChip(
                    selected = sortOption == SortOption.ROASTERY,
                    onClick = { onSortChange(SortOption.ROASTERY) },
                    label = stringResource(R.string.sort_roastery)
                )
            }
        }

        // Show Archived Beans Toggle
        CremaGlassFilterToggleCard(
            title = stringResource(R.string.show_archived_shots),
            checked = filterState.showArchived,
            onCheckedChange = { onFilterChange(filterState.copy(showArchived = it)) }
        )

        // Rating Range
        CremaGlassFilterCard(title = stringResource(R.string.rating_range)) {
            FilterRangeSlider(
                label = stringResource(R.string.rating),
                value = filterState.ratingRange,
                valueRange = 1f..10f,
                steps = 8,
                onValueChange = { onFilterChange(filterState.copy(ratingRange = it)) }
            )
        }

        // Sensory Profile
        CremaGlassFilterCard(title = stringResource(R.string.sensory_profile)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterRangeSlider(stringResource(R.string.sweetness), filterState.sweetnessRange, 1f..5f, 3) { onFilterChange(filterState.copy(sweetnessRange = it)) }
                FilterRangeSlider(stringResource(R.string.acidity), filterState.acidityRange, 1f..5f, 3) { onFilterChange(filterState.copy(acidityRange = it)) }
                FilterRangeSlider(stringResource(R.string.body), filterState.bodyRange, 1f..5f, 3) { onFilterChange(filterState.copy(bodyRange = it)) }
                FilterRangeSlider(stringResource(R.string.bitterness), filterState.bitternessRange, 1f..5f, 3) { onFilterChange(filterState.copy(bitternessRange = it)) }
            }
        }

        // Roast Level
        CremaGlassFilterCard(title = stringResource(R.string.roast_level)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ROAST_OPTIONS.forEach { roast ->
                    CremaFilterChip(
                        selected = filterState.selectedRoasts.contains(roast),
                        onClick = {
                            val newSet = if (filterState.selectedRoasts.contains(roast)) {
                                filterState.selectedRoasts - roast
                            } else {
                                filterState.selectedRoasts + roast
                            }
                            onFilterChange(filterState.copy(selectedRoasts = newSet))
                        },
                        label = roast
                    )
                }
            }
        }

        // Flavor Tags
        CremaGlassFilterCard(title = stringResource(R.string.flavor_tags)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FLAVOR_OPTIONS.forEach { tag ->
                    FlavorTagChip(
                        tag = tag,
                        isSelected = filterState.selectedFlavorTags.contains(tag),
                        onClick = {
                            val newSet = if (filterState.selectedFlavorTags.contains(tag)) {
                                filterState.selectedFlavorTags - tag
                            } else {
                                filterState.selectedFlavorTags + tag
                            }
                            onFilterChange(filterState.copy(selectedFlavorTags = newSet))
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.apply_filters), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CremaGlassFilterCard(
    title: String,
    content: @Composable () -> Unit
) {
    CremaGlassCard(
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleSmall, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun CremaGlassFilterToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    CremaGlassCard(
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun CremaFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun FilterRangeSlider(
    label: String,
    value: ClosedFloatingPointRange<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "${value.start.toInt()} - ${value.endInclusive.toInt()}", 
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = BaristaMonospaceFontFamily,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        RangeSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(top = 2.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

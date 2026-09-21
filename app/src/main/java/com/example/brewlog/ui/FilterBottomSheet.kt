package com.example.brewlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.brewlog.R

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.filters), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            TextButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.reset_all))
            }
        }

        // Sorting Section
        FilterSection(title = stringResource(R.string.sort_by)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sortOption == SortOption.NAME,
                    onClick = { onSortChange(SortOption.NAME) },
                    label = { Text(stringResource(R.string.sort_name)) },
                    leadingIcon = if (sortOption == SortOption.NAME) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                )
                FilterChip(
                    selected = sortOption == SortOption.RATING,
                    onClick = { onSortChange(SortOption.RATING) },
                    label = { Text(stringResource(R.string.sort_rating)) },
                    leadingIcon = if (sortOption == SortOption.RATING) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                )
            }
        }

        // Rating Range
        FilterSection(title = stringResource(R.string.rating_range)) {
            FilterRangeSlider(
                label = stringResource(R.string.rating),
                value = filterState.ratingRange,
                valueRange = 1f..10f,
                steps = 8,
                onValueChange = { onFilterChange(filterState.copy(ratingRange = it)) }
            )
        }

        // Sensory Profile
        FilterSection(title = stringResource(R.string.sensory_profile)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FilterRangeSlider(stringResource(R.string.sweetness), filterState.sweetnessRange, 1f..5f, 3) { onFilterChange(filterState.copy(sweetnessRange = it)) }
                FilterRangeSlider(stringResource(R.string.acidity), filterState.acidityRange, 1f..5f, 3) { onFilterChange(filterState.copy(acidityRange = it)) }
                FilterRangeSlider(stringResource(R.string.body), filterState.bodyRange, 1f..5f, 3) { onFilterChange(filterState.copy(bodyRange = it)) }
                FilterRangeSlider(stringResource(R.string.bitterness), filterState.bitternessRange, 1f..5f, 3) { onFilterChange(filterState.copy(bitternessRange = it)) }
            }
        }

        // Roast Level
        FilterSection(title = stringResource(R.string.roast_level)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ROAST_OPTIONS.forEach { roast ->
                    FilterChip(
                        selected = filterState.selectedRoasts.contains(roast),
                        onClick = {
                            val newSet = if (filterState.selectedRoasts.contains(roast)) {
                                filterState.selectedRoasts - roast
                            } else {
                                filterState.selectedRoasts + roast
                            }
                            onFilterChange(filterState.copy(selectedRoasts = newSet))
                        },
                        label = { Text(roast) }
                    )
                }
            }
        }

        // Flavor Tags
        FilterSection(title = stringResource(R.string.flavor_tags)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FLAVOR_OPTIONS.forEach { tag ->
                    FilterChip(
                        selected = filterState.selectedFlavorTags.contains(tag),
                        onClick = {
                            val newSet = if (filterState.selectedFlavorTags.contains(tag)) {
                                filterState.selectedFlavorTags - tag
                            } else {
                                filterState.selectedFlavorTags + tag
                            }
                            onFilterChange(filterState.copy(selectedFlavorTags = newSet))
                        },
                        label = { Text(tag) }
                    )
                }
            }
        }
        
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.apply_filters), fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title, 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = "${value.start.toInt()} - ${value.endInclusive.toInt()}", 
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        RangeSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

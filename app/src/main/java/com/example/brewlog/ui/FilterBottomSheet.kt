package com.example.brewlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort & Filter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Filters")
            }
        }

        // Sorting
        Column {
            Text("Sort By", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sortOption == SortOption.NAME,
                    onClick = { onSortChange(SortOption.NAME) },
                    label = { Text("Name") }
                )
                FilterChip(
                    selected = sortOption == SortOption.RATING,
                    onClick = { onSortChange(SortOption.RATING) },
                    label = { Text("Rating") }
                )
            }
        }

        // Rating Range
        FilterRangeSlider(
            label = "Rating",
            value = filterState.ratingRange,
            valueRange = 1f..10f,
            steps = 8,
            onValueChange = { onFilterChange(filterState.copy(ratingRange = it)) }
        )

        // Sensory Profile Ranges
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Sensory Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            
            FilterRangeSlider("Sweetness", filterState.sweetnessRange, 1f..5f, 3) { onFilterChange(filterState.copy(sweetnessRange = it)) }
            FilterRangeSlider("Acidity", filterState.acidityRange, 1f..5f, 3) { onFilterChange(filterState.copy(acidityRange = it)) }
            FilterRangeSlider("Body", filterState.bodyRange, 1f..5f, 3) { onFilterChange(filterState.copy(bodyRange = it)) }
            FilterRangeSlider("Bitterness", filterState.bitternessRange, 1f..5f, 3) { onFilterChange(filterState.copy(bitternessRange = it)) }
        }

        // Roast Level
        Column {
            Text("Roast Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Column {
            Text("Flavor Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        
        Spacer(modifier = Modifier.height(32.dp))
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text("${value.start.toInt()} - ${value.endInclusive.toInt()}", style = MaterialTheme.typography.labelSmall)
        }
        RangeSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

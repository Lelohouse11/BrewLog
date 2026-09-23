package com.example.brewlog.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brewlog.BuildConfig
import com.example.brewlog.R
import com.example.brewlog.data.ThemeMode
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val gramsStepSize by viewModel.gramsStepSize.collectAsState()
    val grindStepSize by viewModel.grindStepSize.collectAsState()
    val context = LocalContext.current
    
    val showThemeDialog = remember { mutableStateOf(false) }
    val showLanguageDialog = remember { mutableStateOf(false) }
    val showGramsStepDialog = remember { mutableStateOf(false) }
    val showGrindStepDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                SettingsSectionHeader(stringResource(R.string.appearance))
            }

            item {
                val themeLabel = when (themeMode) {
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                }
                SettingsItem(
                    title = stringResource(R.string.theme),
                    subtitle = themeLabel,
                    icon = Icons.Default.Palette,
                    onClick = { showThemeDialog.value = true }
                )
            }

            item {
                val currentLang = viewModel.getCurrentLanguageTag()
                val langLabel = when {
                    currentLang.isEmpty() -> stringResource(R.string.language_system)
                    currentLang.startsWith("de") -> stringResource(R.string.language_de)
                    currentLang.startsWith("en") -> stringResource(R.string.language_en)
                    else -> stringResource(R.string.language_system)
                }
                SettingsItem(
                    title = stringResource(R.string.language),
                    subtitle = langLabel,
                    icon = Icons.Default.Language,
                    onClick = { showLanguageDialog.value = true }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                SettingsSectionHeader("Barista Einstellungen")
            }

            item {
                SettingsItem(
                    title = "Gramm Schrittweite",
                    subtitle = "${String.format(Locale.ROOT, "%.1f", gramsStepSize)} g",
                    icon = Icons.Default.Scale,
                    onClick = { showGramsStepDialog.value = true }
                )
            }

            item {
                SettingsItem(
                    title = "Mahlgrad Schrittweite",
                    subtitle = String.format(Locale.ROOT, "%.1f", grindStepSize),
                    icon = Icons.Default.Tune,
                    onClick = { showGrindStepDialog.value = true }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                SettingsSectionHeader(stringResource(R.string.about))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.version),
                    subtitle = "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    icon = Icons.Default.Info,
                    onClick = {} // Not interactive as requested
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.github_repo),
                    subtitle = "https://github.com/Lelohouse11/BrewLog",
                    icon = Icons.AutoMirrored.Filled.Launch,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Lelohouse11/BrewLog".toUri())
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            // Handle securely
                        }
                    }
                )
            }
        }
    }

    if (showThemeDialog.value) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            onDismiss = { showThemeDialog.value = false },
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog.value = false
            }
        )
    }

    if (showLanguageDialog.value) {
        LanguageSelectionDialog(
            currentTag = viewModel.getCurrentLanguageTag(),
            onDismiss = { showLanguageDialog.value = false },
            onSelect = {
                viewModel.setLanguage(it)
                showLanguageDialog.value = false
            }
        )
    }

    if (showGramsStepDialog.value) {
        StepSizeSelectionDialog(
            title = "Gramm Schrittweite wählen",
            currentValue = gramsStepSize,
            unit = "g",
            onDismiss = { showGramsStepDialog.value = false },
            onSelect = {
                viewModel.setGramsStepSize(it)
                showGramsStepDialog.value = false
            }
        )
    }

    if (showGrindStepDialog.value) {
        StepSizeSelectionDialog(
            title = "Mahlgrad Schrittweite wählen",
            currentValue = grindStepSize,
            unit = "",
            onDismiss = { showGrindStepDialog.value = false },
            onSelect = {
                viewModel.setGrindStepSize(it)
                showGrindStepDialog.value = false
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_theme)) },
        text = {
            Column {
                ThemeOption(stringResource(R.string.theme_system), ThemeMode.SYSTEM, currentMode, onSelect)
                ThemeOption(stringResource(R.string.theme_light), ThemeMode.LIGHT, currentMode, onSelect)
                ThemeOption(stringResource(R.string.theme_dark), ThemeMode.DARK, currentMode, onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ThemeOption(
    label: String,
    mode: ThemeMode,
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = mode == currentMode, onClick = { onSelect(mode) })
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun LanguageSelectionDialog(
    currentTag: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column {
                LanguageOption(stringResource(R.string.language_system), "", currentTag, onSelect)
                LanguageOption(stringResource(R.string.language_de), "de", currentTag, onSelect)
                LanguageOption(stringResource(R.string.language_en), "en", currentTag, onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun LanguageOption(
    label: String,
    tag: String,
    currentTag: String,
    onSelect: (String) -> Unit
) {
    val selected = if (tag.isEmpty()) currentTag.isEmpty() else currentTag.startsWith(tag)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(tag) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = { onSelect(tag) })
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun StepSizeSelectionDialog(
    title: String,
    currentValue: Float,
    unit: String = "",
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit
) {
    val options = listOf(0.1f, 0.5f, 1.0f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(step) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = abs(currentValue - step) < 0.01f, onClick = { onSelect(step) })
                        Text(
                            text = "${String.format(Locale.ROOT, "%.1f", step)} $unit".trim(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

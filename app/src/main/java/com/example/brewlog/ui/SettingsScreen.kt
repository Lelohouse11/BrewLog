package com.example.brewlog.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.brewlog.ui.components.CremaGlassCard
import com.example.brewlog.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    brewViewModel: BrewViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val gramsStepSize by viewModel.gramsStepSize.collectAsState()
    val grindStepSize by viewModel.grindStepSize.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val showThemeDialog = remember { mutableStateOf(false) }
    val showLanguageDialog = remember { mutableStateOf(false) }
    val showGramsStepDialog = remember { mutableStateOf(false) }
    val showGrindStepDialog = remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { brewViewModel.importData(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = brewViewModel.exportData()
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
            }
        }
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SettingsSectionHeader(stringResource(R.string.appearance))
                    Spacer(Modifier.height(8.dp))
                    CremaGlassCard(contentPadding = PaddingValues(0.dp)) {
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

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

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
                }

                item {
                    SettingsSectionHeader(stringResource(R.string.barista_settings))
                    Spacer(Modifier.height(8.dp))
                    CremaGlassCard(contentPadding = PaddingValues(0.dp)) {
                        SettingsItem(
                            title = stringResource(R.string.grams_step_size),
                            subtitle = "${String.format(Locale.ROOT, "%.1f", gramsStepSize)} g",
                            icon = Icons.Default.Scale,
                            onClick = { showGramsStepDialog.value = true }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsItem(
                            title = stringResource(R.string.grind_step_size),
                            subtitle = String.format(Locale.ROOT, "%.1f", grindStepSize),
                            icon = Icons.Default.Tune,
                            onClick = { showGrindStepDialog.value = true }
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(R.string.data_backup))
                    Spacer(Modifier.height(8.dp))
                    CremaGlassCard(contentPadding = PaddingValues(0.dp)) {
                        SettingsItem(
                            title = stringResource(R.string.import_json),
                            subtitle = stringResource(R.string.import_json_desc),
                            icon = Icons.Default.FileDownload,
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                            }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsItem(
                            title = stringResource(R.string.export_json),
                            subtitle = stringResource(R.string.export_json_desc),
                            icon = Icons.Default.FileUpload,
                            onClick = {
                                exportLauncher.launch("brew_settings.json")
                            }
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(R.string.about))
                    Spacer(Modifier.height(8.dp))
                    CremaGlassCard(contentPadding = PaddingValues(0.dp)) {
                        SettingsItem(
                            title = stringResource(R.string.version),
                            subtitle = "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            icon = Icons.Default.Info,
                            onClick = {}
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsItem(
                            title = stringResource(R.string.github_repo),
                            subtitle = "https://github.com/Lelohouse11/BrewLog",
                            icon = Icons.AutoMirrored.Filled.Launch,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Lelohouse11/BrewLog".toUri())
                                try {
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
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
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
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
            .clickable(onClick = onClick),
        color = Color.Transparent
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
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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
    val isDark = isAppInDarkTheme()
    val dialogBg = if (isDark) EspressoGlassBg else VellumGlassBg

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = { Text(stringResource(R.string.select_theme), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
    val isDark = isAppInDarkTheme()
    val dialogBg = if (isDark) EspressoGlassBg else VellumGlassBg

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = { Text(stringResource(R.string.select_language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
    val isDark = isAppInDarkTheme()
    val dialogBg = if (isDark) EspressoGlassBg else VellumGlassBg

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
                            fontFamily = BaristaMonospaceFontFamily,
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

package com.example.gpnflix.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gpnflix.data.model.ImageQualityMode
import com.example.gpnflix.data.model.SpaceCompressionPreset
import com.example.gpnflix.ui.viewmodel.GpnFlixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: GpnFlixViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val tmdbTestState by viewModel.tmdbTestState.collectAsStateWithLifecycle()
    val regionLocale by viewModel.regionLocale.collectAsStateWithLifecycle()

    var showTmdbKeyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCompressionDialog by remember { mutableStateOf(false) }
    var showImageQualityDialog by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var cacheBytes by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        cacheBytes = withContext(Dispatchers.IO) {
            calculateDirBytes(context.cacheDir)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                    Text(
                        text = "Paramètres",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Tableau Configuration API TMDB v4 / v3
            Text(
                text = "Configuration API TMDB (v4 / v3)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tmdb_config_table_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (userSettings.isTmdbActive) {
                        Color(0xFF22C55E).copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (userSettings.isTmdbActive) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = if (userSettings.isTmdbActive) Color(0xFF22C55E) else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tableau de connexion TMDB",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Structured 2-column configuration table
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            ConfigTableRow(
                                field = "Paramètre",
                                value = "Valeur / État",
                                isHeader = true
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ConfigTableRow(
                                field = "Statut API",
                                value = if (userSettings.isTmdbActive) "Connecté (Actif)" else "Inactif (Archive.org seul)"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ConfigTableRow(
                                field = "Source active",
                                value = when {
                                    userSettings.hasCustomTmdbKey -> "Saisie locale (Appareil)"
                                    userSettings.isTmdbActive -> "Secrets AI Studio (BuildConfig)"
                                    else -> "Aucune"
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ConfigTableRow(
                                field = "Mode Auth",
                                value = when {
                                    userSettings.isTmdbV4Token -> "Jeton v4 (Authorization Bearer)"
                                    userSettings.isTmdbActive -> "Clé API v3 (?api_key=...)"
                                    else -> "Jeton v4 (eyJ...) ou Clé v3"
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ConfigTableRow(
                                field = "Clé enregistrée",
                                value = if (userSettings.maskedTmdbKey.isNotBlank()) {
                                    userSettings.maskedTmdbKey
                                } else {
                                    "Aucune clé définie"
                                }
                            )
                        }
                    }

                    if (tmdbTestState.message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val statusColor = when (tmdbTestState.isSuccess) {
                            true -> Color(0xFF22C55E)
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.secondary
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (tmdbTestState.isTesting) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = statusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = tmdbTestState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showTmdbKeyDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_tmdb_key_dialog_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (userSettings.hasCustomTmdbKey) "Modifier clé v4" else "Introduire clé v4",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.testTmdbConnection() },
                            enabled = !tmdbTestState.isTesting,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("test_tmdb_connection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tester")
                        }

                        if (userSettings.hasCustomTmdbKey) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearCustomTmdbKey()
                                    Toast.makeText(context, "Clé personnalisée supprimée", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("clear_tmdb_key_button")
                            ) {
                                Text("Réinit.")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Apparence & Langue",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsItemRow(
                        title = "Thème d'affichage",
                        currentValue = when (userSettings.themeMode) {
                            "DARK" -> "Mode sombre (Recommandé)"
                            "LIGHT" -> "Mode clair"
                            else -> "Système"
                        },
                        icon = Icons.Filled.DarkMode,
                        onClick = { showThemeDialog = true }
                    )
                    SettingsItemRow(
                        title = "Langue & Région du catalogue",
                        currentValue = "${userSettings.language} (${regionLocale.tmdbLanguage})",
                        icon = Icons.Filled.Language,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Streaming & Qualité vidéo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsItemRow(
                        title = "Qualité de lecture par défaut",
                        currentValue = userSettings.defaultQuality,
                        icon = Icons.Filled.HighQuality,
                        onClick = { showQualityDialog = true }
                    )
                    SettingsItemRow(
                        title = "Compresseur d'espace (Séries & Films)",
                        currentValue = "${userSettings.compressionPreset.label} (${userSettings.compressionPreset.exampleText})",
                        icon = Icons.Filled.Compress,
                        onClick = { showCompressionDialog = true }
                    )
                    SettingsItemRow(
                        title = "Qualité & Formats d'images",
                        currentValue = userSettings.imageQualityMode.label,
                        icon = Icons.Filled.Image,
                        onClick = { showImageQualityDialog = true }
                    )
                    SettingsSwitchRow(
                        title = "Mode Famille / Dessins animés",
                        subtitle = "Filtre automatiquement l'accueil pour les enfants",
                        icon = Icons.Filled.ChildCare,
                        checked = userSettings.familyKidsMode,
                        onCheckedChange = { viewModel.toggleFamilyKidsMode(it) }
                    )
                    SettingsSwitchRow(
                        title = "Télécharger en Wi-Fi uniquement",
                        subtitle = "Économise vos données mobiles",
                        icon = Icons.Filled.Wifi,
                        checked = userSettings.downloadWifiOnly,
                        onCheckedChange = { viewModel.toggleWifiOnly(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Stockage local",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                SettingsItemRow(
                    title = "Vider le cache d'images & réseau",
                    currentValue = formatCacheBytes(cacheBytes),
                    icon = Icons.Filled.CleaningServices,
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                            }
                            cacheBytes = withContext(Dispatchers.IO) { calculateDirBytes(context.cacheDir) }
                            Toast.makeText(context, "Cache vidé avec succès", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Sources légales & Mentions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsItemRow(
                        title = "API & Licences légales utilisées",
                        currentValue = "Internet Archive • TMDB • Domaine Public",
                        icon = Icons.Filled.Security,
                        onClick = { showLegalDialog = true }
                    )
                    SettingsItemRow(
                        title = "Version de GpnFlix",
                        currentValue = "2.1.0 (Live Netflix-Style Edition)",
                        icon = Icons.Filled.Info,
                        onClick = {}
                    )
                }
            }
        }
    }

    if (showTmdbKeyDialog) {
        var apiKeyInput by remember { mutableStateOf("") }
        var showPlainKey by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTmdbKeyDialog = false },
            title = {
                Text(
                    text = "Introduire votre clé API TMDB",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Collez votre Jeton d'accès en lecture API v4 (commençant par eyJ...) ou votre clé API v3. Il est stocké localement sur l'appareil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            ConfigTableRow(field = "Type", value = "Authentification", isHeader = true)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ConfigTableRow(field = "Jeton v4", value = "Authorization: Bearer eyJ...")
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ConfigTableRow(field = "Clé v3", value = "?api_key=<32 caractères>")
                        }
                    }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Clé ou Jeton TMDB v4") },
                        placeholder = { Text("eyJhbGciOiJIUzI1NiJ9...") },
                        singleLine = true,
                        visualTransformation = if (showPlainKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPlainKey = !showPlainKey }) {
                                Icon(
                                    imageVector = if (showPlainKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPlainKey) "Masquer" else "Afficher"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tmdb_api_key_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = apiKeyInput.trim()
                        if (trimmed.length >= 16) {
                            viewModel.saveCustomTmdbKey(trimmed)
                            showTmdbKeyDialog = false
                            Toast.makeText(
                                context,
                                "Clé TMDB enregistrée. Actualisation du catalogue...",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Veuillez coller une clé ou un jeton TMDB valide.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.testTag("save_tmdb_key_button")
                ) {
                    Text("Enregistrer & Activer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTmdbKeyDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showThemeDialog) {
        val themeOptions = listOf("DARK" to "Mode sombre", "LIGHT" to "Mode clair", "SYSTEM" to "Suivre le système")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choisir le thème") },
            text = {
                Column {
                    themeOptions.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = userSettings.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showQualityDialog) {
        val qualityOptions = listOf("4K UHD", "Auto (1080p HD)", "1080p HD", "720p HD", "480p SD")
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Qualité de streaming par défaut") },
            text = {
                Column {
                    qualityOptions.forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDefaultQuality(q)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = userSettings.defaultQuality == q,
                                onClick = {
                                    viewModel.setDefaultQuality(q)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = q)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showCompressionDialog) {
        AlertDialog(
            onDismissRequest = { showCompressionDialog = false },
            title = { Text("Compresseur d'espace hors-ligne") },
            text = {
                Column {
                    SpaceCompressionPreset.entries.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCompressionPreset(preset)
                                    showCompressionDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = userSettings.compressionPreset == preset,
                                onClick = {
                                    viewModel.setCompressionPreset(preset)
                                    showCompressionDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = preset.label, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${preset.exampleText} • ${preset.targetQualityLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCompressionDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showImageQualityDialog) {
        AlertDialog(
            onDismissRequest = { showImageQualityDialog = false },
            title = { Text("Qualité des affiches & images") },
            text = {
                Column {
                    ImageQualityMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setImageQualityMode(mode)
                                    showImageQualityDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = userSettings.imageQualityMode == mode,
                                onClick = {
                                    viewModel.setImageQualityMode(mode)
                                    showImageQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageQualityDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showLanguageDialog) {
        val langOptions = listOf("Français uniquement", "Auto (Région)", "English", "Español")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Langue et région du catalogue") },
            text = {
                Column {
                    langOptions.forEach { l ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(l)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = userSettings.language == l,
                                onClick = {
                                    viewModel.setLanguage(l)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = l)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showLegalDialog) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            title = { Text("Sources Légales GpnFlix") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "GpnFlix s'appuie exclusivement sur des API publiques et licences autorisées :\n\n" +
                            "• Internet Archive (archive.org) : films complets du Domaine Public, classiques historiques, animations libres (Blender Foundation) et archives NASA en streaming MP4 légal.\n\n" +
                            "• The Movie Database (TMDB) : métadonnées officielles localisées (français ou langue de l'appareil), affiches haute résolution et bandes-annonces officielles YouTube.\n\n" +
                            "Ce produit utilise l'API TMDB mais n'est ni approuvé ni certifié par TMDB.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = false }) { Text("Compris") }
            }
        )
    }
}

@Composable
private fun ConfigTableRow(
    field: String,
    value: String,
    isHeader: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isHeader) MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                else Color.Transparent
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = field,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = if (isHeader) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
                fontFamily = if (!isHeader) FontFamily.Monospace else FontFamily.Default,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}

private fun calculateDirBytes(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    var total = 0L
    dir.listFiles()?.forEach { child ->
        total += if (child.isDirectory) calculateDirBytes(child) else child.length()
    }
    return total
}

private fun formatCacheBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 Ko utilisés"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.FRANCE, "%.1f Mo utilisés", mb)
    } else {
        String.format(Locale.FRANCE, "%.0f Ko utilisés", kb)
    }
}

@Composable
fun SettingsItemRow(
    title: String,
    currentValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

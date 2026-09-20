package com.verdor.musica.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdor.musica.R
import com.verdor.musica.data.SettingsStore
import com.verdor.musica.ui.theme.Danger
import com.verdor.musica.ui.theme.Green
import com.verdor.musica.ui.theme.Ink
import com.verdor.musica.ui.theme.Muted
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: SettingsStore,
    onClearLibrary: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val ytKey by settings.youtubeKeyRaw.collectAsState(initial = "")
    val jamendoId by settings.jamendoClientIdRaw.collectAsState(initial = "")
    val lang by settings.language.collectAsState(initial = "auto")

    var ytField by remember(ytKey) { mutableStateOf(ytKey) }
    var jamendoField by remember(jamendoId) { mutableStateOf(jamendoId) }
    var showConfirmClear by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(stringResource(R.string.nav_settings), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.settings_api_keys), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        if (settings.hasBuiltInYoutubeKey || settings.hasBuiltInJamendoKey) {
            Text(
                "Ya hay una clave predeterminada activa (compilada con la app). Solo llena esto si quieres usar la tuya propia en su lugar.",
                color = Green, fontSize = 11.5.sp, lineHeight = 16.sp
            )
            Spacer(Modifier.height(10.dp))
        }

        Text(stringResource(R.string.yt_key_label), color = Muted, fontSize = 12.sp)
        OutlinedTextField(value = ytField, onValueChange = { ytField = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text(stringResource(R.string.yt_key_help), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

        Text(stringResource(R.string.jamendo_key_label), color = Muted, fontSize = 12.sp)
        OutlinedTextField(value = jamendoField, onValueChange = { jamendoField = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text(stringResource(R.string.jamendo_key_help), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                scope.launch {
                    settings.setYoutubeKey(ytField.trim())
                    settings.setJamendoClientId(jamendoField.trim())
                    saved = true
                }
            }) { Text(stringResource(R.string.save)) }
            if (saved) {
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.saved), color = Green, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Idioma / Language", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("auto" to "Auto", "es" to "Español", "en" to "English").forEach { (key, label) ->
                FilterChip(
                    selected = lang == key,
                    onClick = { scope.launch { settings.setLanguage(key) } },
                    label = { Text(label) }
                )
            }
        }
        Text(
            "El cambio de idioma en Android normalmente se aplica desde Ajustes del sistema > Idiomas de la app. Esta preferencia queda guardada para cuando se active esa integración.",
            color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(24.dp))
        Text("Almacenamiento", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { showConfirmClear = true },
            colors = ButtonDefaults.buttonColors(containerColor = Danger)
        ) { Text(stringResource(R.string.clear_library)) }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            confirmButton = {
                TextButton(onClick = { showConfirmClear = false; onClearLibrary() }) { Text(stringResource(R.string.clear_library)) }
            },
            dismissButton = { TextButton(onClick = { showConfirmClear = false }) { Text("Cancelar") } },
            text = { Text(stringResource(R.string.confirm_clear)) }
        )
    }
}

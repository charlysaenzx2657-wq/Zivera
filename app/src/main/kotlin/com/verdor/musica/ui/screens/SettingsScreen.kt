package com.verdor.musica.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    onClearLibrary: () -> Unit,
    userEmail: String?,
    authBusy: Boolean,
    authError: String?,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onSignInWithGoogle: () -> Unit,
    offlineModeManual: Boolean,
    onSetOfflineMode: (Boolean) -> Unit,
    isOnline: Boolean
) {
    val scope = rememberCoroutineScope()
    val lang by settings.language.collectAsState(initial = "auto")

    var showConfirmClear by remember { mutableStateOf(false) }
    var emailField by remember { mutableStateOf("") }
    var passwordField by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(stringResource(R.string.nav_settings), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)

        // ---- Account ----
        Spacer(Modifier.height(18.dp))
        Text("Cuenta", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        if (userEmail != null) {
            Text("Sesión iniciada como", color = Muted, fontSize = 12.sp)
            Text(userEmail, color = Ink, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            Text(
                "Tus favoritos y qué has descargado se guardan en la nube y se sincronizan si usas la app en otro dispositivo.",
                color = Muted, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 10.dp)
            )
            Button(onClick = onSignOut, colors = ButtonDefaults.buttonColors(containerColor = Danger)) {
                Text("Cerrar sesión")
            }
        } else {
            Text(
                "Inicia sesión para guardar tus favoritos y descargas en la nube (opcional — la app funciona igual sin cuenta, solo local).",
                color = Muted, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 10.dp)
            )

            OutlinedButton(onClick = onSignInWithGoogle, modifier = Modifier.fillMaxWidth()) {
                Text("Continuar con Google")
            }
            Spacer(Modifier.height(14.dp))
            Text("— o con correo —", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = emailField, onValueChange = { emailField = it },
                label = { Text("Correo") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = passwordField, onValueChange = { passwordField = it },
                label = { Text("Contraseña") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            if (authError != null) {
                Text(authError, color = Danger, fontSize = 11.5.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = !authBusy && emailField.isNotBlank() && passwordField.isNotBlank(),
                    onClick = { onSignIn(emailField.trim(), passwordField) }
                ) { Text("Iniciar sesión") }
                OutlinedButton(
                    enabled = !authBusy && emailField.isNotBlank() && passwordField.isNotBlank(),
                    onClick = { onSignUp(emailField.trim(), passwordField) }
                ) { Text("Crear cuenta") }
            }
            if (authBusy) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        // ---- Offline mode ----
        Spacer(Modifier.height(24.dp))
        Text("Modo offline", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Solo música descargada", color = Ink, fontSize = 13.5.sp)
                Text(
                    if (!isOnline) "Sin conexión detectada — activado automáticamente"
                    else "Actívalo para navegar solo tu biblioteca, aunque tengas internet",
                    color = Muted, fontSize = 11.sp, lineHeight = 15.sp
                )
            }
            Switch(
                checked = offlineModeManual,
                onCheckedChange = onSetOfflineMode,
                colors = SwitchDefaults.colors(checkedTrackColor = Green)
            )
        }

        // ---- Language ----
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

        // ---- Storage ----
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

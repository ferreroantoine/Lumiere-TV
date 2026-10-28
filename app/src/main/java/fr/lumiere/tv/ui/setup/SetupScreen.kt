package fr.lumiere.tv.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.ui.components.TvTextField
import fr.lumiere.tv.ui.containerViewModel
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.Safe

/** Saisie de l'adresse du serveur Lumière (premier lancement, ou changement de serveur). */
@Composable
fun SetupScreen(initialAddress: String, onConnected: () -> Unit) {
    val vm = containerViewModel { SetupViewModel(it.serverCheck::check, it.serverStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf(initialAddress) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(state) { if (state is SetupState.Connected) onConnected() }

    Box(Modifier.fillMaxSize().padding(horizontal = Safe.Horizontal, vertical = Safe.Vertical), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.widthIn(max = 860.dp)) {
            Text("Connexion à Lumière", style = MaterialTheme.typography.displaySmall)
            Text(
                "Saisissez l'adresse de votre serveur Lumière : son IP publique ou son nom de domaine, avec le port (8484 par défaut).",
                style = MaterialTheme.typography.bodyLarge,
                color = LumiereColors.TextMuted,
            )
            TvTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = "Exemple : 82.64.10.20:8484",
                keyboardType = KeyboardType.Uri,
                onSubmit = { vm.submit(input) },
                modifier = Modifier.focusRequester(focus),
            )
            Button(onClick = { vm.submit(input) }, enabled = state != SetupState.Checking) {
                Text(if (state == SetupState.Checking) "Test en cours…" else "Tester et enregistrer")
            }
            when (val s = state) {
                is SetupState.Failed -> Text(s.message, color = LumiereColors.Error, style = MaterialTheme.typography.bodyLarge)
                is SetupState.Connected -> Text("Connecté à Lumière ${s.version}.", color = LumiereColors.Brand300)
                else -> {}
            }
            Text(
                "Votre serveur Lumière est exposé sur Internet sans mot de passe.",
                style = MaterialTheme.typography.bodySmall,
                color = LumiereColors.TextMuted,
            )
        }
    }
}

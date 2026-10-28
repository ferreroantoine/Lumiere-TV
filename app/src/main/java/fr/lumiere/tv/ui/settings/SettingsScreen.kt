package fr.lumiere.tv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.BuildConfig
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.theme.LumiereColors
import kotlinx.coroutines.launch

/** Réglages de l'appli : adresse du serveur et version. Les réglages de Lumière restent sur le web. */
@Composable
fun SettingsScreen(onChangeServer: () -> Unit, modifier: Modifier = Modifier) {
    val container = appContainer()
    val address by container.currentAddress.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        Text("Réglages", style = MaterialTheme.typography.headlineMedium)
        Text("Serveur Lumière", style = MaterialTheme.typography.titleMedium, color = LumiereColors.TextMuted)
        Text(address?.baseUrl ?: "Aucun", style = MaterialTheme.typography.titleLarge)
        Button(onClick = {
            scope.launch {
                container.serverStore.clear()
                onChangeServer()
            }
        }) { Text("Changer de serveur") }
        Text(
            "Les clés d'API, les extensions et les téléchargements se règlent dans Lumière sur le web.",
            style = MaterialTheme.typography.bodyMedium,
            color = LumiereColors.TextMuted,
        )
        Text("Lumière TV ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = LumiereColors.TextMuted)
    }
}

package fr.lumiere.tv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.BuildConfig
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.components.GlassPanel
import fr.lumiere.tv.ui.components.LumiereButton
import fr.lumiere.tv.ui.theme.GlassVariant
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereIcons
import kotlinx.coroutines.launch

/** Réglages de l'appli : adresse du serveur et version. Les réglages de Lumière restent sur le web. */
@Composable
fun SettingsScreen(onChangeServer: () -> Unit, modifier: Modifier = Modifier) {
    val container = appContainer()
    val address by container.currentAddress.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = modifier.fillMaxSize()) {
        Text("Réglages", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        GlassPanel(variant = GlassVariant.Regular, modifier = Modifier.widthIn(max = 640.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(LumiereIcons.Server, contentDescription = null, tint = LumiereColors.Brand300, modifier = Modifier.size(22.dp))
                Text("Serveur Lumière", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Text(address?.baseUrl ?: "Aucun", style = MaterialTheme.typography.headlineSmall, color = LumiereColors.Zinc200)
            LumiereButton(
                "Changer de serveur",
                onClick = {
                    scope.launch {
                        container.serverStore.clear()
                        onChangeServer()
                    }
                },
                icon = LumiereIcons.RefreshCw,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            "Les clés d'API, les extensions et les téléchargements se règlent dans Lumière sur le web.",
            style = MaterialTheme.typography.bodyMedium,
            color = LumiereColors.TextMuted,
        )
        Text("Lumière TV ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = LumiereColors.Zinc500)
    }
}

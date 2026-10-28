package fr.lumiere.tv.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.ui.components.GlassSurface
import fr.lumiere.tv.ui.components.LogoMark
import fr.lumiere.tv.ui.theme.GlassVariant
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereIcons
import fr.lumiere.tv.ui.theme.LumiereShapes
import fr.lumiere.tv.ui.theme.glass

/** Largeurs de la barre latérale : repliée (icônes) et ouverte (libellés). */
object SidebarSize {
    val Collapsed = 76.dp
    val Expanded = 256.dp
}

fun Section.icon(): ImageVector = when (this) {
    Section.Search -> LumiereIcons.Search
    Section.Home -> LumiereIcons.Home
    Section.Movies -> LumiereIcons.Film
    Section.Series -> LumiereIcons.Tv
    Section.Library -> LumiereIcons.Library
    Section.Settings -> LumiereIcons.Settings
}

/**
 * Barre latérale incrustée de Lumière : panneau de verre flottant, repliée en icônes, ouverte
 * avec les libellés quand elle a le focus. La section affichée porte la lentille et une icône
 * ambrée ; l'entrée qui a le focus devient dorée.
 */
@Composable
fun Sidebar(
    section: Section,
    expanded: Boolean,
    itemFocus: Map<Section, FocusRequester>,
    onSelect: (Section) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width by animateDpAsState(
        if (expanded) SidebarSize.Expanded else SidebarSize.Collapsed,
        spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "barre",
    )
    Column(
        modifier = modifier.fillMaxHeight().width(width)
            // Ouverte, elle recouvre le contenu : verre épais pour que le texte dessous ne se mêle pas aux libellés.
            .glass(LumiereShapes.Panel, if (expanded) GlassVariant.Thick else GlassVariant.Regular, elevation = 24.dp)
            .padding(horizontal = 10.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp).height(40.dp)) {
            LogoMark(size = 36.dp)
            AnimatedVisibility(expanded, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    "Lumière",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 26.dp).fillMaxWidth()
                // En entrant dans le menu, le focus va sur la section affichée.
                .focusProperties { onEnter = { itemFocus.getValue(section).requestFocus() } }
                .focusGroup()
                .selectableGroup(),
        ) {
            Section.entries.forEach { item ->
                SidebarItem(
                    item,
                    selected = item == section,
                    expanded = expanded,
                    onClick = { onSelect(item) },
                    modifier = Modifier.focusRequester(itemFocus.getValue(item)),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        ConnectionStatus(expanded, Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun SidebarItem(section: Section, selected: Boolean, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = LumiereShapes.Tile,
        selected = selected,
        focusedScale = 1.04f,
        transparent = true,
    ) { focused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 17.dp, end = 12.dp),
        ) {
            Icon(
                section.icon(),
                contentDescription = section.label,
                tint = when {
                    focused -> LumiereColors.Ink950
                    selected -> LumiereColors.Brand300
                    else -> LumiereColors.Zinc400
                },
                modifier = Modifier.size(21.dp),
            )
            if (expanded) {
                Text(
                    section.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }
    }
}

/** Pastille verte de connexion au serveur, comme au pied de la barre du web. */
@Composable
private fun ConnectionStatus(expanded: Boolean, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.height(20.dp)) {
        Box(
            Modifier.size(8.dp)
                .shadow(6.dp, CircleShape, ambientColor = LumiereColors.Emerald400, spotColor = LumiereColors.Emerald400)
                .background(LumiereColors.Emerald400, CircleShape),
        )
        if (expanded) {
            Text(
                "Connecté au serveur",
                style = MaterialTheme.typography.bodySmall,
                color = LumiereColors.TextMuted,
                maxLines = 1,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

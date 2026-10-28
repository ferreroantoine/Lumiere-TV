package fr.lumiere.tv.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties

/** Entrée du menu latéral (section affichée), fournie par l'écran principal. */
val LocalSidebarFocus = staticCompositionLocalOf<FocusRequester?> { null }

/**
 * Gauche, en sortant de ce groupe (rangée, grille) par son bord gauche, ouvre le menu latéral.
 * Sans cela, la recherche du focus peut remonter vers un bouton plus haut, jugé plus proche que
 * le menu. [onOtherExit] traite les autres directions.
 */
fun Modifier.leftExitsTo(sidebar: FocusRequester?, onOtherExit: (FocusDirection) -> Boolean = { false }): Modifier =
    focusProperties {
        onExit = {
            when {
                requestedFocusDirection == FocusDirection.Left && sidebar != null -> sidebar.requestFocus()
                onOtherExit(requestedFocusDirection) -> cancelFocusChange()
            }
        }
    }

package fr.lumiere.tv.ui.navigation

import fr.lumiere.tv.data.api.LumiereJson
import fr.lumiere.tv.data.api.MediaRef
import kotlinx.serialization.Serializable

/** Destinations plein écran. Les sections (Accueil, Films…) vivent dans [Route.Main]. */
sealed interface Route {
    @Serializable data object Setup : Route
    @Serializable data object Main : Route
    /** Fiche d'un titre ; `autoPlay` lance la lecture dès la fiche chargée (bouton Lecture de l'accueil). */
    @Serializable data class Title(val id: Int, val autoPlay: Boolean = false) : Route

    /** Le média est transmis en JSON : Navigation Compose ne sait pas porter une interface scellée. */
    @Serializable data class Player(val mediaJson: String, val titleId: Int? = null) : Route {
        val media: MediaRef get() = LumiereJson.decodeFromString(MediaRef.serializer(), mediaJson)

        companion object {
            fun of(media: MediaRef, titleId: Int? = null) =
                Player(LumiereJson.encodeToString(MediaRef.serializer(), media), titleId)
        }
    }
}

/** Sections du menu latéral. */
enum class Section(val label: String) {
    Search("Rechercher"),
    Home("Accueil"),
    Movies("Films"),
    Series("Séries"),
    Library("Ma bibliothèque"),
    Settings("Réglages"),
}

package fr.lumiere.tv.data.api

import kotlinx.serialization.json.Json

/**
 * Lecture et écriture du JSON de Lumière : champs inconnus ignorés (le serveur peut en ajouter),
 * valeurs nulles non envoyées, valeurs par défaut envoyées (`mode: "auto"`).
 */
val LumiereJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

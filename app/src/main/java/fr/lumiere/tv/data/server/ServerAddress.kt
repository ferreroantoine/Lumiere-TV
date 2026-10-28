package fr.lumiere.tv.data.server

import okhttp3.HttpUrl

/** Adresse de base du serveur Lumière, sans barre finale (`http://1.2.3.4:8484`). */
@JvmInline
value class ServerAddress(val baseUrl: String) {

    companion object {
        const val DEFAULT_PORT = 8484
        private val SCHEME = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*)://")

        /**
         * Normalise une adresse saisie à la télécommande : `hôte`, `hôte:port`,
         * `http://hôte:port` ou `https://hôte[:port]`. Sans schéma, `http://` est ajouté ;
         * en `http` sans port, le port de Lumière (8484) est utilisé.
         */
        fun parse(input: String): ParseResult {
            val text = input.trim()
            if (text.isEmpty()) return ParseResult.Invalid("Saisissez l'adresse du serveur.")

            val scheme = SCHEME.find(text)?.groupValues?.get(1)?.lowercase() ?: "http"
            if (scheme != "http" && scheme != "https") {
                return ParseResult.Invalid("Adresse en http:// ou https:// uniquement.")
            }
            val rest = SCHEME.find(text)?.let { text.substring(it.range.last + 1) } ?: text
            val authority = rest.substringBefore('/')
            val host = authority.substringBefore(':')
            val portText = authority.substringAfter(':', missingDelimiterValue = "")

            val port = when {
                portText.isNotEmpty() -> portText.toIntOrNull()?.takeIf { it in 1..65535 }
                    ?: return ParseResult.Invalid("Le port doit être un nombre entre 1 et 65535.")
                scheme == "http" -> DEFAULT_PORT
                else -> null
            }

            val url = try {
                HttpUrl.Builder().scheme(scheme).host(host).apply { if (port != null) port(port) }.build()
            } catch (_: IllegalArgumentException) {
                return ParseResult.Invalid("Adresse invalide. Exemple : 82.64.10.20:8484")
            }
            return ParseResult.Ok(ServerAddress(url.toString().trimEnd('/')))
        }
    }
}

sealed interface ParseResult {
    data class Ok(val address: ServerAddress) : ParseResult
    data class Invalid(val message: String) : ParseResult
}

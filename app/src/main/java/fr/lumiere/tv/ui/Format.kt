package fr.lumiere.tv.ui

import fr.lumiere.tv.data.api.ApiException
import fr.lumiere.tv.data.api.ServerUnreachableException
import kotlin.math.roundToInt

/** État d'un écran qui charge des données du serveur. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val value: T) : UiState<T>
    data class Error(val message: String, val unreachable: Boolean) : UiState<Nothing>
}

fun Throwable.toUiError(): UiState.Error = when (this) {
    is ServerUnreachableException -> UiState.Error(
        "Lumière est injoignable. Le PC est peut-être éteint, Lumière arrêté, ou l'adresse a changé.",
        unreachable = true,
    )
    is ApiException -> UiState.Error(message, unreachable = false)
    else -> UiState.Error("Une erreur inattendue est survenue.", unreachable = false)
}

/** Libellé du bouton de reprise : « Reprendre à 42 min », « Reprendre à 1 h 05 ». */
fun formatResume(positionSec: Double): String =
    if (positionSec < 60) "Reprendre au début" else "Reprendre à ${resumeTime(positionSec)}"

/** Instant de reprise : « 42 min », « 1 h 05 ». */
fun resumeTime(positionSec: Double): String {
    val minutes = (positionSec / 60).toInt()
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h %02d".format(minutes % 60)
}

/** Taille d'un fichier : « 1,4 Go », « 700 Mo ». */
fun formatSize(bytes: Long): String {
    val gb = bytes / 1_000_000_000.0
    return if (gb >= 1) "%.1f Go".format(gb).replace('.', ',') else "${bytes / 1_000_000} Mo"
}

/** Durée d'un film ou d'un épisode : « 1 h 52 », « 45 min ». */
fun formatRuntime(minutes: Int): String =
    if (minutes < 60) "$minutes min" else "${minutes / 60} h %02d".format(minutes % 60)

/** Position de lecture en temps : « 1:02:05 », « 4:09 ». */
fun formatClock(seconds: Double): String {
    val total = seconds.coerceAtLeast(0.0).roundToInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

package fr.lumiere.tv.player

import kotlin.random.Random

/** Ce dont le contrôleur a besoin du lecteur vidéo (ExoPlayer en vrai, un faux en test). */
interface PlayerPort {
    /** Charge une URL et commence à `startPositionSec` (secondes, dans le flux chargé). */
    fun load(url: String, startPositionSec: Double)
    fun play()
    fun pause()
    fun seekTo(positionSec: Double)

    /** Position dans le flux chargé, en secondes. */
    val positionSec: Double

    /** L'image avance (faux pendant un chargement, même si la lecture est demandée). */
    val isPlaying: Boolean

    /** Lecture demandée par l'utilisateur, chargement en cours ou non (faux seulement en pause). */
    val playWhenReady: Boolean
    fun setListener(listener: Listener)
    fun release()

    interface Listener {
        fun onEnded()
        fun onError(error: Throwable)
    }
}

/**
 * Erreur du lecteur avec un message compréhensible ; `network` : flux coupé (réseau, serveur),
 * à reprendre au même endroit plutôt qu'à convertir.
 */
class PlayerFailure(message: String, val network: Boolean = false, cause: Throwable? = null) : Exception(message, cause)

private val ALPHABET = ('a'..'z') + ('A'..'Z') + ('0'..'9')

/** Identifiant d'un chargement de flux (`&stream=<id>`), accepté par le serveur (`[\w-]{1,64}`). */
fun newStreamId(): String = (1..12).map { ALPHABET[Random.nextInt(ALPHABET.size)] }.joinToString("")

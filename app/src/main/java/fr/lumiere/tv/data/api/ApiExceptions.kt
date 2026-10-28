package fr.lumiere.tv.data.api

/** Réponse en erreur du serveur Lumière ; `message` vient de `ApiErrorBody.message` (en français). */
class ApiException(val status: Int, override val message: String) : Exception(message)

/** Le serveur ne répond pas (PC éteint, Lumière arrêté, adresse changée, réseau coupé). */
class ServerUnreachableException(cause: Throwable) : Exception(cause.message, cause)

# Lumière TV : client Android TV de Lumière

Application Android TV native qui se connecte au serveur **Lumière** (plateforme de streaming
personnelle de l'utilisateur) pour parcourir le catalogue et regarder films, séries et fichiers de
la bibliothèque, à la télécommande. Cible principale : stick Xiaomi sous Android TV.

Conception validée : [docs/superpowers/specs/2026-10-28-client-android-tv-design.md](docs/superpowers/specs/2026-10-28-client-android-tv-design.md).
La lire avant toute modification de périmètre, d'architecture ou de lecture vidéo.

## Le serveur Lumière

- Dépôt : `../Lumiere` (monorepo Node/TypeScript, API Fastify, interface
  React). Documents : `README.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN.md`, `CHOIX_TECHNIQUES.md`.
- **Contrats d'API : `Lumiere/packages/shared/src/*.ts`.** Les DTO Kotlin de `data/api` en sont la
  copie : à chaque évolution, relire le fichier TypeScript correspondant plutôt que deviner un champ.
  Les routes et leurs schémas zod sont dans `Lumiere/apps/server/src/modules/*/*.routes.ts`.
- Port 8484, aucune authentification. Par défaut le serveur écoute sur `127.0.0.1` : pour la TV il
  doit être lancé avec `HOST=0.0.0.0` (PowerShell : `$env:HOST="0.0.0.0"; npm start`).
- **Le serveur est distant** : il reste sur le PC de l'utilisateur, le stick est ailleurs, sur un
  autre réseau. Le port est redirigé sur la box et l'appli joint Lumière par Internet, à l'adresse
  saisie dans ses réglages.
- **Exposé sans protection, par choix de l'utilisateur** (VPN, proxy HTTPS et authentification
  proposés et écartés pour l'instant) : HTTP en clair, aucun mot de passe. Ne pas le présenter
  comme sûr ; garder toutes les requêtes (API, images, flux, sous-titres) sur un seul
  `OkHttpClient` pour pouvoir ajouter une authentification par un intercepteur.
- Serveur de test complet hors ligne : `npm run demo` dans le dépôt Lumière (MKV multi-pistes, HEVC
  HDR, sous-titres, faux TMDB), à lancer lui aussi avec `HOST=0.0.0.0`.
- **Ne pas modifier le dépôt Lumière** depuis ce projet sans demande explicite. Si une évolution du
  serveur semble nécessaire, la proposer.

## Décisions prises (2026-10-28)

- Kotlin + Jetpack Compose for TV + Media3 ExoPlayer. Pas de Leanback, pas de WebView.
- Un seul module Gradle `app`, paquet `fr.lumiere.tv`. ViewModel + `StateFlow` par écran ;
  composition manuelle dans `AppContainer` (pas de Hilt/Koin).
- Réseau : OkHttp + kotlinx.serialization (`ignoreUnknownKeys = true`). Images : Coil, URL du
  serveur préfixées par son adresse (les URL renvoyées sont relatives).
- Périmètre v1 : accueil, catalogue films/séries, recherche, fiches, lecture, bibliothèque.
  **Hors v1** : téléchargements, paramètres de Lumière, cast ou pilotage depuis le téléphone.
- Connexion : adresse saisie dans les réglages (`hôte`, `hôte:port`, `http(s)://…` ; `http://` et
  le port 8484 par défaut), vérifiée par `/api/health` puis `/api/system`, retenue dans DataStore.
  Pas de détection automatique sur le réseau local (le serveur n'y est pas). Trafic HTTP en clair
  autorisé (configuration de sécurité réseau), HTTPS accepté.
- Cast (plus tard) : piste d'un « Lire sur la TV » relayé par le serveur Lumière, qui marche
  quels que soient les réseaux.
- Lecture : l'appli déclare ses codecs réels (`MediaCodecList`) avec les mêmes noms que
  `Lumiere/apps/web/src/lib/capabilities.ts` ; le serveur décide direct / remux / conversion.
  Sous-titres WebVTT du serveur rendus par l'appli (recalage offset/rate). Repli en
  `mode: 'transcode'` si la lecture directe échoue.
- Style « Lumière façon TV » : sombre, fond d'ambiance coloré par l'affiche en focus, panneaux
  translucides sans flou temps réel (le stick est peu puissant), focus agrandi et lumineux.

## Conventions

- Interface **entièrement en français**, y compris les messages d'erreur. Les messages d'erreur du
  serveur (`ApiErrorBody.message`) sont déjà en français : les afficher tels quels.
- Commentaires et documentation en français, dans le ton de Lumière (phrases simples, sans jargon).
- Pas d'emoji ni de tiret cadratin dans le code, les textes ou la documentation.
- Navigation : tout doit être utilisable au pavé directionnel ; chaque écran a un focus initial
  explicite et aucun élément ne doit piéger le focus. Respecter la zone de sécurité de l'écran
  (48 dp horizontal, 27 dp vertical).
- Ne pas deviner une version de dépendance, un nom d'API ou d'option : vérifier dans la
  documentation officielle ou le code avant de l'écrire.
- Tests unitaires JVM pour toute logique non graphique (DTO, adresse du serveur, WebVTT, recalage,
  capacités, positionnement en mode `restart`).

## Environnement

- PC Windows 11. Shells : PowerShell et Git Bash.
- Stick : `192.168.1.160` sur le réseau local au 2026-10-28 (192.168.1.0/24, box Freebox en
  .254), ADB réseau ouvert sur le port 5555. PC de développement : `192.168.1.25` en Wi-Fi.
  L'installation par `adb` réseau ne marche que tant que le stick est sur ce réseau ; ensuite il
  sera sur un autre réseau que le PC.
- Outils requis : JDK 17+, SDK Android (outils en ligne de commande), `platform-tools` (adb).
  Au 2026-10-28 seul un JDK 11 (AdoptOpenJDK) est présent : rien d'autre n'est installé.

## Commandes

À compléter au scaffold (wrapper Gradle). Prévu :

```
adb connect 192.168.1.160:5555     # première fois : accepter l'autorisation sur la TV
gradlew.bat assembleDebug
gradlew.bat installDebug
gradlew.bat testDebugUnitTest
```

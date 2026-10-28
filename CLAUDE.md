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
- Outils installés (2026-10-28) : JDK 21 portable dans `%LOCALAPPDATA%\Programs\jdk21`
  (`JAVA_HOME` utilisateur), SDK Android dans `%LOCALAPPDATA%\Android\Sdk` (`platforms;android-37.0`,
  `build-tools;36.0.0`, `platform-tools`, `emulator`, image `system-images;android-31;android-tv;x86`),
  AVD `lumiere_tv`. Le JDK 11 d'origine reste présent : toujours utiliser le JDK 21.
- **PC d'entreprise** : proxy `<proxy de l entreprise>` avec inspection HTTPS. Gradle passe par
  `~/.gradle/gradle.properties` (proxy + `javax.net.ssl.trustStoreType=Windows-ROOT`) ; `sdkmanager`
  par `SDKMANAGER_OPTS` avec les mêmes options.
- **FortiClient** bloquait `adb.exe` vers le réseau local. Depuis que l'utilisateur l'a désactivé
  (2026-10-28), `adb connect 192.168.1.160:5555` fonctionne et le PC est autorisé sur le stick. Si le
  blocage revient, ne pas le contourner (contrôle de sécurité de l'entreprise) : émulateur, ou APK
  copié sur le stick.
- Essais sur le stick sans ouvrir le pare-feu Windows : `adb -s 192.168.1.160:5555 reverse tcp:8484 tcp:8484`
  puis l'adresse `127.0.0.1` dans l'appli (comme pour l'émulateur).
- L'émulateur n'atteint pas le PC par `10.0.2.2` : utiliser `adb reverse tcp:8484 tcp:8484` et
  l'adresse `127.0.0.1` dans l'appli.
- Capacités constatées sur l'émulateur : vidéo `h264, hevc, vp9`, audio `aac, mp3, opus, flac`. Son
  décodeur HEVC refuse le 10 bits : la lecture repasse alors en conversion (cas couvert).
- **Stick** : Xiaomi `MiTV-AESP0` (nom de code `aquaman`), Android 9 (API 28). Capacités déclarées :
  vidéo `h264, hevc, vp9`, audio `aac, mp3, opus, flac, ac3, eac3`. Son décodeur matériel
  `OMX.amlogic.hevc.decoder.awesome` lit le HEVC 10 bits HDR : la démo « Cosmos Odyssey » (HEVC 10 bits
  + E-AC3) part en lecture directe. `screencap` montre l'image en noir (plan vidéo matériel) : seuls
  les éléments de l'interface apparaissent sur les captures.

## Commandes

Gradle passe mal par le Bash de Claude Code (hook context-mode) : le lancer via `ctx_execute`
(`cmd /c gradlew.bat ...`) ou PowerShell, avec `JAVA_HOME` sur le JDK 21.

```
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest          # tests JVM
gradlew.bat assembleRelease            # APK signé : app/build/outputs/apk/release/app-release.apk
emulator -avd lumiere_tv               # émulateur Android TV
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 reverse tcp:8484 tcp:8484
```

Signature de distribution : clé `%USERPROFILE%\.android\lumiere-tv.jks`, chemin et mot de passe dans
`local.properties` (`lumiere.keystore`, `lumiere.keystore.password`, `lumiere.key.alias`). Ne jamais
les versionner.

Démo Lumière : sa génération de vidéos échoue avec ffmpeg 9 sur le fichier VP9 (`gbrap`) ; le fichier
a été produit à la main (`-pix_fmt yuv420p`) dans `tools/demo-source/content` (ignoré par git).
Correction à proposer côté Lumière : ajouter `-pix_fmt yuv420p` à ce job.

## Écarts par rapport à la spec (décidés pendant l'implémentation)

- Repli en conversion depuis la lecture directe **et** le remux (vidéo copiée refusée par le décodeur).
- La position n'est jamais négative (le serveur peut annoncer une origine de flux juste avant zéro).
- L'avance rapide s'arrête 3 s avant la fin ; une erreur du lecteur dans les 3 dernières secondes
  vaut fin du média.
- Lecteur aligné sur le lecteur web de Lumière (`apps/web/src/features/player/PlayerPage.tsx`) :
  origine provisoire posée avant l'arrivée de `/origin`, réponses d'un ancien flux ignorées, appuis
  d'avance groupés (350 ms), relance plus loin si l'image clé précède la position quittée, reprise
  au même endroit après une coupure réseau (3 fois par minute au plus), garde pendant le passage à
  l'épisode suivant.
- Les touches du lecteur ne s'appliquent que quand l'image a le focus (cartes et panneau gardent la
  navigation au pavé directionnel).
- Le bandeau de l'accueil est fixe et présente le titre dont l'affiche a le focus.
- Film : le fichier en cours de visionnage passe avant la copie de bibliothèque pour le bouton Lecture.

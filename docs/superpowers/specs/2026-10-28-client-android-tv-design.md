# Client Android TV de Lumière : conception

Date : 2026-10-28. Statut : à relire.

## 1. Objet

Une application Android TV native, installée sur le stick Xiaomi (Android TV), qui se connecte
**à distance, par Internet**, au serveur **Lumière** resté sur le PC de l'utilisateur, et permet de parcourir le
catalogue, d'ouvrir les fiches et de regarder films, séries et fichiers de la bibliothèque, à la
télécommande.

Ce n'est **pas** une copie de l'interface web (`Lumiere/apps/web`) : même service, mais une
interface pensée pour la télévision (navigation au pavé directionnel, lecture à 3 m, peu de texte,
grandes illustrations).

### Ce que l'utilisateur a demandé

- Un client Android TV pour Lumière, cohérent avec la plateforme TV, distinct du front web.
- Le projet vit dans `androidclient`, avec un `CLAUDE.md`.
- Périmètre v1 : **regarder** (accueil, catalogue, recherche, fiches, lecture) et
  **bibliothèque**.
- Le stick et le PC **ne sont pas sur le même réseau** : le PC reste chez l'utilisateur, Lumière y
  tourne en permanence, le port est redirigé sur la box et l'appli contacte le serveur par
  Internet. L'adresse (IP publique ou nom de domaine, avec le port) se **saisit dans les
  réglages** de l'appli.
- Exposition choisie par l'utilisateur : **port ouvert sans protection** (voir section 4,
  risques acceptés).
- Style **« Lumière façon TV »**.
- Technique : **Kotlin + Jetpack Compose for TV + Media3 (ExoPlayer)**.

### Hors périmètre v1 (décidé)

- Cast ou pilotage depuis le téléphone (reporté). Piste retenue pour plus tard : un « Lire sur la
  TV » relayé par le serveur Lumière (téléphone et TV parlent au même serveur), qui fonctionne
  même quand la TV, le téléphone et le PC sont sur des réseaux différents ; demande une évolution
  de Lumière.
- Lancer ou gérer des téléchargements depuis la TV (reste sur le web).
- Modifier les paramètres de Lumière (clés, extensions) depuis la TV (reste sur le web).
- Toute modification du dépôt Lumière.

### Critères de réussite

1. Au premier lancement sur le stick, on saisit l'adresse publique du serveur Lumière (lancé avec
   `HOST=0.0.0.0`, port redirigé sur la box) ; l'appli la vérifie, la retient et s'y reconnecte
   ensuite seule.
2. On navigue dans tout le catalogue, la recherche et la bibliothèque à la télécommande, sans
   piège de focus.
3. Un film ou un épisode se lance, se met en pause, avance et recule, change de piste audio et de
   sous-titres, et reprend là où on s'était arrêté (position partagée avec le web).
4. Un MKV HEVC avec piste AC3 est lu en **lecture directe** (pas de conversion ffmpeg).
5. La fin d'un épisode propose l'épisode suivant.

## 2. Contexte : le serveur Lumière

Référence : `../Lumiere` (commit `f06ac93` au moment de la conception).
Documents utiles : `README.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN.md`, `CHOIX_TECHNIQUES.md`,
contrats d'API dans `packages/shared/src/*.ts`.

- API Fastify sous `/api`, port 8484, **sans authentification**. Écoute sur `127.0.0.1` par
  défaut : pour la TV, lancer avec `HOST=0.0.0.0`.
- Catalogue venant de TMDB, fichiers fournis par des extensions Stremio, lecture directe, remux ou
  conversion ffmpeg décidée par le serveur selon les capacités déclarées par le client.
- URL d'images relatives (`/api/images/<taille>/<fichier>`, ou point d'entrée « paresseux »
  `/api/titles/:id/artwork/:type`) : à préfixer par l'adresse du serveur.
- Erreurs au format `ApiErrorBody { error, message, details? }`, `message` déjà en français.

Routes utilisées par la v1 :

| Méthode | Route | Usage dans l'appli |
| --- | --- | --- |
| GET | `/api/health` | Test de connexion : `{ status: 'ok' }` |
| GET | `/api/system` | Test de connexion : confirmation (`version`), `tmdbConfigured` |
| GET | `/api/home` | Accueil (`HomeFeed`) |
| GET | `/api/titles?kind&q&sort&genre&page&pageSize` | Catalogue, recherche (`Page<TitleSummary>`) |
| GET | `/api/genres` | Filtre par genre |
| GET | `/api/titles/:id` | Fiche (`TitleDetails`) |
| GET | `/api/titles/:id/episodes/:saison/:episode` | Versions d'un épisode (`EpisodeFiles`) |
| GET | `/api/library`, `/api/library/:id` | Bibliothèque (`LibraryItem`) |
| GET | `/api/playback/continue` | Reprendre la lecture (`ContinueWatchingItem[]`) |
| POST | `/api/playback/sessions` | Création de session (`PlaybackRequest` vers `PlaybackSession`) |
| GET | `/api/playback/sessions/:id/stream?start&stream` | Flux vidéo |
| GET | `/api/playback/sessions/:id/streams/:streamId/origin` | Instant réel de départ (remux/conversion) |
| GET | `/api/playback/sessions/:id/subtitles/:trackId` | Sous-titres en WebVTT |
| GET | `/api/playback/sessions/:id/subtitles/online` | Sous-titres en ligne (`OnlineSubtitlesResult`) |
| GET/PUT/DELETE | `/api/playback/sessions/:id/subtitles/:trackId/sync` | Recalage enregistré (`SubtitleSyncState`) |
| POST | `/api/playback/sessions/:id/subtitles/:trackId/sync/auto` | Recalage automatique (`SubtitleSyncResult`) |
| POST | `/api/playback/sessions/:id/progress` | Position (`ProgressUpdate`) |
| DELETE | `/api/playback/sessions/:id` | Fin de session |
| PUT | `/api/playback/watched` | Marquer vu |

Les corps exacts de `GET /api/genres`, `PUT /api/playback/watched` et des requêtes `sync` sont à
lire dans `apps/server/src/modules/{catalog,playback}/*.routes.ts` au moment de les implémenter.

## 3. Architecture

Un seul module Gradle `app`, paquet racine et `applicationId` : `fr.lumiere.tv`.

```
app/src/main/java/fr/lumiere/tv/
  LumiereApp.kt          Application : crée le conteneur
  AppContainer.kt        racine de composition (client HTTP, dépôts, préférences)
  MainActivity.kt        hôte Compose, navigation
  data/
    api/                 LumiereApi (OkHttp + kotlinx.serialization), DTO miroirs de packages/shared,
                         ApiException (porte ApiErrorBody.message)
    server/              ServerAddress (normalisation de l'adresse saisie), ServerCheck (/api/health puis /api/system)
    prefs/               ServerPrefs (DataStore) : adresse du serveur retenue
  player/
    Capabilities.kt      codecs et conteneurs décodables (MediaCodecList) vers ClientCapabilities
    PlaybackController   session, flux, repositionnement « restart », pistes, progression, repli conversion
    subtitles/           parseur WebVTT, application du recalage (offset, rate), état des pistes
  ui/
    theme/               couleurs, typographie, formes (identité Lumière)
    components/          carte d'affiche, rangée, fond d'ambiance, badges de release, états vides/erreurs
    setup/ home/ catalog/ search/ title/ library/ player/ settings/
```

- **État** : un `ViewModel` par écran, exposant un `StateFlow<UiState>` (chargement, contenu,
  erreur). Pas de framework d'injection : `AppContainer` crée et câble les services, comme
  `container.ts` côté serveur.
- **Images** : Coil, URL absolues construites à partir de l'adresse du serveur.
- **Pas de SSE en v1** : les écrans se rechargent quand on y revient (aucun téléchargement à
  suivre en direct).
- **Contrats** : les DTO Kotlin recopient `packages/shared/src`. Champs inconnus ignorés
  (`ignoreUnknownKeys = true`) pour tolérer les ajouts côté serveur.

## 4. Connexion au serveur distant

Le serveur est joint **par Internet** : PC de l'utilisateur, Lumière lancé avec `HOST=0.0.0.0`,
port (8484 par défaut) redirigé sur la box vers le PC. Pas de détection automatique (le stick
n'est pas sur le réseau du PC).

1. Si une adresse est enregistrée et répond à `/api/health`, on l'utilise directement.
2. Sinon, écran **Configuration** : champ d'adresse au clavier TV. Formats acceptés : `hôte`,
   `hôte:port`, `http://hôte:port`, `https://hôte[:port]` (hôte = IP publique ou nom de domaine,
   DynDNS par exemple). Sans schéma, `http://` est ajouté ; sans port, `8484`.
3. **Tester et enregistrer** : `GET /api/health` puis `GET /api/system` (affiche la version de
   Lumière). En cas d'échec, message précis : délai dépassé (box, redirection de port, PC éteint
   ou Lumière arrêté), refus de connexion, réponse qui n'est pas Lumière.
4. Réglages de l'appli : modifier l'adresse relance cet écran.
5. Délais réseau adaptés à Internet (connexion 10 s, lecture 30 s pour l'API ; le flux vidéo est
   géré par ExoPlayer).

### Sécurité : risques acceptés

Lumière n'a **aucune authentification** et l'utilisateur a choisi d'ouvrir le port **sans
protection**, en HTTP non chiffré (VPN Tailscale, proxy HTTPS avec mot de passe et
authentification dans Lumière lui ont été proposés et écartés pour l'instant). Conséquences
connues : toute personne qui trouve le port peut parcourir le catalogue, lire les fichiers, lancer
des téléchargements et lire ou modifier les paramètres (clés d'API, extensions) ; le trafic peut
être lu sur le trajet.

Conséquences pour l'appli :

- Le trafic HTTP en clair doit être autorisé (configuration de sécurité réseau Android). HTTPS
  fonctionne aussi si l'utilisateur le met en place plus tard.
- Toutes les requêtes (API, images Coil, flux ExoPlayer, sous-titres) passent par **un seul
  `OkHttpClient`** partagé, pour qu'ajouter plus tard un en-tête d'authentification ou un mot de
  passe ne touche qu'un intercepteur.
- L'écran Configuration rappelle en une ligne que le serveur est exposé sans mot de passe.

### Débit

En dehors du domicile, la lecture est limitée par le débit **montant** de la connexion du PC. La
v1 laisse le serveur décider (direct, remux, conversion) d'après les codecs seulement ; une
qualité réduite pour les connexions lentes demanderait une évolution du serveur (hors v1).

## 5. Écrans et navigation

- **Menu latéral** (`NavigationDrawer` de Compose for TV), replié en icônes, déplié au focus :
  Rechercher, Accueil, Films, Séries, Bibliothèque, Réglages. Retour depuis une section :
  Accueil. Retour depuis l'Accueil : quitter.
- **Accueil** : bannière haute (image de fond, titre, résumé, boutons Lecture et Fiche) pour le
  titre mis en avant. Rangées : Reprendre la lecture, Films populaires, Séries populaires, Dans ma
  bibliothèque. Le fond d'ambiance prend la couleur de l'affiche qui a le focus.
- **Films / Séries** : grille d'affiches paginée (chargement au défilement). Barre en haut : tri
  (popularité, récents, titre, note) et genre.
- **Recherche** : recherche vocale (micro de la télécommande, `RecognizerIntent` si disponible) et
  clavier à l'écran. Résultats au fil de la saisie, avec un léger délai.
- **Fiche** : image de fond plein écran, titre, année, note, durée, genres, résumé.
  - Bouton **Lecture**, ou **Reprendre à 42 min** si une position existe.
  - Bouton **Versions** : liste des fichiers avec badges (résolution, HDR, codec, audio, langues
    MULTi/VFF/VOSTFR).
  - Série : onglets de saisons et rangée d'épisodes (vignette, titre, durée, barre de progression).
    Lecture d'un épisode par référence `{ type: 'episode', titleId, season, episode }`.
  - `sourceNotice` affiché quand le serveur en donne un.
- **Bibliothèque** : rangées Films et Séries, à partir de `/api/library`.
- **Réglages de l'appli** : adresse du serveur (changer), version de l'appli.

## 6. Lecteur

- **Capacités** : construites à partir de `MediaCodecList` (vidéo : h264, hevc, vp9, av1 ; audio :
  aac, mp3, opus, flac, ac3, eac3 si un décodeur ou le passthrough est disponible) ; conteneurs
  `mp4`, `mkv`, `webm`. Mêmes noms de codecs que `apps/web/src/lib/capabilities.ts`.
- **Session** : `POST /api/playback/sessions`, ExoPlayer lit `streamUrl`.
  - `seek: native` : repositionnement d'ExoPlayer (HTTP Range).
  - `seek: restart` : rechargement de `streamUrl?start=<s>&stream=<id>`, puis lecture de
    `/origin` pour connaître l'instant réel de départ ; la position affichée = origine + position
    du flux.
- **Commandes télécommande** : OK = pause et commandes ; gauche/droite = -10 s / +10 s ; haut =
  panneau des pistes ; Retour = quitter (enregistre la position et ferme la session).
- **Audio** : préférence pour le français. En lecture directe, changement de piste dans ExoPlayer.
  En remux/conversion, nouvelle session avec `audioTrack` à la position courante.
- **Sous-titres** : WebVTT du serveur, parsé et affiché par l'appli (calque Compose), grande
  taille. Recalage `display = rate * t + offset` lu sur `/sync`, réglable par pas de 0,25 s et
  enregistré (`PUT`), recalage automatique (`/sync/auto`). Sous-titres en ligne : liste à la
  demande, ou d'office si `onlineSubtitles.autoLanguage` est renseigné.
- **Progression** : `POST …/progress` toutes les 10 s, à la pause et en quittant.
- **Épisode suivant** : `session.next` ; compte à rebours de 10 s dans les 20 dernières secondes.
- **Repli** : erreur ExoPlayer en lecture directe, nouvelle session avec `mode: 'transcode'`.

## 7. Style visuel : « Lumière façon TV »

- Thème sombre, fond d'ambiance coloré par l'illustration qui a le focus (palette extraite de
  l'affiche, transition douce).
- Panneaux translucides sobres (pas de flou en temps réel, coûteux sur le stick).
- Focus très visible : carte agrandie, bordure lumineuse, ombre.
- Zone de sécurité de l'écran respectée (marges de 48 dp horizontales, 27 dp verticales).
- Typographie lisible à 3 m ; tout le texte en français.
- Références visuelles : `Lumiere/docs/DESIGN.md` et `Lumiere/docs/captures/`.

## 8. Erreurs

- Serveur injoignable : écran plein avec **Réessayer** et **Changer de serveur**, et un rappel des
  causes probables (PC éteint, Lumière arrêté, redirection de port, IP publique changée).
- Erreur d'API : `ApiErrorBody.message` affiché tel quel.
- Fiche sans fichier : `sourceNotice`, sinon « Aucun fichier proposé pour ce titre ».
- Lecture impossible même en conversion : message et retour à la fiche.

## 9. Tests

- **Unitaires (JVM)** : décodage des DTO à partir de réponses JSON d'exemple ; normalisation de
  l'adresse saisie et test de connexion (faux serveur) ; parseur WebVTT et recalage ; calcul des capacités (à partir d'une liste
  de codecs simulée) ; calcul de position en mode `restart` ; choix de la piste audio.
- **Manuels sur le stick** : avec la démo hors ligne de Lumière (`npm run demo`, serveur lancé
  avec `HOST=0.0.0.0`) qui fournit MKV multi-pistes, HEVC HDR et sous-titres. Pendant le
  développement, le stick et le PC peuvent être sur le même réseau (adresse locale saisie) ; un
  essai final se fait par l'adresse publique.

## 10. Outillage et installation

- PC Windows : JDK 17 ou plus récent, outils en ligne de commande du SDK Android, `platform-tools`
  (adb). Aucun installé au moment de la conception (seul un JDK 11 est présent) : à installer avec
  l'accord de l'utilisateur.
- Gradle avec wrapper. Versions d'AGP, Kotlin, Compose for TV, Media3, OkHttp, Coil,
  kotlinx.serialization : à vérifier dans la documentation officielle au moment du scaffold.
- Installation : `adb connect 192.168.1.160:5555` (accepter l'autorisation sur la TV), puis
  `gradlew installDebug`. Possible tant que le stick est sur le même réseau que le PC ; une fois
  le stick installé ailleurs, il faudra un APK copié sur le stick ou un accès ADB distant.
- Côté PC (à faire par l'utilisateur) : lancer Lumière avec `HOST=0.0.0.0`, rediriger le port
  sur la box vers le PC, autoriser le port dans le pare-feu Windows, noter l'IP publique (ou
  configurer un DynDNS si elle change).

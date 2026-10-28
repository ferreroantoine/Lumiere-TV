# Lumière TV

Application **Android TV** pour regarder sa bibliothèque **Lumière** à la télécommande : accueil,
films, séries, recherche (clavier ou voix), fiches avec versions, saisons et épisodes,
bibliothèque, lecteur plein écran avec pistes audio, sous-titres recalables, reprise et épisode
suivant.

L'appli ne contient aucun contenu : elle se connecte au serveur Lumière qui tourne sur votre PC,
par Internet ou sur le réseau local.

## Côté PC : rendre Lumière joignable

1. Lancer Lumière en écoutant sur le réseau :
   ```powershell
   $env:HOST="0.0.0.0"; npm start
   ```
2. Sur la box, rediriger un port (8484 par exemple) vers l'adresse locale du PC.
3. Autoriser ce port dans le pare-feu Windows.
4. Noter l'IP publique de la box. Si elle change, configurer un nom DynDNS (gratuit) et l'utiliser
   à la place.

> **Attention** : Lumière n'a pas de mot de passe. Une fois le port ouvert, toute personne qui le
> trouve peut parcourir le catalogue, lire les fichiers, lancer des téléchargements et modifier
> les paramètres, et le trafic passe en clair. Un VPN (Tailscale) ou un proxy HTTPS avec mot de
> passe éviteraient ces risques.

## Installer l'appli sur le stick

L'APK signé est produit par `gradlew.bat assembleRelease` dans
`app/build/outputs/apk/release/app-release.apk`.

- **Depuis un PC sur le même réseau que le stick** (Options pour les développeurs : débogage
  réseau activé) :
  ```bash
  adb connect <ip-du-stick>:5555
  adb install -r app-release.apk
  ```
  Au premier `adb connect`, accepter l'autorisation qui s'affiche sur la TV.
- **Sans PC à côté** : copier l'APK sur une clé USB, ou l'envoyer avec une appli de transfert
  (« Send Files to TV », « Downloader »…), puis l'ouvrir depuis le stick en autorisant
  l'installation d'applis inconnues pour cette appli.

## Premier lancement

L'appli demande l'adresse du serveur : IP publique ou nom de domaine, avec le port
(`82.64.10.20:8484`, `mon-nom.dyndns.org:8484`, `https://lumiere.exemple.fr`). Sans port, 8484 est
utilisé ; sans `http://`, il est ajouté. L'adresse est testée puis retenue ; elle se change dans
**Réglages**.

## À la télécommande

| Écran | Touches |
| --- | --- |
| Partout | Gauche : menu latéral · Retour : section précédente, puis Accueil, puis sortie |
| Fiche d'une série | OK sur un épisode : lecture · appui long : choisir une version |
| Lecteur | OK : pause · Gauche / Droite : -10 s / +10 s · Haut : pistes et sous-titres · Retour : quitter |

## Développement

Prérequis : JDK 17 ou plus récent, SDK Android (plateforme 37.0, build-tools 36.0.0), `adb`.

```bash
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest
gradlew.bat assembleRelease
```

La signature de la version de distribution lit `local.properties` (non versionné) :
`lumiere.keystore`, `lumiere.keystore.password`, `lumiere.key.alias`. Sans ces lignes, l'APK de
distribution n'est pas signé.

Serveur de test : la démo hors ligne de Lumière (`npm run demo` dans le dépôt Lumière). Avec
l'émulateur Android TV, `adb reverse tcp:8484 tcp:8484` puis l'adresse `127.0.0.1` dans l'appli.

Conception : [docs/superpowers/specs/2026-10-28-client-android-tv-design.md](docs/superpowers/specs/2026-10-28-client-android-tv-design.md).

# CLAUDE.md — Vélo 3D Pontcharra (simulateur vélo type Zwift / MyWhoosh)

Ce fichier donne à Claude Code tout le contexte pour reprendre le projet.
Le propriétaire du projet est **Cyril**, cycliste à Pontcharra (Isère). Lis aussi la section « Préférences de travail » avant de répondre.

---

## 1. Objectif du logiciel

Un simulateur de vélo d'intérieur en 3D, dans le navigateur, qui se branche sur un home trainer connecté et une ceinture cardio.
Cyril y roule sur **ses vraies routes autour de Pontcharra** : le monde 3D est construit à partir de toutes ses sorties Strava (mai 2018 → octobre 2026).
On y trouve le vrai relief, les vraies photos aériennes, les vrais bâtiments, les villages avec leurs panneaux, des cyclistes et des promeneurs.

Tout tient dans **un seul fichier HTML**, sans serveur ni installation. Il s'ouvre dans **Chrome** (ordinateur ou Android) : Web Bluetooth est obligatoire pour le home trainer.

---

## 2. Fonctionnalités déjà faites

### Monde et rendu
- **Réseau routier réel** : 908 routes, 613 nœuds dont 550 carrefours (3 routes ou plus) et 63 culs-de-sac, soit environ 949 km.
  - Il a été construit en fusionnant 498 sorties vélo Strava (14 820 km roulés).
  - 110 sorties à trace GPS complète servent de géométrie de référence.
  - Il reste environ 23 km de routes au tracé « approximatif », issues de traces simplifiées.
- **Relief** : tuiles d'altitude AWS Terrarium (SRTM) en trois niveaux (z12 proche, z10 lointain, z8 horizon).
  - Courbure terrestre au-delà de 3 km.
  - Relief de secours procédural quand on est hors ligne.
- **Photos aériennes IGN** (Géoplateforme WMTS), avec ArcGIS World Imagery en secours, en 4 couches :
  - z8 pour l'horizon ;
  - z11 pour le lointain ;
  - z13 pour la zone moyenne ;
  - z16 pour la photo proche, qui suit la caméra.
- **Bâtiments réels** IGN BD TOPO (WFS), avec repli sur OpenStreetMap (Overpass), puis sur des bâtiments procéduraux.
  - Toits variés, fenêtres percées, volets, balcons, églises.
- **Végétation** : forêts IGN/OSM et arbres déduits de la photo aérienne, avec niveaux de détail.
  - Haies, vergers, vignes.
  - Herbe animée par le vent.
- **Eau** (Isère, lacs) et ponts automatiques là où une route franchit l'eau.
- **Équipements de route** :
  - balises J6, bornes kilométriques, glissières, panneaux de virage dangereux ;
  - trottoirs et lampadaires en agglomération ;
  - plateau d'enrobé aux carrefours.
- **Ciel en temps réel** : soleil et lune à l'heure réelle, nuages, étoiles, phare du vélo la nuit.
- **Six vues caméra** : poursuite, côté, face, vue cycliste, drone, bord de route.
- **Qualité graphique réglable** (Basse, Moyenne, Élevée, Ultra) et résolution adaptative selon les images par seconde.

### Navigation sur le réseau
- Le cycliste circule de route en route. Sa position est un triplet (route, abscisse en mètres, sens).
- **Choix aux carrefours** : 10 s avant un carrefour (et à moins de 500 m), l'écran se coupe en deux, ou en trois s'il y a trois routes.
  - Le côté par défaut est en surbrillance.
  - On choisit en touchant une moitié d'écran, ou avec les flèches ← → du clavier.
  - Chaque option affiche « vers Commune », le sens du virage et le nombre de passages de Cyril sur cette route.
- **Choix par défaut** : il maximise `log(1 + passages) + 1,5 × cos(angle du virage)`, donc il privilégie les routes habituelles et le tout droit.
- **Itinéraire GPX guidé** : charger un GPX ne remplace plus le monde. Aux carrefours, le choix par défaut suit le GPX.
  - Le bouton « Parcours Pontcharra (guidé) » utilise le tracé intégré `TRK`.
- **Culs-de-sac** : demi-tour automatique. Demi-tour manuel avec la touche U ou le menu ☰.
- **Mini-carte** : réseau sur 2,4 km autour du cycliste, trajet prévu en jaune, villages en rouge, cyclistes en points de couleur.
- **Profil du bas** : altitude des 5 km à venir sur le trajet prévu.

### Cyclistes PNJ
- Leur nombre dépend de la qualité : 4 en Basse, 7 en Moyenne, 10 en Élevée, 13 en Ultra.
- Trois profils, avec étiquette de prénom et maillots de clubs locaux fictifs :
  - **costauds** : apparaissent derrière, sur le chemin réellement parcouru, et doublent ;
  - **à ta portée** : roulent à la puissance moyenne récente de Cyril (moyenne glissante sur 3 min) ;
  - **tranquilles** : apparaissent devant, on les rattrape.
- **Aspiration réaliste** : réduction de traînée `0,42 × exp(−(écart − 0,4)/2,9)`.
  - Elle est nulle au-delà de 15 m ou d'un décalage latéral de 0,95 m, et plafonnée à 50 %.
  - Elle est affichée dans le HUD (« aspiration −20 % ») et baisse le bruit du vent.
  - Elle est envoyée au home trainer en réduisant le coefficient de vent (Cw) de la commande FTMS de simulation de pente.
- **Dépassements** : décalage latéral, retour en file, et on ne rentre pas dans la roue de devant.
- **Comportement joueur** :
  - ils prennent parfois la roue de Cyril quand il les double ;
  - ils **disputent les sprints de pancarte** : attaque entre 120 et 320 m avant la pancarte, ou en réaction quand Cyril sprinte à moins de 45 m.

### Jeu des pancartes (sprints)
- Les agglomérations sont détectées par densité de bâtiments le long de chaque route et nommées avec les données IGN, OSM et l'API géo.
- Panneaux EB10 (entrée) et EB20 (sortie) dans les deux sens.
- Arche de sprint 300 m avant la sortie, si le village fait au moins 350 m.
- **Zone de sprint** : les 300 derniers mètres avant un panneau de sortie, dans le sens de marche, pour les villages d'au moins 300 m.
- **Résultat** : classement contre les PNJ (« 1er devant… » ou « 2e derrière… »), puissance maximale, record par pancarte, points.
- **Pancartes de secours** : sans données de bâtiments, des pancartes « synthétiques » sont placées aux centres des communes.

### Piétons et promeneurs
- Leur nombre dépend de la qualité : 10 en Basse, 18 en Moyenne, 26 en Élevée, 34 en Ultra. Ils sont dessinés en instanciation (7 maillages pour tous).
- Les promeneurs marchent sur les trottoirs des villages et, plus rarement, sur le bas-côté.
- Des spectateurs sont présents à 3 pancartes de sortie sur 4.
- Ils **applaudissent, sautent, sifflent et crient « Allez ! »** (synthèse vocale en français) quand Cyril sprinte, roule à plus de 41 km/h, ou quand un PNJ sprinte près d'eux.

### Cycliste et animation
- Cycliste à squelette avec pédalage correct, réalisé par cinématique inverse.
- Positions aéro et roue libre, inclinaison dans les virages.
- **Danseuse sur variation de puissance**. Il se lève quand :
  - la moyenne des 3 dernières secondes dépasse la plus grande de ces deux valeurs : 1,28 × la moyenne des 15 s précédentes, ou cette moyenne + 70 W ;
  - et qu'elle dépasse aussi 0,8 × FTP.

  Il se rassoit dès que la puissance retombe sous la référence × 1,12 + 25 W, au plus tôt après 3 s, et au plus tard après 25 s.
- Il se met aussi en danseuse dans les fortes pentes et pendant les sprints.

### Matériel et entraînement
- **Home trainer FTMS** en Web Bluetooth.
  - Service 0x1826 ; Indoor Bike Data 0x2AD2 ; Control Point 0x2AD9.
  - Filtres de nom : CYCPLUS, Cycplus, T2.
  - Simulation de pente : opcode 0x11, pente en 0,01 %, Crr 0,0040, Cw 0,19 kg/m réduit par l'aspiration.
  - Mode ERG : opcode 0x05.
- **Ceinture cardio** : service 0x180D, caractéristique 0x2A37.
- **Séances ERG** : endurance, sweet spot, seuil, VO2max, force, sprints, avec zones Coggan selon la FTP saisie et intensité réglable de 50 à 150 %.
- **Test FTP en rampe** : la FTP retenue est 75 % de la meilleure minute.
- **Garage** : points, équipement améliorable, historique FTP, records de sprint.
- **Pièce d'accueil en 3D** : vélo sur home trainer, bureau avec écran animé, posters.
- **Sons de nature** générés par WebAudio : vent, feuilles, rivière, grillons, oiseaux, roue libre.

### Strava
- Enregistrement à 1 Hz pendant les sorties et les séances : position GPS réelle, altitude, distance, vitesse, puissance, fréquence cardiaque, cadence.
- En quittant la sortie, une **fenêtre demande** s'il faut l'envoyer sur Strava. Rien ne part sans validation.
- **Fichier FIT** encodé dans le navigateur.
  - Messages : file_id, event, record, lap, session et activity.
  - Session en `sport = cycling (2)`, `sub_sport = virtual_activity (58)`.
  - Le fichier a été vérifié avec la bibliothèque Python `fitdecode`, contrôle des sommes de vérification (CRC) compris.
- **Envoi direct** avec l'application API Strava personnelle de Cyril (OAuth).
  - Il faut l'ID client et le secret, puis coller l'URL de retour (`http://localhost/velo3d?...code=...`).
  - Envoi via `POST /api/v3/uploads` (`data_type=fit`, `trainer=1`), suivi de l'état du traitement.
  - Puis `PUT /activities/{id}` avec `sport_type: VirtualRide`.
- **Secours** : bouton « Télécharger le .fit » pour l'importer à la main sur strava.com/upload.

---

## 3. Choix techniques

### Fichier unique
- Livrable : **un seul fichier HTML** d'environ 1,2 Mo et 2 721 lignes, sans dépendance réseau pour le code.
- Le fichier `index.html` du dépôt est ce fichier (livré à l'origine sous le nom `velo-3d-pontcharra.html`).
- **three.js r150** est **inclus en entier dans le fichier** (version minifiée) : pas de CDN, pas de modules ES, pas d'outil de build.
- JavaScript « classique » en mode strict, sans framework. Tout le code applicatif est dans un seul `<script>`.

### Plan du fichier (dans l'ordre)
Les numéros de ligne sont approximatifs. Repère-toi avec les en-têtes `// ====================== … ======================`.

| Lignes ≈ | Contenu |
|---|---|
| 1–117 | `<head>` : CSS de l'interface, puis le HTML (HUD, menu ☰, accueil, écrans, fenêtre `#res`) |
| 118–125 | `<script>` **three.js r150 minifié**. **NE PAS LIRE NI MODIFIER** : lignes énormes. |
| 126–2719 | `<script>` de l'application, sections ci-dessous |
| 129 | `const TRK="lat,lon,alt;…"` : parcours Pontcharra par défaut (une seule ligne très longue) |
| 130 | `const NETD={"E":[…]}` : **réseau routier** (une ligne d'environ 285 Ko, ne pas l'afficher en entier) |
| ≈131 | Base : utilitaires, préréglages de qualité `QPRE`, file de tâches `job()`/`runJobs()`, cache réseau, classe `GB` (constructeur de géométrie) |
| ≈245 | Rendu, ciel, lumière, astronomie (`skyUpdate`, `FORCE_SUN` pour les tests) |
| ≈364 | Textures procédurales |
| ≈517 | **Réseau routier** : `initNetwork`, `roadQ`, `posAt`, `finishTrack`, `buildCoarse`, relief `ground`/`hT` |
| ≈652 | Terrain : tuiles proches, moyennes et lointaines, horizon, couloir le long des routes, mosaïques photo, photo proche `CLIP` |
| ≈877 | Données cartographiques : WFS IGN, Overpass, procédural ; chargement dynamique autour du cycliste (`dataLoaderTick`) |
| ≈1043 | Route : chaussée par tronçon, plateaux de carrefour, ponts, mobilier |
| ≈1151 | Végétation et herbe |
| ≈1360 | Bâtiments |
| ≈1585 | Eau |
| ≈1612 | Agglomérations : détection par route, noms, panneaux, arches (`villagesTick`) |
| ≈1703 | Cycliste et vélo : `makeRiderInst`, `riderAnim(dt, état, instance)`, maillots `jerseyCanvas` |
| ≈1988 | Application : état global, FTMS, cardio, séances, interface (`nav`, `prof`) |
| ≈2095 | Jeu : sprints, points, garage, sons, pièce d'accueil |
| ≈2217 | **Navigation** : `RID`, `advance`, `optionsAt`, `defaultIdx`, itinéraire GPX `setRoute`, choix au carrefour `jxTick` |
| ≈2301 | **Cyclistes PNJ** : `npcTick`, aspiration `draftOf`, sprints `exitAhead` |
| ≈2402 | **Piétons** : `pedTick`, applaudissements, sifflets, voix |
| ≈2467 | **Strava** : enregistreur `REC`, `fitEncode`, OAuth, envoi, fenêtre `offerStrava`, `stravaPanel` |
| ≈2549 | Monde et boucle principale : `startWorld`, caméras, `loop()`, mini-carte, menu ☰, démarrage |

### Repère et unités
- L'origine est le **centre de Pontcharra** : `LA = 45.4225`, `LO0 = 6.0228`.
- Conversion : `x = (lon − LO0) × KX` (vers l'est), `z = −(lat − LA) × 110574` (vers le **sud**), avec `KX = 111320 × cos(LA)`. +Y est vers le haut. Fonctions `ll2xz` et `xz2ll`.
- Repère du cycliste : +Z vers l'avant, +X à sa gauche. Un décalage latéral positif sur la route va **à droite**, selon la direction `(−tz, tx)`.

### Format du réseau `NETD`
- `NETD.E[i]` est la route numéro i. **C'est le même numéro que « Route i » / « #i » dans la carte de validation 2D.**
  - `a`, `b` : identifiants des nœuds aux extrémités.
  - `g` : points (x, z) en **décimètres**, codés en deltas d'entiers selon le principe des polylignes Google. Décodage : `decInts(s, 2)`.
  - `z` : altitude en décimètres tous les 20 m, codée de la même façon (`decInts(s, 1)`).
  - `u` : nombre de sorties qui empruntent la route.
  - `c` : 1 si le tracé est approximatif.
  - `lf`, `lb` : commune indiquée « vers … » dans le sens a→b et dans le sens b→a.
- Au chargement, `initNetwork()` rééchantillonne toutes les routes **tous les 5 m** (`SEG`) dans des tableaux communs (`X, Z, H, G, TX, TZ, DA, SD, EOF, LNK`, environ 191 000 échantillons).
- Les tableaux sont découpés en tronçons (`CHUNKS`) de 240 échantillons au plus, jamais à cheval sur deux routes.
- Le réseau a été produit hors ligne par des scripts Python, absents du dépôt :
  1. fusion des traces Strava (rayon 14 m) ;
  2. déduplication, carrefours, élagage, lissage ;
  3. comptage des passages ;
  4. export.

### Performances
- Tout le travail lourd passe par la file `job(fn, priorité, clé)`, exécutée par `runJobs(budget en ms)` à chaque image pour éviter les saccades.
- Les clés des tâches sont gérées par un `Set` (`JKEYS`) et la file est triée une image sur six.
- Les données cartographiques (tuiles de 2 048 m) sont chargées **autour du cycliste** (rayon de 4,5 à 6,5 km), jamais pour tout le réseau d'un coup.
- Cache navigateur : Cache Storage `velo3d-data-v4` pour les tuiles, le WFS et Overpass.

### Sources de données externes (dans le navigateur)
- Altitude : `https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png`
- Photos : `https://data.geopf.fr/wmts?…ORTHOIMAGERY.ORTHOPHOTOS…`, avec ArcGIS World Imagery en secours.
- Données : `https://data.geopf.fr/wfs/ows`. Couches BDTOPO_V3 : `batiment`, `zone_de_vegetation`, `surface_hydrographique`, `zone_d_habitation`.
- OSM : Overpass (3 miroirs). Communes : `https://geo.api.gouv.fr/communes`.

### Stockage local (localStorage)
- `v3q` : qualité graphique.
- `ftp`, `kg` : FTP et poids.
- `v3gpx`, `v3gpxn` : itinéraire GPX guidé.
- `vg3` : jeu (points, records, équipement).
- `velo3d-strava` : ID client, secret et jetons Strava. **Ne jamais les committer.**

### Tests utilisés jusqu'ici
- Chromium sans interface (Playwright, rendu logiciel SwiftShader), avec des données simulées pour le relief, les photos et le WFS.
- Les fonctions sont appelées directement depuis la page : `nav('ride')`, `advance`, `npcTick`, `gameTick`, `fitEncode`…
- Aides pour les tests : `FORCE_SUN = new THREE.Vector3(.4,.6,.5)` force le plein jour, `JOB_BOOST = 400` accélère les chargements.
- Le banc de test n'est pas dans le dépôt.

---

## 4. Problèmes connus et points non vérifiés

1. **Jamais testé sur une vraie carte graphique.**
   - Les tests tournaient en rendu logiciel, à moins d'une image par seconde. La fluidité réelle sur le PC et le téléphone de Cyril est inconnue.
   - `initNetwork()` prend environ 0,7 s dans ce banc. La création d'un PNJ prend environ 150 ms.
2. **Envoi Strava direct jamais testé de bout en bout** : Strava n'était pas joignable depuis l'environnement de test.
   - On suppose que l'API Strava accepte les requêtes du navigateur (CORS).
   - Si ça échoue, le message d'erreur propose le téléchargement du .fit.
   - Le secret client reste stocké dans le navigateur : c'est acceptable pour un usage personnel, à ne jamais publier.
3. **Home trainer et cardio non retestés** après la refonte réseau : pas de matériel dans l'environnement de test.
   - L'effet de l'aspiration sur la résistance dépend de la façon dont le home trainer applique le Cw.
4. **Trous temporaires dans le terrain au loin** au démarrage : il y a environ 820 tuiles proches à construire pour tout le réseau. Ils se comblent à mesure que la file de tâches avance.
5. **Petits risques de scintillement (z-fighting)** :
   - aux carrefours entre deux chaussées de même classe (`cls`) ;
   - le long des frontières entre deux couloirs de terrain, qui se recouvrent sur 0,6 m.
6. **Données du réseau** :
   - environ 23 km de routes approximatives (virages coupés) ;
   - 3 petits bouts de route isolés de 0,2 à 0,7 km, sans carrefour détecté ;
   - quelques détours d'un jour ou erreurs GPS (322 km de routes ne sont vus qu'une seule fois).
7. **Libellés « vers … »** : ils sont calculés hors ligne à partir des centres de communes et peuvent être approximatifs.
8. **PNJ lourds** : environ 81 000 sommets chacun (géométrie fusionnée non partagée). Ça peut peser sur mobile.
9. **Itinéraire GPX** : s'il emprunte une route absente du réseau, le HUD affiche « hors itinéraire » et les carrefours reviennent au choix habituel. Il n'y a pas d'autre alerte.
10. **Cul-de-sac** : le demi-tour automatique se fait sans message.
11. **Changements par rapport à la version précédente** :
    - en mode Exploration, le bouton ◀ fait maintenant demi-tour ;
    - les anciens records de sprint ne correspondent plus aux nouvelles clés (`NOM@route±`).
12. **Voix « Allez ! »** : elle dépend d'une voix française installée dans le navigateur.
13. **Confidentialité** : le réseau couvre les rues autour du domicile de Cyril. **Le dépôt doit rester privé.**

---

## 5. Ce qu'il reste à faire (par priorité)

1. **Tester sur le matériel de Cyril** : Chrome sur PC et sur mobile, avec le home trainer et la ceinture cardio.
   - Mesurer la fluidité.
   - Ajuster le nombre de PNJ et de piétons, et les distances d'affichage.
2. **Corriger le réseau avec les remarques de la carte 2D.**
   - Cyril colle des lignes du type `#123 À retirer — …` ou `#45 Tracé faux — …`, plus une liste de routes manquantes.
   - `#n` correspond à `NETD.E[n]`.
   - Retirer une route : supprimer l'entrée et vérifier les carrefours (nœuds `a`/`b`).
3. **Tester l'envoi Strava réel** (création de l'application API, autorisation, envoi, type « Vélo virtuel »). Si CORS bloque, garder le .fit et améliorer les explications.
4. **Alléger les PNJ** : partager les géométries entre PNJ, ou ajouter une version simplifiée au-delà de 60 m.
5. **Petits plus envisagés, non demandés à ce stade** : messages au cul-de-sac et quand on sort de l'itinéraire, mémorisation de la dernière position, groupes de PNJ qui roulent ensemble.

---

## 6. Préférences de travail de Cyril

- **Toujours répondre en français.**
- **Explications simples, étape par étape.** Donner l'information progressivement plutôt que tout d'un coup.
- **Réponses directes et pratiques**, orientées action.
- **Vérifier les chiffres** (watts, distances, temps, pentes) et les termes techniques **avant de les affirmer**. Ne pas répondre de mémoire : Cyril a déjà relevé des erreurs de ce type.
- **Livrer un fichier HTML complet et fonctionnel**, pas un correctif à appliquer à la main.
- **Conserver toutes les fonctions existantes** à chaque évolution, sauf demande contraire.
- Il teste dans **Chrome** (Web Bluetooth). Le rendu doit fonctionner sur ordinateur et sur smartphone, en portrait comme en paysage.
- Prévenir clairement de ce qui n'a pas pu être testé.
- **Modifier `index.html` par remplacements ciblés.** Ne jamais lire ni réécrire en entier les lignes de three.js, `TRK` et `NETD`. Utiliser la recherche (grep) sur les en-têtes de section.

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
- **Relief** : tuiles d'altitude AWS Terrarium (SRTM) en trois niveaux (z12 proche, z10 lointain, z8 horizon). Couches de terrain : couloir le long des routes, tuiles proches, moyennes, lointain, anneau d'horizon.
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
- **Six vues caméra** : poursuite, côté, face, vue cycliste, drone, bord de route (+ gros plan visage).
- **Virages adoucis** (24 octobre 2026, `roundCorners`) : rayon minimum `RMIN` = 16 m sur toutes les routes (lissage local des points trop anguleux, carrefours fixes). Mesuré : plus aucun point anguleux, rayon mini 15,2 m. Les virages serrés peuvent s'écarter de quelques mètres de la photo aérienne.
- **Pente maximale 15 %** (`GMAX`, `slopeLimit`) : profil de chaque route ramené dans l'enveloppe des pentes permises, carrefours rapprochés en hauteur si besoin ; la pente envoyée au home trainer et la physique ne dépassent plus 15 %. Mesuré sur le réseau et sur un relief artificiel très raide : aucun pas de 5 m au-dessus de 15 %. Le terrain se raccorde à la route (talus, déblais) sur ~30 m.
- **Qualité graphique réglable** (Tablette, Basse, Moyenne, Élevée, Ultra) et résolution adaptative selon les images par seconde.
  - **Tablette** (pensée pour la Redmi Pad SE : Snapdragon 680, GPU Adreno 610) : pas d'anticrénelage, ombres 1024 non adoucies, pas de nuages, distances réduites (routes 3,2 km), 3 PNJ allégés, 8 piétons, résolution 1× pouvant descendre à 0,55×.
  - Choisie automatiquement au premier lancement quand le processeur graphique est d'entrée de gamme (Adreno ≤ 63x, Mali, PowerVR, SwiftShader), lu via `WEBGL_debug_renderer_info` : Chrome sur tablette se présente souvent comme un ordinateur.
- **Autres routes et rues** (jamais roulées) : affichées en 3D mais **non roulables**.
  - Source : couche IGN `BDTOPO_V3:troncon_de_route` chargée avec les bâtiments (attributs lus : `nature`, `largeur_de_chaussee`, `nombre_de_voies`, `position_par_rapport_au_sol`) ; sinon les `highway` d'OpenStreetMap.
  - Les parties qui doublent une route du réseau sont retirées ; chaque morceau s'arrête au bord de la chaussée roulable.
  - Goudron, chemin (gravier) ou sentier (terre), largeur selon les données ; tunnels et escaliers ignorés.
  - Ribbons posés sur le terrain tel que le maillage le dessine (`terrH`), par cellules de 512 m (`ORL`), affichés jusqu'à `QP.near` (560 m en Tablette). Pas d'arbre ni d'herbe dessus (code 253 dans les masques).
  - Dessinées en gris fin sur la mini-carte et la grande carte.
- **Forêts allégées de 90 %** (`FOREST_K = 0,1` dans `genCell`) : massifs IGN/OSM et arbres déduits de la photo. Haies, vergers et peupliers inchangés.
- **Allègement du rendu (fin octobre 2026, toutes qualités)**, après mesure de ce qui charge la carte graphique (le sol ≈ 75 % du travail, l'horizon ≈ 10 %, les ombres ≈ 5 %, les bâtiments, PNJ et piétons < 1 % chacun) :
  - **A. Sol = photo aérienne seule** (`TFRAG`) : plus de textures de détail, de rocher ni de neige ; en général 1 lecture de texture par pixel au lieu de 3 à 6 ;
  - **B. Moins de sol dessiné deux fois** : la tuile proche (16 m) est trouée sous le couloir de la route jusqu'à `CUT_D` = 52 m (36 m avant) ; 36 m gardés dans les virages serrés (rayon < 58 m, couloir rogné côté intérieur) et sur les 20 derniers mètres avant un carrefour. Vérifié par rayons verticaux tous les 2 m : aucun nouveau trou ;
  - **C. Résolution × 0,8** (`RES_K`, `PRMAX()`) : 36 % de pixels en moins ; la résolution adaptative reste active en dessous ;
  - mesuré (rendu logiciel, 1280 × 800, 3 endroits du parcours Pontcharra) : −19 % avec A + B, −41 % avec A + B + C par rapport à avant.
- ~~Aucun arbre ni herbe en 3D~~ remplacé (24 octobre 2026, demande de Cyril) par une **végétation légère** (`NOVEG = false`, `VLIGHT = true`) :
  - **quelques arbres**, version simplifiée (84/96 triangles) : 5 % des massifs IGN/OSM (`FOREST_K = 0,05`), haies, vergers et peupliers à moitié, arbres isolés en bosquets dans les prés (bruit `vn`) ; **plus d'arbres déduits de la photo** (lecture des photos `getImageData` trop coûteuse sur tablette ; toiles photo toujours sans `willReadFrequently`) ;
  - **herbe courte** (8 à 16 cm) autour du cycliste ;
  - **clôtures des prés** (`buildFurniture`) : bois à deux lisses ou piquets et fils, par tronçons de 140 m, à 6,6–8,2 m de l'axe, hors village, forêt, eau, bâtiments, autres routes et talus.
- **Route allégée** : texture 128 × 512 (au lieu de 256 × 1024), bruit simple, marquages en aplats, filtrage anisotrope ≤ 4.
- **Animaux et sons de campagne** (24 octobre 2026, section `animaux et sons de campagne`, `FAU`, `fauTick`, `fauCell`, `fauSounds`) :
  - placement déterministe par cellule de 256 m (calcul par tranches dans la file `job`, seulement quand les données de la zone sont chargées) ;
  - **vaches** (robes rouge et blanche, fauve, noire, blanche) et **moutons** en troupeaux dans les prés (hors routes, bâtiments, eau, forêts, vignes, pente douce) ; tête qui broute et queue animées dans le shader (`fauMesh`, attribut `aK`) ;
  - **chiens et chats** devant certaines maisons du bord de la route : le chien se tourne vers toi, saute et remue la queue ; **écureuil** rare qui monte et descend un tronc près de la route ;
  - **sons positionnés** (volume selon la distance, gauche/droite) : cloches des vaches (graves) ou clochettes des moutons, meuglements, bêlements, aboiements au passage, miaulements ; **cris d'enfants** près des écoles (jours d'école, plus forts à la récréation 10 h–10 h 20 et 15 h–15 h 15 et à midi ; rien le mercredi après-midi, le week-end, en juillet-août) ; **cloches d'église** en passant à moins de 220 m (heures sonnées si on passe dans les 4 premières minutes de l'heure, 7 h–21 h) ;
  - écoles : couche IGN `BDTOPO_V3:zone_d_activite_ou_d_interet` (nature « enseignement… »), bâtiments OSM `school`, sinon une école supposée au centre de chaque village (`addSchool`).
- **Arbres simplifiés** (fin octobre 2026) : feuillus proches 84 triangles au lieu de 142 (moins de cartes de feuillage, 2 branches, cœur en dodécaèdre), conifères 96 au lieu de 124 ; arbres détaillés seulement jusqu'à `treeNear` × 0,6 (33 m en Tablette au lieu de 55), version simple au-delà. Distance d'affichage des arbres, relief lointain, horizon et montagnes inchangés.

### Vélodrome (fin octobre 2026)
- **Piste UCI 250 m** (Rouler → 🏟️ Vélodrome, ou au départ d'une séance : « Où faire la séance ? » → Vélodrome) ; module `VEL`, `velBuild`, `velFrame` (branche de `loop()` : monde extérieur caché, `VELG` affiché).
  - Bord intérieur : demi-droite, raccordements progressifs (clothoïdes 13 m), virages R = 22 m ; longueur des droites ajustée (≈ 42,4 m) pour que la **ligne de mesure (20 cm au-dessus du bord intérieur) fasse 250,000 m** (vérifié). Largeur 7 m (minimum UCI cat. 1 et 2), relevé 12° en ligne droite → 44° en virage (≈ 45° pour une piste de 250 m).
  - Côte d'azur 0,70 m (10 % de la largeur, relevé 14°), zone de sécurité 4,2 m, lignes : mesure noire à 20 cm, sprinteurs rouge à 85 cm (certaines sources disent 90 cm), stayers bleue à 2,45 m (≥ 1/3 de 7 m), arrivée, 200 m, lignes de poursuite.
  - Repère : `u` le long du bord intérieur (0 = ligne de poursuite de la ligne droite d'arrivée), `w` = distance sur la piste depuis le bord intérieur ; `velPos`, `velPlace` (cycliste perpendiculaire à la piste relevée), `velLW(w)` longueur d'un tour selon la hauteur.
- **Physique** : piste plate (pente 0 envoyée au home trainer en sortie libre, ERG en séance), aspiration derrière les coureurs (même formule que sur route), on monte doubler par le haut puis on redescend à la corde.
- **Tours** : chrono au passage de la ligne d'arrivée (le premier passage lance le chrono), dernier tour, record de la session et record absolu (`GS.vel = {best, laps}`), annonce vocale tous les 5 tours, célébration au record.
- **Lumière de rythme** (ruban LED sur la côte d'azur) : point vert qui roule à la vitesse correspondant à la puissance cible (sortie : Z2 65 % FTP, Z3 83 %, seuil, éteinte — touche le bandeau du haut ; séance : consigne du bloc). Point doré = rythme de ton record du tour.
- **Arène** : piste en bois (lames), rambarde vitrée, mur LED défilant, tribunes (14 rangs, 8 en Tablette/Basse), sièges et public instanciés (corps, tête, bras, jambes ; animés dans le shader : le public saute quand ça accélère, **lève les bras au passage du cycliste** (`VEL.cr.uRu`, `uNear`), **ola** qui fait deux tours de l'arène toutes les 45 à 105 s et tout de suite au record (`VEL.ola`), **drapeaux** France / Savoie / Pontcharra / damier qui flottent, **pancartes** « ALLEZ CYRIL ! », « PONTCHARRA », « PLUS VITE ! » levées au passage ; flashs de téléphones), toit en dôme avec arches et lampes, écran circulaire suspendu (tour, chrono, record, vitesse, puissance), sol central lumineux.
- **Jeux de lumière** (`velShow`) : couleurs selon ta zone de puissance, faisceaux mobiles (8, ou 4 en Tablette/Basse) qui suivent le cycliste ou balaient la piste, stroboscope et lasers en sprint (> 140 % FTP), mode doré + confettis + ovation au record du tour, vague lumineuse à chaque passage de ligne.
- **Plein écran automatique** à l'entrée du vélodrome (`fsEnter` dans `velStart`) et bouton **⛶** à côté du ☰ pendant toutes les sorties (`#fsb`).
- **Caméras** (`velCam`, bouton caméra, noms `VCAMN`) : poursuite, « télé » depuis le centre, vue haute, vue du cycliste (penchée avec la piste), **gros plan visage**. Mini-carte : l'ovale, toi, les coureurs, la lumière de rythme et le fantôme du record.
- **Sons** (`velAudio`) : rumeur du public (plus forte au passage de l'ola), applaudissements ; quand ça chauffe (sprint, record, ola) **banda** (grosse caisse « boum, boum, boum-boum-boum » `kick` + claps en rythme), **cornes** (`horn`), cloches, sifflets ; **roues sur la piste en bois** (`AU.wood` : grondement grave de la piste creuse + chuintement des pneus, modulés au rythme de rotation des roues, plus fort quand des coureurs roulent près de toi) ; **souffle** quand on double ou qu'on est doublé (`whoosh`) ; sons de nature coupés en salle. Trois cyclistes PNJ (pris dans `NPCS`) roulent sur la piste.
- Enregistrement Strava : la trace GPS est un petit ovale au centre de Pontcharra (le vélodrome est placé à l'origine du repère) ; nom « Vélodrome de Pontcharra · N tours ».

### Navigation sur le réseau
- **Parcours imposé (depuis octobre 2026, à la demande de Cyril après essai sur smartphone)** : plus de sortie libre sur tout le réseau.
  - On choisit un parcours avant chaque sortie, séance ou exploration (`courseThen`) : parcours calculé, favori, parcours Pontcharra ou GPX.
  - `setCourse(routes, nom)` marque les routes du parcours (`ACT`), reconstruit la table spatiale (`RHM` = routes du parcours, `RHM_ALL` = tout le réseau), la grille grossière et l'emprise `BB`, puis relance `startWorld()` : **le monde 3D n'est construit qu'autour du parcours** (terrain fin, chaussées, carrefours, bâtiments, données). Mesuré : ~6 fois moins de triangles qu'avec tout le réseau.
  - `inW(e)` (route utilisable : tout le réseau tant qu'aucun parcours) et `inWorld(e)` (route construite en 3D : rien tant qu'aucun parcours n'est choisi). `withFullNet(f)` fait un calcul sur tout le réseau (choix du parcours, recalage GPX).
  - Aux carrefours, seules les routes du parcours sont proposées (`optionsAt`) : pas d'écran de choix ; cyclistes PNJ et promeneurs restent aussi sur le parcours. Les autres routes du réseau ne sont visibles que via la couche « autres routes » (non roulables).
  - Boucle : à l'arrivée, on enchaîne un nouveau tour. Aller simple : message d'arrivée, puis demi-tour automatique au bout.
  - GPX et tracé intégré `TRK` : **recalés sur le réseau** (`gpxToEdges` : un point tous les 50 m accroché à moins de 40 m, route retenue si 2 points de suite s'y accrochent, routes reliées par plus courts chemins). Le parcours Pontcharra donne 42,1 km pour un tracé de 42,4 km.
  - Le parcours est mémorisé (`v3gpxe`, `v3gpxn`) et rechargé au lancement ; sans parcours, aucun monde n'est construit (l'accueil reste disponible).
- Le cycliste circule de route en route. Sa position est un triplet (route, abscisse en mètres, sens).
- **Choix aux carrefours** (désactivé tant qu'un parcours est imposé, c'est-à-dire toujours ; code conservé) : 10 s avant un carrefour (et à moins de 500 m), l'écran se coupe en deux, ou en trois s'il y a trois routes.
  - Le côté par défaut est en surbrillance.
  - On choisit en touchant une moitié d'écran, ou avec les flèches ← → du clavier.
  - Chaque option affiche « vers Commune », le sens du virage et le nombre de passages de Cyril sur cette route.
- **Choix par défaut** : il maximise `log(1 + passages) + 1,5 × cos(angle du virage)`, donc il privilégie les routes habituelles et le tout droit.
- **Itinéraire GPX guidé** : charger un GPX ne remplace plus le monde. Aux carrefours, le choix par défaut suit le GPX.
  - Le bouton « Parcours Pontcharra (guidé) » utilise le tracé intégré `TRK`.
- **Carrefours arrondis** (3 routes ou plus) : chaque route s'arrête à `rb` m du nœud (≈ 9 m, jusqu'à 24 m si deux routes partent presque dans le même sens).
  - Chaussée du carrefour (`buildJunction`) : un ruban courbe par secteur entre deux routes voisines (Bézier cubique tangente aux deux routes, lignes de rive qui suivent la courbe) + un éventail central d'enrobé uni.
  - La même courbe arrondit la trajectoire du cycliste et des PNJ (`posSmooth`, `jPath`) : plus de virage à angle vif ni de saut de caméra. Le choix au carrefour est connu à l'avance (`riderNext`, `npcNext`).
- **Grande carte** : un clic sur la mini-carte (ou touche M, ou menu ☰) l'agrandit au centre, en transparence, orientée dans le sens de marche. Un nouveau clic (ou Échap) la ferme.
  - Au carrefour, une zone ronde « Carte » au centre de l'écran partagé l'ouvre. Chaque branche y est dessinée sur 1,5 km avec sa lettre (A, B, C…), la même que sur l'écran partagé.
- **Choix du parcours avant la sortie** (Rouler → Choisir un parcours) :
  - boucle (retour au départ) ou aller simple ; départ au centre de Pontcharra ou à la dernière position ; fourchettes de distance et de dénivelé positif ;
  - environ 30 parcours calculés (plus courts chemins de Dijkstra par 2 ou 3 points de passage, pénalité sur les routes déjà empruntées, moins de 15 % de routes répétées, doublons écartés) ;
  - classement par distance ou par dénivelé (croissant ou décroissant) ; pour chacun : carte, profil, km, D+, pente maxi sur 200 m, communes traversées ;
  - toute la carte d'un parcours se touche pour l'ouvrir ; bouton ▶ pour rouler directement ; bouton « Rouler ce parcours » collé en bas de l'écran ;
  - les zones qui défilent s'arrêtent au bas de la partie visible (`fitScr`, barre d'adresse de Chrome, paysage) ; en-tête de l'accueil réduit dans les sous-écrans sur les petits écrans ;
  - favoris (`GS.favr`) ; le parcours choisi est guidé aux carrefours en suivant la **liste exacte des routes** (`ROUTE.edges`), plus fiable que le guidage géométrique d'un GPX.
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
- **Arches lumineuses** (`lightArch`, `sprintArches`) : arche verte « Départ sprint » et arche rouge « Arrivée » au panneau de sortie, bandeaux LED animés (`LEDTX`).
- **Longueur du sprint** : 150 m par défaut, réglable de 100 à 500 m (Réglages → Préférences, `SET.sprLen`, `sprL()`), pour les villages d'au moins 200 m.
- **Avant le sprint** : annonce vocale à moins de 260 m, compte à rebours 3-2-1 plein écran, puis « GO ». Pendant : barre de distance restante, puissance et max (`#spz`, `sprintTick`).
- **Résultat plein écran** (`sprintEnd2`, `resultShow`) : puissance maximale en très grand, moyenne, W/kg, vitesse max, durée, écart au record. **Uniquement mes résultats** (plus de classement contre les PNJ).
- **Classement par pancarte** : mes 10 meilleurs passages avec la date (`GS.sprh`). Un sprint « réussi » (`GS.spr`) = pic ≥ FTP.
- **Côtes chronométrées** (`courseClimbs`, `komTick`, `komEnd`) : montées du parcours d'au moins 500 m, +35 m et 4 % de moyenne, nommées d'après la commune la plus proche ; arches « Côte » / « Sommet », chrono, W/kg, VAM, mes 10 meilleurs temps (`GS.komh`).
- **Pancartes de secours** : sans données de bâtiments, des pancartes « synthétiques » sont placées aux centres des communes.

### Préférences, ambiance et météo (octobre 2026)
- **Préférences** (Réglages → 🎛️, `prefRender`, clé `v3set`) : longueur du sprint, commentateur vocal, coach de séance, musique (aucune / calme / motivation) et volumes, moment de la journée, FC max, poids du cycliste, HUD.
- **Commentateur vocal** en français (`speak`, synthèse vocale du navigateur) : villages, sprints, côtes, records, récompenses. **Plus d'encouragements vocaux** (fin octobre 2026, demande de Cyril) : ni « Bonne route » au départ, ni « Bravo » en fin de séance, ni encouragements de mi-bloc, ni cris « Allez ! » des spectateurs (applaudissements et sifflets gardés). Le coach garde ses consignes (puissance, cadence, « Dix secondes. »).
- **Célébrations** (`celebrate`) : confettis, feux d'artifice et fanfare pour records, badges, niveaux.
- **Musique d'ambiance** générée (`MUS`, `musTick`), cloches de vaches et cloches d'église (`countrySounds`, `churchBell` à l'entrée des villages).
- **Météo retirée** (fin octobre 2026, à la demande de Cyril : la pluie faisait ramer l'appli) : plus de pluie, neige, brouillard ni météo réelle (Open-Meteo n'est plus appelé). Ciel normal.
  - **Vent retiré** aussi (fin octobre 2026) : air calme (`WX` à zéro, `wxHead` renvoie 0), plus d'option ni de champ « Vent » dans le HUD ; le home trainer ne reçoit que la vraie pente et l'aspiration.
- **Moment de la journée** (`todApply`) : heure réelle, jour, coucher de soleil, nuit. La nuit : phare renforcé (portée 120 m), feu arrière clignotant.
- **HUD personnalisable** (`HUDF`, `hudApply`) : champs au choix (vitesse, puissance, puissance 3 s, W/kg, NP, cadence, pente, cardio coloré par zone, distance, reste, altitude, D+, temps, kcal) et grand format.

### Piétons et promeneurs
- Leur nombre dépend de la qualité : 10 en Basse, 18 en Moyenne, 26 en Élevée, 34 en Ultra. Ils sont dessinés en instanciation (7 maillages pour tous).
- Les promeneurs marchent sur les trottoirs des villages et, plus rarement, sur le bas-côté.
- Des spectateurs sont présents à 3 pancartes de sortie sur 4.
- Ils **applaudissent, sautent et sifflent** (plus de cris « Allez ! » vocaux depuis fin octobre 2026) quand Cyril sprinte, roule à plus de 41 km/h, ou quand un PNJ sprinte près d'eux.

### Cycliste et animation
- Cycliste à squelette avec pédalage correct, réalisé par cinématique inverse.
- Positions aéro et roue libre, inclinaison dans les virages.
- **Danseuse sur variation de puissance**. Il se lève quand :
  - la moyenne des 3 dernières secondes dépasse la plus grande de ces deux valeurs : 1,28 × la moyenne des 15 s précédentes, ou cette moyenne + 70 W ;
  - et qu'elle dépasse aussi 0,8 × FTP.

  Il se rassoit dès que la puissance retombe sous la référence × 1,12 + 25 W, au plus tôt après 3 s, et au plus tard après 25 s.
- Il se met aussi en danseuse dans les fortes pentes et pendant les sprints.
- **Cadence de l'avatar = cadence du home trainer** (fin octobre 2026) : dès que le home trainer envoie le champ « cadence » FTMS (`CADOK`), l'avatar pédale exactement à cette cadence (`cadReal`, `CADR`, lissage rapide 0,15 s) et le HUD l'affiche ; il s'arrête de pédaler si la cadence tombe à 0. Si le home trainer n'envoie pas de cadence, cadence estimée (comme avant) et un message le signale une fois.
- **Avatar réaliste du joueur** (fin octobre 2026 ; les PNJ gardent leur tête simple) — section `tête réaliste du joueur` :
  - **visage sculpté** (`faceGeo`) : crâne et visage en coupes horizontales (`HSEC`, splines), relief du nez (arête, pointe, ailes), orbites, arcades, pommettes, lèvres, menton (`faceD`), ~13 000 sommets ; bouche réellement ouverte (cavité, dents) ;
  - **peau peinte point par point** (`paintFace`, 512 × 512, calculée par tranches de 5 ms dans la file `job`) : grain, rougeurs, cernes, lèvres, narines, sourcils, cheveux courts sous le casque, barbe ; relief fin (`bumpMap`) ; matériau « peau » (`MeshPhysicalMaterial`, reflet `sheen`) ;
  - **yeux** : globe, iris peint (`paintIris`), paupières avec cils ; **clignements**, petits mouvements du regard ;
  - **expressions** (`faceTick`, `FST`, « morph targets ») : bouche qui s'ouvre au rythme de la respiration au-dessus de ~62 % FTP, grimace d'effort (coins de la bouche, joues, sourcils) au-dessus de ~95 % FTP, **sueur** (visage qui brille) et **rougeur** qui montent avec l'effort ;
  - **oreilles** (pavillon, hélix, anthélix, conque, tragus), **cou** (pomme d'Adam), **casque façon Abus GameChanger** (24 octobre 2026, `helmetParts`, `hShell`) : coque lisse à 19 mm du crâne prolongée vers l'arrière puis tronquée (`hCut`, becquet), bandeau noir latéral, grande ouïe latérale en nid d'abeille (`hexEdge`), entrée d'air avant, sorties d'air arrière à lamelles, logos « ABUS / GAMECHANGER » en décalcomanies, sangles plates en Y, boucle ; décor peint par tranches (`job` « helmvents ») ; version aéro (chrono) gardée, **lunettes enveloppantes** (`glassesParts` : verre teinté transparent, monture, branches) ;
  - **mains** : doigts qui entourent les cocottes (`handFingers`, gants courts : première phalange gantée) ; **corps** plus fin pour le joueur (`buildBody(true)`) : quadriceps, vaste interne, rotule, mollets à deux chefs, crête du tibia, biceps, triceps, avant-bras, omoplates, pectoraux, trapèzes en pente ;
  - **Vestiaire → Visage** : teint (4), cheveux (6), barbe (rasé, 3 jours, courte), yeux (5), lunettes (sur le nez, relevées sur le casque, sans) — clés `fsk`, `fhr`, `fbd`, `fey`, `gls` de `v3kit` (`faceApply`) ;
  - caméra **« Gros plan visage »** sur route (vue n° 7) et au vélodrome.

### Suivi, récompenses et vestiaire (inspirés de Zwift, MyWhoosh et Rouvy)
- **kcal** : 1 kJ de travail ≈ 1 kcal dépensée (rendement brut 24 %, `KCAL_J = 0,24 × 4 184`). Affichées dans le HUD.
- **Récompenses pendant les sorties et les séances** : tous les 5 km et toutes les 100 kcal (bandeau animé + points).
- **Badges** : km, kcal, D+, séances, sprints gagnés cumulés, et records d'une seule sortie. **Niveaux** : XP = points gagnés (niveau L à `250·L·(L−1)` XP).
- **Objectifs de la semaine** (km et kcal, réglables), graphique des 8 dernières semaines, historique des sorties (`GS.hist`), suivi du poids pour le régime (`GS.wlog`).
- **« Ride On »** : un PNJ qu'on double (ou qui nous double) envoie parfois un « Ride On » (+5 points).
- Récapitulatif (km, temps, kcal, récompenses) en fin de sortie, avant la question Strava.
- **Vestiaire** : 11 maillots (styles dégradé, bandes, écharpe, néon, pois, tricolore, arc-en-ciel…, certains à débloquer), couleurs du casque, des chaussures, chaussettes, gants, verres des lunettes ; cadre (couleur, finition brillante/mate/métallisée), accent, jantes carbone 45/60 mm ou alu, pneus à flancs beiges, guidoline, selle. Compteur GPS du cintre qui affiche vitesse, puissance et cardio.

### Analyse et progression (octobre 2026)
- **Analyse de fin de sortie** (`analyzeRide`, `anaOpen`, écran `s-ana`) : NP, IF, TSS, max, FC moyenne/max, cadence, kcal ; graphiques puissance, cardio, cadence, altitude ; temps par zone de puissance (Coggan) et par zone cardio (% de FC max : 60/70/80/90). Accessible depuis la fenêtre Strava et depuis Mon suivi (dernière sortie gardée dans `v3last`).
- **Courbe de puissance et records** (`PCD`, `prCheck`, `GS.pc`) : 5 s à 1 h.
- **Détection automatique de FTP** : si 95 % du meilleur 20 min dépasse la FTP de plus de 3 W, l'appli propose la nouvelle valeur (rien n'est changé sans accord).
- **Forme CTL/ATL/TSB** (`fitnessSeries`) : moyennes exponentielles du TSS sur 42 et 7 jours, graphique 90 jours.
- **Série de jours** (`streakTick`, `GS.stk`) : sortie d'au moins 10 min par jour, points bonus, badges 3/7/14/30 jours.
- **Carte souvenir** (`postcard`) : image 1080×1350 (trace, chiffres clés) à télécharger ou partager.
- **Plans d'entraînement** (`TPLANS`, `tplanStart`, `tplanDone`, écran `s-tpl`, `GS.tplan`) : FTP 8 semaines, fond 6, perte de poids Dukan 8, sprint 6, remise en forme 4 ; semaine allégée toutes les 4 semaines ; la séance du jour est créée par `genWorkout`. Une séance compte si 80 % est fait.
- **Équipement à débloquer** (`OLOCK`, vestiaire) : casque aéro (niveau 8), jantes carbone 80 mm (2 000 km), roue lenticulaire arrière (niveau 15), cadre or (25 000 kcal), cadre chrome (15 000 m D+), verres irisés (7 jours d'affilée).

### Matériel et entraînement
- **Home trainer FTMS** en Web Bluetooth (`setupTrainer`).
  - Réponses du point de contrôle lues (`onCP`) : si le contrôle est perdu (code 5), il est redemandé et la dernière consigne renvoyée ; commande refusée signalée.
  - Plage de puissance lue (0x2AD8, `PRNG`) : la consigne ERG y est bornée. Consigne ERG renvoyée au moins toutes les 10 s.
  - Cardio transmis par le home trainer utilisé si aucune ceinture n'est connectée (`HRBELT`). Reconnexion automatique (3 essais).
  - **Coach de séance** (`wkCoach`) : annonce de chaque bloc (puissance, cadence conseillée), mi-bloc, 10 dernières secondes, rappels « augmente / baisse la puissance » et « pédale plus vite / plus en force » toutes les 25 s si l'écart est net. Ligne « toi X W · Y tr/min · ♥ » sous la consigne (`#wi2`).
  - Service 0x1826 ; Indoor Bike Data 0x2AD2 ; Control Point 0x2AD9.
  - Filtres de nom : CYCPLUS, Cycplus, T2.
  - Simulation de pente : opcode 0x11, pente en 0,01 %, Crr 0,0040, Cw 0,19 kg/m réduit par l'aspiration.
  - Mode ERG : opcode 0x05.
- **Ceinture cardio** : service 0x180D, caractéristique 0x2A37.
- **Séances ERG** : endurance, sweet spot, seuil, VO2max, force, sprints, avec zones Coggan selon la FTP saisie et intensité réglable de 50 à 150 %.
- **Test FTP en rampe** : la FTP retenue est 75 % de la meilleure minute.
- **Créateur de séance automatique** (`genWorkout`) : objectif (endurance, tempo, sweet spot, seuil, VO2max, 30/30, sprints, force, vélocité, pyramide, récupération, **régime Dukan**), durée de 20 à 180 min, niveau. Échauffement, blocs et retour au calme se calculent seuls ; variante aléatoire, séance gardée dans la liste (★).
  - **Régime Dukan** : zone « brûle-graisses » (63–72 % de la FTP selon la phase), relances d'1 min selon la phase (Attaque : 45 min maximum, sans relance). Affiche les kcal estimées et l'équivalent en graisse (7 700 kcal/kg).
  - En séance : consigne de chaque bloc, annonce du bloc suivant, bips 3-2-1.
  - **Combiner deux travaux** (« Combiner avec ») : deux parties égales séparées de 3 min de récupération ; avec le régime Dukan, le second travail occupe ~34 % du temps au milieu de la base brûle-graisses, à intensité réduite de 8 % en phase d'attaque et de 4 % en croisière.
  - **Raccourcir une séance en cours** (bouton ⏱) : on réduit d'abord l'endurance, puis on retire des répétitions entières par la fin ; les récupérations entre efforts et l'intensité ne changent pas ; retour au calme ramené à 2 min au moins.
- **Accueil simplifié** : 4 grands boutons — **Rouler** (rouler sur le dernier parcours, choisir un parcours, parcours Pontcharra, GPX, exploration ; plus de sortie libre), **S'entraîner**, **Mon suivi**, **Réglages** (matériel, **Strava** avec état de connexion et sorties envoyées `GS.strv`, vestiaire, garage, graphismes).
- **Diagnostic des blocages** (Réglages → 🩺, `diagStart`, `diagReport`, `diagShow`, clé `v3diag` = actif, reste actif après redémarrage) : pendant une sortie, mesure image par image le temps de chaque tâche (fonctions de la boucle enveloppées, tâches `job` par préfixe de clé, `ren.render`, envois de textures `texImage2D`/`texSubImage2D`, nouveaux shaders, lecture des données entre les images, `JSON.parse`) et, si Chrome le permet, le temps réel de la carte graphique (`EXT_disjoint_timer_query_webgl2`). Une image > 50 ms est attribuée à sa plus grosse cause ; le reste du temps hors appli est classé « Navigateur / carte graphique ». Bandeau 🩺 en haut (images/s, blocages > 100 ms, pire), rapport texte à copier ou enregistrer (☰ → Rapport diagnostic, ou bouton dans la fenêtre de fin de sortie).
- **Encadré d'état des chargements masqué** (`#ld`, demande de Cyril) ; les messages importants passent par des fenêtres.
- **Séances : la pente ne joue pas** (`gP` dans `loop`) : sur un parcours, résistance en ERG (comme avant), et désormais vitesse, danseuse et posture calculées comme sur le plat.
- **Vérification au lancement** (`netCheck`) : si Internet ou le Bluetooth sont bloqués (fichier ouvert dans un aperçu, par exemple l'appli Claude ou un gestionnaire de fichiers), une fenêtre explique de télécharger le fichier et de l'ouvrir dans Chrome ; le panneau de chargement l'indique aussi.
- **Plein écran** et **écran maintenu allumé** pendant les sorties (API Wake Lock), utiles sur tablette.
- **Garage** : points, équipement améliorable, historique FTP, records de sprint.
- **Pièce d'accueil en 3D** : vélo sur home trainer, bureau avec écran animé, posters.
- **Sons de nature** générés par WebAudio : vent, feuilles, rivière, grillons, oiseaux, roue libre.

### Strava
- Enregistrement à 1 Hz pendant les sorties et les séances : position GPS réelle, altitude, distance, vitesse, puissance, fréquence cardiaque, cadence.
- En quittant la sortie (☰ → « 🏁 Terminer et enregistrer », ou ✕ en séance), une **fenêtre demande** s'il faut l'envoyer sur Strava. Rien ne part sans validation.
  - La sortie est gardée dès **30 s et 100 m**, même si le parcours n'est pas fini.
  - **Sauvegarde automatique** toutes les 30 s et quand l'appli passe en arrière-plan (`recSave`, clé `v3rec`). Au lancement suivant, une sortie interrompue (appli fermée, écran éteint…) est proposée à l'envoi sur Strava et ajoutée à l'historique (`recOfferSaved`).
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
- Livrable : **un seul fichier HTML** d'environ 1,3 Mo et 3 430 lignes, sans dépendance réseau pour le code.
- Le fichier `Index.html` du dépôt (avec un I majuscule) est ce fichier (livré à l'origine sous le nom `velo-3d-pontcharra.html`).
- **three.js r150** est **inclus en entier dans le fichier** (version minifiée) : pas de CDN, pas de modules ES, pas d'outil de build.
- JavaScript « classique » en mode strict, sans framework. Tout le code applicatif est dans un seul `<script>`.

### Plan du fichier (dans l'ordre)
Les numéros de ligne sont approximatifs. Repère-toi avec les en-têtes `// ====================== … ======================`.

| Lignes ≈ | Contenu |
|---|---|
| 1–155 | `<head>` : CSS de l'interface, puis le HTML (HUD, menu ☰, accueil, écrans, fenêtre `#res`, grande carte `#bmap`, bandeau `#rw`) |
| ≈156 | `<script>` **three.js r150 minifié**. **NE PAS LIRE NI MODIFIER** : lignes énormes. |
| ≈157–3820 | `<script>` de l'application, sections ci-dessous |
| ≈161 | `const TRK="lat,lon,alt;…"` : parcours Pontcharra par défaut (une seule ligne très longue) |
| ≈162 | `const NETD={"E":[…]}` : **réseau routier** (une ligne d'environ 285 Ko, ne pas l'afficher en entier) |
| ≈163 | Base : utilitaires, détection du GPU (`GPU`, `WEAK_GPU`), préréglages de qualité `QPRE`, file de tâches `job()`/`runJobs()`, cache réseau, classe `GB` |
| ≈277 | Rendu, ciel, lumière, astronomie (`skyUpdate`, `FORCE_SUN` pour les tests) |
| ≈396 | Textures procédurales |
| ≈549 | **Réseau routier** : `initNetwork`, `buildRoadHash` (`RHM`/`RHM_ALL`, `inW`, `inWorld`, `withFullNet`), `roadQ`, `posAt`, **carrefours arrondis** (`jcOf`, `bez`, `jCtl`, `jPath`, `posSmooth`, `buildJunction`), `finishTrack`, `buildCoarse`, relief `ground`/`hT` |
| ≈739 | Terrain : tuiles proches, moyennes et lointaines, horizon, couloir le long des routes, mosaïques photo, photo proche `CLIP` |
| ≈964 | Données cartographiques : WFS IGN, Overpass, **autres routes** (`addORoad`, `parseBDTopoRoute`, `osmRoad`, `terrH`, `buildOCell`, `oroadUpdate`, `oroadsPath`), procédural ; chargement dynamique (`dataLoaderTick`) |
| ≈1183 | Route : chaussée par tronçon (rognée aux carrefours), chaussées de carrefour (`ROAD.jn`), ponts, mobilier |
| ≈1303 | Végétation (`FOREST_K`) et herbe |
| ≈1513 | Bâtiments |
| ≈1738 | Eau |
| ≈1765 | Agglomérations : détection par route, noms, panneaux, arches (`villagesTick`) |
| ≈1856 | Cycliste et vélo : `jerseyCanvas` (styles de maillot), matériaux du joueur `KM`, finesse `GSEG`, corps `buildBody(hi)` (+ `legBump`, `armBump`), **tête réaliste du joueur** (`FACEO`, `HSEC`, `faceGeo`, `paintFace`, `earGroup`, `helmetParts`, `glassesParts`, `handFingers`, `faceBuild`, `faceApply`, `faceTick`), `makeRiderInst`, `riderAnim`, compteur `compTick` |
| ≈2188 | Application : état global, FTMS, cardio, séances (`WKALL`, `startW`, `shortenWk`), interface (`nav`, `prof`) |
| ≈2312 | Jeu : sprints, points, garage |
| ≈2359 | **Suivi et récompenses** (`SES`, `sesTick`, `reward`, `BADGES`, `progTick`, `suiviRender`), **créateur de séances** (`GOALS`, `genWorkout` avec `g2`, `wkStats`), **vestiaire** (`KITS`, `KIT`, `applyKit`), **grande carte** (`BIGM`, `drawBig`), veille et plein écran |
| ≈2615 | **Choix du parcours** (`PLAN`, `edgeClimb`, `dijk`, `planRun`, `planRender`, `planDetail`, `planRide`), **parcours imposé** (`setCourse`, `gpxToEdges`, `courseThen`), **accueil à 4 boutons** (`roulRender`, `regRender`) ; puis sons et pièce d'accueil |
| ≈2829 | **Navigation** : `RID`, `advance` (mémorise `st.prev`), `optionsAt`, `defaultIdx`, `riderNext`, itinéraire `setRoute`/`routeIdx` (suit `ROUTE.edges` s'il existe), choix au carrefour `jxTick` |
| ≈2920 | **Cyclistes PNJ** : `npcTick`, `npcChoose`/`npcNext`, aspiration `draftOf`, sprints `exitAhead`, « Ride On » |
| ≈3030 | **Piétons** : `pedTick`, applaudissements, sifflets, voix |
| ≈2846 | **Préférences et ambiance** (`SET`, `speak`, `resultShow`, `celebrate`, musique, `WX` (neutre), `todApply`), **arches, sprints et côtes** (`lightArch`, `sprintTick`, `sprintEnd2`, `courseClimbs`, `komTick`), **analyse** (`analyzeRide`, `prCheck`, `streakTick`, `fitnessSeries`, `anaOpen`, `postcard`), **plans** (`TPLANS`), **coach et HUD** (`wkCoach`, `HUDF`, `prefRender`, `suiviExtra`) |
| ≈3300 | **Vélodrome** (`VEL`, `velBuildCurve`, `velBuild`, `velStart`/`velStop`, `velFrame`, `velShow`, `velCam`, `velHud`, `velMap`) — juste avant les sons de la nature |
| ≈3096 | **Strava** : enregistreur `REC`, `fitEncode`, OAuth, envoi, fenêtre `offerStrava`, `stravaPanel` |
| ≈3179 | Monde et boucle principale : `startWorld`, caméras, `loop()`, mini-carte, menu ☰, démarrage |

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
- **Anti-blocages (fin octobre 2026)**, après les « blocages » signalés par Cyril (absents des premières versions) :
  - **données de carte traitées par tranches de 6 ms** (`sliced`) : bâtiments, autres routes (IGN `troncon_de_route` ou OSM) et eau ne sont plus traités d'un seul bloc à l'arrivée d'une zone de 2 km (mesuré avec des données IGN simulées : blocages de 200 à 400 ms sur le banc, soit plusieurs secondes sur tablette). Abandon propre (`STALE`) si le parcours change pendant le chargement ;
  - **mini-carte** : le fond (autres routes, réseau, villages) est dessiné une fois dans une image en mémoire (`mapBase`, `MC`, `MAPV`), redessiné seulement après ~2 km ou de nouvelles données (au plus toutes les 5 s). Avant, des milliers de traits redessinés toutes les 0,4 s saturaient l'affichage (46 images en 60 s sur le banc avec données, 1 700 à 1 800 après) ;
  - **ponts** (`computeBridges`) : masque d'eau seul et routes du parcours seulement (avant : masques complets bâtiments + végétation + routes sur tout le réseau) ;
  - **forêts** : plus téléchargées ni traitées quand `NOVEG` ;
  - **bâtiments** (`buildTile`) : paquets d'environ 120 bâtiments et main rendue toutes les 4 ms (plus de tableaux géants) ;
  - **résolution adaptative** plus calme (`DPRS`) : baisse après 5 s de lenteur, remontée après 15 s de fluidité, jamais deux changements en moins de 10 s (chaque changement réalloue l'image) ;
  - **photos aériennes** : mosaïques renvoyées à la carte graphique au plus toutes les 2,5 s (0,8 s avant), photo proche toutes les 1,2 s (0,5 s avant), toiles non « lues » (`willReadFrequently` désactivé quand `NOVEG`) pour un envoi plus rapide ;
  - **après le 1er rapport de diagnostic (smartphone Mali-G615, 90 images/s mais blocages de 0,7 à 11 s)** : la cause était le terrain lointain, moyen et l'horizon, reconstruits en entier à chaque arrivée d'eau (rivières) et calculant pour chaque point le creusement de l'eau. Corrigé :
    - couches éloignées (`buildMidTile`, `buildFar`, `buildRing`) calculées avec `groundFar` (relief seul, sans eau ni micro-relief) et découpées en générateurs (main rendue toutes les 4 ms) ;
    - versions séparées : `TER.ver` (proche, couloirs) et `TER.fver` (moyen, lointain, horizon) ; `terrInvalidate(near, far)` : l'arrivée de l'eau ne reconstruit que le proche, l'arrivée du relief z10/z8 que l'éloigné ;
    - `rasterRings` : ne parcourt que les arêtes qui traversent la bande de la cellule (même résultat ; rivière de 40 000 points sur 20 cellules : 430 ms → 20 ms) ;
    - diagnostic : temps en arrière-plan non compté, plus de fonctions suivies (relief des routes, zones d'eau, éclairage du ciel…).
  - reste connu : quelques pauses du ramasse-miettes JavaScript (≈ 100–200 ms sur le banc, quelques fois par minute) pendant la construction des bâtiments, et la construction d'une tuile de terrain proche (jusqu'à ~90 ms sur le banc).

### Sources de données externes (dans le navigateur)
- Altitude : `https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png`
- Photos : `https://data.geopf.fr/wmts?…ORTHOIMAGERY.ORTHOPHOTOS…`, avec ArcGIS World Imagery en secours.
- Données : `https://data.geopf.fr/wfs/ows`. Couches BDTOPO_V3 : `batiment`, `zone_de_vegetation`, `surface_hydrographique`, `zone_d_habitation`.
- OSM : Overpass (3 miroirs). Communes : `https://geo.api.gouv.fr/communes`.

### Stockage local (localStorage)
- `v3q2` : qualité graphique (`tab`, `low`, `med`, `high`, `ultra`). L'ancienne clé `v3q` n'est plus lue, pour que la tablette reparte en « Tablette ».
- `v3kit` : choix du vestiaire (maillot, couleurs, jantes, pneus…, et visage : `fsk` teint, `fhr` cheveux, `fbd` barbe, `fey` yeux, `gls` lunettes).
- `ftp`, `kg` : FTP et poids.
- `v3gpxe`, `v3gpxn` : **parcours imposé** (liste des routes `[e, sens]` et nom). `v3gpx` (ancien GPX en points) n'est plus écrit : il est seulement relu une fois et recalé.
- `v3rec` : sortie en cours, sauvegardée toutes les 30 s (effacée à la fin normale de la sortie).
- `vg3` : jeu : points, records, équipement, et désormais `xp`, `kcal`, `asc` (D+), `spr` (sprints gagnés), `hist` (historique, 400 max), `bdg` (badges), `goal`/`wgoal` (objectifs de la semaine), `wlog` (poids), `myw` (séances gardées), `favr` (parcours favoris, en liste de routes `[e, sens]`), `strv` (sorties envoyées sur Strava), `sprh` (10 meilleurs sprints par pancarte), `komh` (10 meilleurs temps par côte), `pc` (records de puissance), `stk` (série de jours), `tplan` (plan en cours).
- `v3set` : préférences (sprint, voix, coach, musique, moment de la journée, FC max, HUD).
- `v3last` : dernière sortie (points réduits à 3 000 au plus) pour l'analyse.
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
8. **PNJ lourds** : environ 81 000 sommets chacun (géométrie fusionnée non partagée). En qualité Basse et Tablette, leurs maillages sont construits plus grossiers (`npcSeg`, via `GSEG`), mais la géométrie n'est toujours pas partagée.
9. **Itinéraire GPX** : s'il emprunte une route absente du réseau, le HUD affiche « hors itinéraire » et les carrefours reviennent au choix habituel. Il n'y a pas d'autre alerte.
10. **Cul-de-sac** : le demi-tour automatique se fait sans message.
11. **Changements par rapport à la version précédente** :
    - en mode Exploration, le bouton ◀ fait maintenant demi-tour ;
    - les anciens records de sprint ne correspondent plus aux nouvelles clés (`NOM@route±`).
12. **Voix du commentateur** : elle dépend d'une voix française installée dans le navigateur.
13. **Nouveautés d'octobre 2026 non testées sur le vrai matériel** :
    - la qualité « Tablette » n'a tourné qu'en rendu logiciel ; la fluidité réelle sur la Redmi Pad SE est inconnue ;
    - les kcal sont une estimation (rendement fixe de 24 %) ; l'équivalent graisse (7 700 kcal/kg) est un ordre de grandeur, pas une mesure ;
    - avec 90 % d'arbres en moins, les forêts de la photo aérienne paraissent clairsemées ;
    - carrefours : testés sur les 550 nœuds (aucune géométrie invalide) mais pas regardés un par un ; un carrefour aux branches très serrées peut montrer un petit recouvrement de chaussée ;
    - aux culs-de-sac, le demi-tour automatique reste brusque (pas de courbe) ;
    - **autres routes** : la couche IGN `troncon_de_route` et ses attributs n'ont pas pu être vérifiés (serveurs IGN et OSM injoignables depuis l'environnement de test). Le rendu a été testé avec des routes injectées. Si la couche IGN ne répond pas, l'appli bascule sur OpenStreetMap ;
    - **parcours imposé** : chaque changement de parcours relance la construction du monde (quelques secondes de chargement, données en cache) ;
    - **choix du parcours** : D+ et pentes viennent de l'altitude du réseau ; ils ne sont justes qu'une fois le vrai relief SRTM chargé. Avec le relief de secours, certaines pentes atteignent le plafond de 25 %. Les favoris sont enregistrés par numéro de route : ils deviendront faux si le réseau est modifié (corrections de la carte 2D).
14. **Nouveautés de fin octobre 2026 non testées sur le vrai matériel** :
    - réponses FTMS (`onCP`), plage de puissance (0x2AD8) et cardio transmis par le home trainer : testés avec des données simulées seulement ;
    - voix : dépend d'une voix française installée ; musique et sons de cloche générés, jamais écoutés ;
    - côtes : détectées sur l'altitude du réseau, donc justes seulement avec le vrai relief.
    - **avatar réaliste, tribunes animées, sons du vélodrome** : vérifiés en rendu logiciel seulement (captures) ; sons générés jamais écoutés. Coût mesuré sur le banc : peinture du visage ≈ 150–200 ms (découpée en tranches de 5 ms), casque ≈ 30 ms, corps détaillé ≈ 30 ms au lancement ; sur tablette, compter 2 à 4 fois plus. Si la tablette rame, le visage du joueur peut être allégé (moins de segments dans `faceGeo`) ;
    - **cadence réelle** : le champ cadence FTMS de la CYCPLUS T2 n'a pas été vérifié ; s'il manque, l'appli le dit et garde la cadence estimée ;
    - **24 octobre 2026** : virages adoucis et pente ≤ 15 % vérifiés par calcul ; végétation légère, clôtures, animaux et leurs sons, casque GameChanger : vus en rendu logiciel seulement ; sons des animaux, des enfants et des cloches jamais écoutés ; la couche IGN des écoles (`zone_d_activite_ou_d_interet`, valeurs « enseignement ») n'a pas pu être vérifiée (IGN injoignable depuis l'environnement de test) ;
    - **plein écran automatique** au vélodrome : Chrome l'accepte seulement après un toucher (c'est le cas : bouton du menu) ; sur certains Android, la barre de navigation peut réapparaître.
15. **Ouverture sur Android** : dans l'aperçu de l'appli Claude, Internet et le Bluetooth sont bloqués (constaté par Cyril : relief et photos simulés). Ouverture directe dans Chrome (Fichiers → Ouvrir avec → Chrome) non vérifiée par nos soins : si Chrome bloque encore, il faudra héberger le fichier en HTTPS (attention à la confidentialité).
16. **Confidentialité** : le réseau couvre les rues autour du domicile de Cyril. **Le dépôt doit rester privé.**

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
5. **Régler la qualité « Tablette »** après un essai sur la Redmi Pad SE (images par seconde, distances, nombre de PNJ).
6. **Bâtiments plus ressemblants** (demandé, reporté par Cyril) : couleur des toits relevée sur la photo aérienne, style local dauphinois/savoyard, bâtiments marquants sur mesure à partir de photos.
7. **Petits plus envisagés, non demandés à ce stade** : vraies sorties Strava proposées comme parcours (nécessite le droit `activity:read`), messages au cul-de-sac et quand on sort de l'itinéraire, mémorisation de la dernière position, groupes de PNJ qui roulent ensemble.

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
- **Modifier `Index.html` par remplacements ciblés.** Ne jamais lire ni réécrire en entier les lignes de three.js, `TRK` et `NETD`. Utiliser la recherche (grep) sur les en-têtes de section.

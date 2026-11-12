<p align="center">
  <img src="web/assets/logo.svg" alt="Logo Carnet Entretien" width="128">
  <h1 align="center">Carnet Entretien</h1>
  <p align="center">Application de bureau pour gérer l'entretien de vos véhicules</p>
</p>

### Site web : https://paulmunoz14.github.io/CarnetEntretien/

## Description

**Carnet Entretien** est une application JavaFX qui permet de :

- Gérer plusieurs véhicules (marque, modèle, kilométrage)
- Tenir un historique kilométrique daté et suivre l'évolution du compteur
- Estimer automatiquement le kilométrage actuel en fonction de votre usage annuel
- Anticiper les échéances d'entretien (vidange, courroie de distribution, filtres, etc.)
- Planifier les entretiens à venir et les retrouver dans un calendrier mensuel
- Suivre le contrôle technique : résultat, date de la prochaine visite et alerte à 2 mois
- Stocker les données localement dans une base SQLite (aucun serveur requis)
- Archiver les factures et suivre les dépenses d'entretien

> **Statut du projet** : En développement actif

## Objectif

Ce projet propose un outil pour toute personne souhaitant suivre sérieusement l'entretien de son véhicule.

## Fonctionnalités

### Implémenté

- [x] Architecture Maven
- [x] Base de données SQLite locale
- [x] Ajout et affichage des véhicules
- [x] Persistance des données
- [x] Gestion des entretiens réalisés (historique)
- [x] Formulaire de modification/suppression de véhicule
- [x] Gestion des préconisations constructeur (intervalles km/mois)
- [x] Tableau de bord avec jauges d'usure (ProgressIndicator + CSS)
- [x] Alertes visuelles (À prévoir / Urgent)
- [x] Algorithme de prédiction des échéances (extrapolation du kilométrage)
- [x] Export des données en CSV
- [x] Ajout d'un véhicule avec sa plaque d'immatriculation
- [x] Affichage de la voiture si saisie par plaque d'immatriculation
- [x] Photo personnelle du véhicule : importée depuis l'ordinateur et copiée dans l'application
- [x] Export en installeur autonome (via `jpackage`)
- [x] Calendrier des entretiens : grille mensuelle, échéances calculées, historique et planifications
- [x] Planification d'un entretien (date, véhicule, remarque) et marquage « Fait »
- [x] Suivi du contrôle technique : historique, échéance (2 ans ou contre-visite) et alerte à 2 mois
- [x] Planification d'un contrôle technique dans le calendrier, avec marquage « Fait »
- [x] Historique kilométrique : relevés datés, évolution entre deux relevés et mise à jour du compteur
- [x] Export CSV incluant l'historique kilométrique

### En cours / À venir

- [ ] Gestion des factures PDF
- [ ] Suivi du carburant
- [ ] Statistiques financières
- [ ] Graphiques de consommation
- [ ] Comparaison entre véhicules
- [ ] Notifications Windows
- [ ] Sauvegardes automatiques
- [ ] Import/export JSON
- [ ] Journal d'activité
- [ ] API REST
- [ ] Application mobile complémentaire
- [ ] Synchronisation entre appareils
- [ ] Comptes utilisateurs
- [ ] Retirer Herobrine

## Fiche technique

| Composant | Technologie |
|-----------|-------------|
| Langage | Java 21 |
| Interface | JavaFX 21 |
| Build | Maven |
| Base de données | SQLite (via `sqlite-jdbc`) |
| Icônes | Ikonli |
| UI | ControlsFX |
| Tests | JUnit 5 |

## Où sont stockées les données ?

Base SQLite, dossier `images/` (photos importées) et `config.properties` (clé API) sont rangés ensemble :

1. dans le dossier indiqué par `-Dcarnet.data.dir=<dossier>` s'il est fourni ;
2. sinon dans le dossier courant, s'il contient déjà `carnet_entretien.db` ou `config.properties` (développement) ;
3. sinon dans `<dossier personnel>/CarnetEntretien` (cas d'un exécutable installé).

## Clé API (recherche par plaque)

La recherche par plaque utilise [apiplaqueimmatriculation.com](https://apiplaqueimmatriculation.com/obtenir_une_cle_api/).
L'utilisateur colle sa clé via le bouton « 🔑 Clé API » (une fenêtre d'explications s'ouvre aussi au premier lancement).
Sans clé personnelle, l'application utilise la clé de démonstration de la documentation, très limitée.
Ordre de lecture : variable d'environnement `PLAQUE_API_TOKEN`, puis `plaque.api.token` dans `config.properties`, puis la clé de démonstration.

## Lancer les tests

```
./mvnw test
```

Les tests de base de données et de fichiers travaillent dans un dossier temporaire : ils ne touchent pas à vos données.

## Prérequis

- **JDK 21 ou supérieur** (testé avec OpenJDK 21)
- **Maven** (ou utiliser le `mvnw` inclus)
- **IntelliJ IDEA** (recommandé) ou tout autre IDE Java

## Installation et lancement

### 1. Cloner le dépôt

```bash
git clone https://github.com/PaulMUNOZ14/CarnetEntretien.git
cd CarnetEntretien
```

### 2. Lancer l'application

Avec Maven :

```bash
./mvnw javafx:run
```

Ou dans IntelliJ : clic droit sur `Launcher.java` → **Run**

> La base de données `carnet_entretien.db` sera automatiquement créée à la racine du projet au premier lancement.

## Arborescence du projet

```
CarnetEntretien/
├── README.md
├── pom.xml
├── mvnw.cmd
├── mvnw
├── .gitignore
├── .mvn/wrapper/                                       # Maven Wrapper (maven-wrapper.jar, maven-wrapper.properties)
├── carnet_entretien.db                                 # Base SQLite (générée automatiquement au lancement)
├── config.properties                                   # Clé API de l'utilisateur (générée via le bouton « Clé API »)
├── images/                                             # Photos de véhicules importées par l'utilisateur (générées automatiquement)
│
├── .github/workflows/ 
│   └── deploy-pages.yml                                # deployements du site web
│
├── web/                                                # Site web de présentation
│   ├── index.html                                      # Page d'accueil
│   ├── assets/                                         # Ressources du site
│   │   ├── apple-touch-icon.png                        # icone apple-touch
│   │   ├── favicon.ico                                 # favicon
│   │   └── logo.svg                                    # logo vectoriel   
│   ├── css/                                            # Fichiers CSS du site
│   │   ├── style.css
│   │   └── style.mini.css                              # Fichier css minifié
│   ├── js/
│   │   ├── script.js
│   │   └── script.mini.js                              # Fichier js minifié
│   └── pages/
│       ├── confidentialite.html
│       ├── feuille-de-route.html
│       ├── fonctionnalites.html
│       ├── mentions-legales.html
│       └── telecharger.html
│
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java                        # Déclaration du module Java
    │   │   └── com/carnetentretien/carnetentretien/
    │   │       ├── HelloApplication.java               # Point d'entrée JavaFX
    │   │       ├── Launcher.java                       # Lanceur de l'application
    │   │       ├── model/                              # Objets métier
    │   │       │   ├── ControleTechnique.java          # Objet ControleTechnique (avec le résultat)
    │   │       │   ├── ControleTechniquePlanifie.java  # Objet Contrôle technique planifié
    │   │       │   ├── Entretien.java                  # Objet Entretien
    │   │       │   ├── EntretienPlanifie.java          # Objet Entretien planifié
    │   │       │   ├── Preconisation.java              # Objet Preconisation
    │   │       │   ├── ReleveKilometrage.java          # Objet Relevé kilométrique
    │   │       │   └── Vehicule.java                   # Objet Vehicule
    │   │       ├── db/                                 # Accès base de données
    │   │       │   ├── ControleTechniqueDB.java        # Accès aux contrôles techniques
    │   │       │   ├── ControleTechniquePlanifieDB.java # Accès aux contrôles techniques planifiés
    │   │       │   ├── DatabaseManager.java            # Connexion, création et migration de la base
    │   │       │   ├── EntretienDB.java                # Accès aux entretiens
    │   │       │   ├── EntretienPlanifieDB.java        # Accès aux entretiens planifiés
    │   │       │   ├── PreconisationDB.java            # Accès aux préconisations
    │   │       │   ├── ReleveKilometrageDB.java        # Accès aux relevés kilométriques
    │   │       │   └── VehiculeDB.java                 # Accès aux véhicules
    │   │       ├── controller/                         # Contrôleurs FXML
    │   │       │   ├── DashboardController.java        # Fenêtre principale : navigation, toast, taille
    │   │       │   ├── DashboardContext.java           # Services communs fournis aux vues
    │   │       │   ├── ui/                             # Outils partagés par les contrôleurs
    │   │       │   │   ├── Composants.java             # Vignettes, plaques, boutons, chargement d'images
    │   │       │   │   ├── DialogueSupport.java        # Habillage et validation des fenêtres de saisie
    │   │       │   │   ├── EntretienUi.java            # Niveaux d'usure et badges colorés
    │   │       │   │   └── Formats.java                # Nombres, montants et dates
    │   │       │   ├── vue/                            # Contrôleurs des écrans
    │   │       │   │   ├── AccueilController.java      # Écran d'accueil (liste des véhicules)
    │   │       │   │   ├── CalendrierController.java   # Calendrier mensuel (grille, liste, actions)
    │   │       │   │   └── VehiculeController.java     # Écran d'un véhicule (jauges, contrôle technique, historique)
    │   │       │   └── dialogue/                       # Contrôleurs des fenêtres de saisie
    │   │       │       ├── DialogueCleApiController.java
    │   │       │       ├── DialogueControleTechniqueController.java
    │   │       │       ├── DialogueControleTechniquePlanificationController.java
    │   │       │       ├── DialogueEntretienController.java
    │   │       │       ├── DialogueKilometrageController.java
    │   │       │       ├── DialoguePlanificationController.java
    │   │       │       ├── DialogueRegleController.java
    │   │       │       ├── DialogueReglagesController.java
    │   │       │       └── DialogueVehiculeController.java
    │   │       └── service/                            # Logique métier
    │   │           ├── AppPaths.java                   # Emplacement des données (base, images, config)
    │   │           ├── CalculateurEntretien.java       # Détermine quand prévoir un entretien
    │   │           ├── CalendrierService.java          # Rassemble historique, planifications et échéances
    │   │           ├── ConfigService.java              # Lecture/écriture de la clé API (config.properties)
    │   │           ├── ControleTechniqueService.java   # État du prochain contrôle technique
    │   │           ├── CsvHistorique.java              # Écriture du fichier CSV de l'historique
    │   │           ├── EvenementCalendrier.java        # Événement affiché dans le calendrier
    │   │           ├── ExportService.java              # Export CSV (fenêtre « Enregistrer sous »)
    │   │           ├── HistoriqueKilometrageService.java # Cohérence des relevés kilométriques
    │   │           ├── ImageService.java               # Import/copie des photos de véhicules
    │   │           └── RecherchePlaqueService.java     # Recherche d'un véhicule par plaque
    │   └── resources/com/carnetentretien/carnetentretien/
    │       ├── style.css                               # Styles globaux
    │       ├── dashboard.fxml                          # Fenêtre principale (assemble les vues)
    │       ├── vues/                                   # Écrans modifiables dans SceneBuilder
    │       │   ├── accueil.fxml                        # Écran d'accueil
    │       │   ├── calendrier.fxml                     # Calendrier mensuel des entretiens
    │       │   └── vehicule.fxml                       # Écran d'un véhicule
    │       └── dialogues/                              # Fenêtres de saisie modifiables dans SceneBuilder
    │           ├── cle-api.fxml                        # Clé d'accès au service de plaques
    │           ├── controle-technique.fxml             # Enregistrement d'un contrôle technique
    │           ├── controle-technique-planification.fxml # Planification d'un contrôle technique
    │           ├── entretien.fxml                      # Ajout/modification d'un entretien
    │           ├── kilometrage.fxml                    # Saisie d'un relevé kilométrique
    │           ├── planification.fxml                  # Planification d'un entretien
    │           ├── regle.fxml                          # Ajout/modification d'une règle de suivi
    │           ├── reglages.fxml                       # Liste des règles de suivi
    │           └── vehicule.fxml                       # Ajout/modification d'un véhicule
    └── test/java/com/carnetentretien/carnetentretien/  # Tests unitaires JUnit 5
        ├── BaseDonneesTest.java                        # Base des tests : dossier de données temporaire
        ├── BaseBddTest.java                            # Base des tests : base SQLite vierge
        ├── model/
        │   ├── EntretienTest.java                      # Test Entretien
        │   ├── PreconisationTest.java                  # Test Preconisation
        │   ├── ReleveKilometrageTest.java              # Test ReleveKilometrage
        │   └── VehiculeTest.java                       # Test Vehicule
        ├── db/
        │   ├── ControleTechniqueDBTest.java            # Test ControleTechniqueDB
        │   ├── ControleTechniquePlanifieDBTest.java    # Test ControleTechniquePlanifieDB
        │   ├── DatabaseManagerTest.java                # Test DatabaseManager
        │   ├── EntretienDBTest.java                    # Test EntretienDB
        │   ├── EntretienPlanifieDBTest.java            # Test EntretienPlanifieDB
        │   ├── PreconisationDBTest.java                # Test PreconisationDB
        │   ├── ReleveKilometrageDBTest.java            # Test ReleveKilometrageDB
        │   └── VehiculeDBTest.java                     # Test VehiculeDB
        └── service/
            ├── AppPathsTest.java                       # Test AppPaths
            ├── CalculateurEntretienTest.java           # Test CalculateurEntretien
            ├── CalendrierServiceTest.java              # Test CalendrierService
            ├── ConfigServiceTest.java                  # Test ConfigService
            ├── ControleTechniqueServiceTest.java       # Test ControleTechniqueService
            ├── CsvHistoriqueTest.java                  # Test CsvHistorique
            ├── HistoriqueKilometrageServiceTest.java   # Test HistoriqueKilometrageService
            ├── ImageServiceTest.java                   # Test ImageService
            └── RecherchePlaqueServiceTest.java         # Test RecherchePlaqueService
```

## Modèle de données

### Table `Vehicule`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER PK | Identifiant unique |
| `immatriculation` | TEXT | Plaque d'immatriculation (facultative) |
| `marque` | TEXT | Marque du véhicule |
| `modele` | TEXT | Modèle |
| `kilometrage_actuel` | INTEGER | Dernier kilométrage relevé |
| `kilometrage_annuel_estime` | INTEGER | Estimation km/an |
| `image_url` | TEXT | URL de la photo trouvée par la recherche de plaque |
| `image_locale` | TEXT | Chemin de la photo personnelle importée |

### Table `Entretien_Fait`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER PK | Identifiant unique |
| `vehicule_id` | INTEGER | Identifiant du véhicule (clé étrangère vers `Vehicule`) |
| `type_operation` | TEXT | Type d'opération |
| `date_realisation` | DATE | Date de réalisation |
| `kilometrage_realisation` | INTEGER | Kilométrage du véhicule au moment de l'opération |
| `cout` | REAL | Coût de l'opération |

### Table `Preconisation`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER PK | Identifiant unique |
| `type_operation` | TEXT | Type d'opération |
| `intervalle_km` | INTEGER | Intervalle en kilomètres |
| `intervalle_mois` | INTEGER | Intervalle en mois |

### Table `Entretien_Planifie`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER PK | Identifiant unique |
| `vehicule_id` | INTEGER | Identifiant du véhicule (clé étrangère vers `Vehicule`) |
| `type_operation` | TEXT | Type d'opération prévue |
| `date_prevue` | DATE | Date à laquelle l'entretien est prévu |
| `note` | TEXT | Remarque libre (facultative) |

### Table `ControleTechnique`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER PK | Identifiant unique |
| `vehicule_id` | INTEGER | Identifiant du véhicule (clé étrangère vers `Vehicule`) |
| `date_realisation` | DATE | Date du contrôle |
| `resultat` | TEXT | `FAVORABLE` ou `DEFAVORABLE` |
| `date_prochaine` | DATE | Date de la prochaine visite (2 ans ou 2 mois) |
| `note` | TEXT | Remarque libre (facultative) |

### Table `ControleTechnique_Planifie`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER PK | Identifiant unique |
| `vehicule_id` | INTEGER | Identifiant du véhicule (clé étrangère vers `Vehicule`) |
| `date_prevue` | DATE | Date à laquelle le contrôle technique est prévu |
| `note` | TEXT | Remarque libre (facultative) |

### Table `Releve_Kilometrage`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER PK | Identifiant unique |
| `vehicule_id` | INTEGER | Identifiant du véhicule (clé étrangère vers `Vehicule`) |
| `date_releve` | DATE | Date de la lecture du compteur (jamais dans le futur) |
| `kilometrage` | INTEGER | Valeur affichée au compteur ce jour-là |
| `note` | TEXT | Remarque libre (facultative) |

## Principe de la prédiction

Le moteur extrapole le kilométrage du jour à partir du dernier relevé :

```
km_estimé = km_actuel + ((jours_écoulés / 365) × km_annuel_estime)
```

Chaque pièce d'entretien est ensuite comparée à sa prochaine échéance :

- **< 80%** de la limite → OK
- **80% – 100%** → À prévoir
- **> 100%** → Urgent

## Historique kilométrique

Le kilométrage n'est pas une simple valeur modifiable : chaque lecture du compteur est conservée
comme un **relevé daté** (table `Releve_Kilometrage`), avec une remarque facultative.

- le relevé le plus récent détermine le `kilometrage_actuel` du véhicule, utilisé par les jauges
  et les prévisions d'échéance ;
- plusieurs relevés peuvent être saisis le même jour, mais un nouveau relevé ne peut pas être
  inférieur au précédent du même jour ;
- la saisie est refusée si la date est dans le futur, si le kilométrage est négatif ou identique
  à un relevé existant, ou si le compteur reculerait par rapport au relevé précédent ou
  dépasserait le relevé suivant ;
- le champ « Kilomètres au compteur » est désactivé lors de la modification d'un véhicule : la
  mise à jour passe par le bouton « ➕ Enregistrer un relevé » de l'écran du véhicule ;
- un véhicule créé reçoit automatiquement un relevé initial daté du jour ; les véhicules créés
  avant cette fonctionnalité en reçoivent un à la première ouverture de la base migrée ;
- supprimer un véhicule supprime aussi son historique kilométrique ;
- l'export CSV contient une section `HISTORIQUE KILOMÉTRIQUE` avec la variation entre deux relevés.

## Contrôle technique

Le suivi du contrôle technique s'appuie sur un historique complet (table `ControleTechnique`) :

- **Favorable** → prochaine visite proposée **2 ans** après le contrôle ;
- **Défavorable** → contre-visite proposée **2 mois** après le contrôle ;
- la date proposée reste modifiable (le rapport officiel fait foi) ;
- le prochain contrôle est signalé **2 mois** avant son échéance (badge « À prévoir », bandeau et
  événements dans le calendrier), puis « En retard » une fois la date dépassée ;
- un contrôle peut être **planifié** depuis le calendrier (date, véhicule, remarque) : la
  planification remplace alors l'échéance calculée, puis « Fait » enregistre le contrôle réalisé ;
- la suppression d'un véhicule efface aussi ses contrôles techniques et leurs planifications.

## Contribution

Les contributions sont les bienvenues ! N'hésitez pas à ouvrir une ***issue*** pour signaler un bug ou proposer une fonctionnalité, ou à soumettre une *pull request*.

## Licence

Copyright (c) 2026 [PaulMUNOZ14](https://github.com/PaulMUNOZ14). Tous droits réservés.

Aucune autorisation n'est accordée pour utiliser, copier, modifier ou distribuer ce logiciel, en tout ou en partie, sans l'accord écrit préalable de l'auteur.

## Remerciements

- La communauté JavaFX pour ses bibliothèques de qualité
- Tous les contributeurs de `sqlite-jdbc`, `ControlsFX` et `Ikonli`

---

**Auteur** : [PaulMUNOZ14](https://github.com/PaulMUNOZ14)

# Carnet Entretien

Application de bureau pour gérer l'entretien de vos véhicules
## Description

**Carnet Entretien** est une application JavaFX qui permet de :

- Gérer plusieurs véhicules (marque, modèle, kilométrage)
- Estimer automatiquement le kilométrage actuel en fonction de votre usage annuel
- Anticiper les échéances d'entretien (vidange, courroie de distribution, filtres, etc.)
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

### En cours / À venir

- [ ] Formulaire de modification/suppression de véhicule
- [ ] Gestion des préconisations constructeur (intervalles km/mois)
- [ ] Algorithme de prédiction des échéances (extrapolation du kilométrage)
- [ ] Tableau de bord avec jauges d'usure (ProgressIndicator + CSS)
- [ ] Alertes visuelles (À prévoir / Urgent)
- [ ] Export en installeur autonome (via `jpackage`)

## Stack technique

| Composant | Technologie |
|-----------|-------------|
| Langage | Java 21+ |
| Interface | JavaFX 21 |
| Build | Maven |
| Base de données | SQLite (via `sqlite-jdbc`) |
| Icônes | Ikonli |
| UI avancée | ControlsFX |
| Tests | JUnit 5 |

## Prérequis

- **JDK 21 ou supérieur** (testé avec OpenJDK 27)
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
├── carnet_entretien.db (généré automatiquement au lancement du programme)
├── .gitingore
└── src/
    ├── main/
    │   ├── java/com/carnetentretien/carnetentretien/
    │   │   ├── HelloApplication.java       # Point d'entrée JavaFX
    │   │   ├── Launcher.java               # Lanceur de l'application
    │   │   ├── model/                      # Objets métier (Vehicule, Entretien...)
    │   │   │   ├── Entretien.java          # Objet Entretien
    │   │   │   └── Vehicule.java           # Objet Vehicule
    │   │   ├── db/                         # Accès base de données (DatabaseManager, VehiculeDB...)
    │   │   │   ├── DatabaseManager.java    # Gestionnaire de la base de données
    │   │   │   ├── EntretienDB.java        # Gestionnaire de l'entretien dans la BDD
    │   │   │   └── VehiculeDB.java         # Gestionnaire des vehicules dans la BDD
    │   │   └── controller/                 # Contrôleurs FXML
    │   │       └── DashboardController     # Contrôleur du tableau de bord
    │   ├── resources/com/carnetentretien/carnetentretien/
    │   │   └── hello-view.fxml             # fxml
    │   └── module-info.java
    └── test/java/                          # Tests unitaires JUnit
```

## Modèle de données

### Table `Vehicule`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER PK | Identifiant unique |
| `marque` | TEXT | Marque du véhicule |
| `modele` | TEXT | Modèle |
| `kilometrage_actuel` | INTEGER | Dernier kilométrage relevé |
| `kilometrage_annuel_estime` | INTEGER | Estimation km/an |

### Table `Entretien_Fait`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER PK | Identifiant unique |
| `vehicule_id` | INTEGER | Identifiant du véhicule |
| `type_operation` | TEXT | Type d'opération |
| `date_realisation` | DATE | Date de réalisation |
| `kilometrage_realisation` | INTEGER | Kilométrage du véhicule au moment de l'opération |
| `cout` | REAL | cout de l'opération |

### Tables prévues (à venir)

- `Preconisation` : plan d'entretien constructeur (intervalle km/mois)

## Principe de la prédiction

Le moteur extrapole le kilométrage du jour à partir du dernier relevé :

```
km_estimé = km_actuel + ((jours_écoulés / 365) × km_annuel_estime)
```

Chaque pièce d'entretien est ensuite comparée à sa prochaine échéance :

- **< 80%** de la limite → OK
- **80% – 100%** → À prévoir
- **> 100%** → Urgent

## Contribution

Les contributions sont les bienvenues ! N'hésitez pas à ouvrir une ***issue*** pour signaler un bug ou proposer une fonctionnalité, ou à soumettre une *pull request*.

## Licence

Copyright (c) 2026 PaulMUNOZ14. Tous droits réservés.

Aucune autorisation n'est accordée pour utiliser, copier, modifier ou distribuer ce logiciel, en tout ou en partie, sans l'accord écrit préalable de l'auteur.

## Remerciements

- La communauté JavaFX pour ses bibliothèques de qualité
- Tous les contributeurs de `sqlite-jdbc`, `ControlsFX` et `Ikonli`

---

**Auteur** : PaulMUNOZ14
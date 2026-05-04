# Projet SDA — Route Planning 

Ce projet a été réalisé dans le cadre du cours de SDA par Abir BAHROUN, Waris MANSOOR et Mathurin MARTEL

L’objectif est de construire un petit moteur de route planning à partir d’un graphe routier extrait d’OpenStreetMap. On charge les données, on construit une représentation compacte du graphe en CSR, puis on compare plusieurs algorithmes de plus court chemin : Dijkstra, A*, ALT et CH.

## Contenu du projet

Le projet contient principalement :

- `osmnx_complet.py` : script Python qui extrait les données OpenStreetMap et génère les fichiers CSV.
- `donnees_osmnx.csv` : fichier contenant les arêtes du graphe.
- `coordinates_osmnx.csv` : fichier contenant les coordonnées des nœuds.
- `GrapheCSR.java` : chargement du graphe et construction de la représentation CSR.
- `Dijkstra.java` : algorithme de référence.
- `AStar.java` : algorithme A* avec heuristique euclidienne.
- `ALT.java` : algorithme ALT avec landmarks.
- `Java_CH_donnees.java` : Phase de prétraitement de l'algorithme CH.
- `Java_CH.java` : Lancement de l'algorithme CH.
- `Benchmark.java` : génération des requêtes, lancement des tests et création des CSV.
- `Main.java` : point d’entrée du programme.
- `ResultatChemin.java` : stockage des résultats d’une requête.
- `NoeudPriorite.java` : objet utilisé dans les files de priorité.

## Génération des données

Pour générer les fichiers CSV depuis OpenStreetMap, lancer :

    python osmnx_complet.py OU py osmnx_complet.py

Ce script produit normalement :

- `donnees_osmnx.csv`
- `coordinates_osmnx.csv`

Ces deux fichiers doivent être dans le même dossier que les fichiers Java avant de lancer le programme.

## Compilation

Pour compiler tous les fichiers Java, lancer :

    javac *.java

## Exécution

Pour lancer le programme principal :

    java Main

Le programme charge le graphe, lance les algorithmes, puis exécute le benchmark.

## Benchmark

Le benchmark est lancé depuis `Benchmark.java`.

Il génère automatiquement des couples départ-arrivée. Les requêtes ne sont pas écrites à la main dans un fichier séparé : elles sont tirées aléatoirement dans le graphe, mais avec une graine fixe.

Dans le code, la graine utilisée est :

    GRAINE_RANDOM = 42

Cela permet d’avoir les mêmes requêtes à chaque exécution, donc des résultats reproductibles.

Le benchmark cherche à obtenir :

- 30 requêtes courtes
- 30 requêtes moyennes
- 30 requêtes longues

Pour chaque requête, les algorithmes sont comparés sur le même départ et la même arrivée.

## Fichiers générés

Après exécution, le benchmark produit trois fichiers CSV :

- `resultats_benchmark.csv`
- `resume_benchmark.csv`
- `resultats_profiling.csv`

### `resultats_benchmark.csv`

Ce fichier contient les résultats détaillés, ligne par ligne.

Pour chaque requête, on retrouve :

- le type de requête ;
- le départ ;
- l’arrivée ;
- l’algorithme utilisé ;
- la distance trouvée ;
- le temps d’exécution ;
- le nombre de nœuds visités ;
- le nombre de relaxations ;
- le nombre d’extractions ;
- la vérification par rapport à Dijkstra.

### `resume_benchmark.csv`

Ce fichier résume les résultats par type de requête et par algorithme.

Il contient notamment :

- le temps moyen ;
- le P50 ;
- le P95 ;
- le débit en requêtes par seconde ;
- les nœuds visités moyens ;
- les relaxations moyennes ;
- les extractions moyennes.

### `resultats_profiling.csv`

Ce fichier contient les mesures liées à la mémoire et au prétraitement.

Il indique notamment :

- le nombre de nœuds ;
- le nombre d’arêtes ;
- la mémoire utilisée par le graphe CSR ;
- le temps de prétraitement ALT ;
- la mémoire supplémentaire utilisée par ALT.

## Vérification des résultats

Dijkstra sert de référence.

Pour chaque requête retenue, le benchmark lance d’abord Dijkstra. Ensuite, A*,ALT et CH sont comparés à Dijkstra.

Si la distance retournée est la même, avec une petite tolérance pour les nombres flottants, alors le résultat est considéré comme correct.

## Remarque sur le lot de requêtes

Le lot de requêtes est généré directement dans `Benchmark.java`.

Il n’y a donc pas forcément de fichier `lot_requetes.csv`. Le choix a été fait comme ça pour éviter d’écrire les requêtes à la main, tout en gardant des tests reproductibles grâce à la graine fixe.

## Résumé rapide pour exécuter le projet :

Pour lancer le projet depuis zéro :

    python osmnx_complet.py
    javac *.java
    java Main

À la fin, les CSV de résultats sont créés automatiquement.

/*
 * Classe Main.
 *
 * C'est le point d'entrée du programme.
 * Le Main organise juste le déroulement :
 * 1) charger le graphe ;
 * 2) créer Dijkstra, A* et ALT ;
 * 3) afficher quelques exemples ;
 * 4) lancer le benchmark complet.
 */
public class Main{
    // Fichier contenant les arêtes du graphe : départ, arrivée, distance.
    private static final String FICHIER_GRAPHE = "donnees_osmnx.csv";

    // Fichier contenant les coordonnées des noeuds, utilisées par A*.
    private static final String FICHIER_COORDONNEES = "coordinates_osmnx.csv";

    public static void main(String[] args){
        // Première étape : charger le graphe une seule fois.
        GrapheCSR graphe = chargerGraphe();

        if(graphe == null){
            return;
        }

        /*
         * On crée les objets algorithmes.
         * Chacun reçoit le même graphe CSR.
         */
        Dijkstra dijkstra = new Dijkstra(graphe);
        AStar    astar    = new AStar(graphe);
        ALT      altDemo  = new ALT(graphe);

        /*
         * ALT a besoin d'un prétraitement avant de pouvoir répondre à des requêtes.
         * altDemo sert seulement pour les exemples affichés dans le terminal.
         * Le benchmark créera ses propres ALT avec 2, 4 et 8 landmarks.
         */
        altDemo.pretraitement(4);

        // Petite démonstration lisible dans le terminal.
        lancerDemo(dijkstra, astar, altDemo);

        // Benchmark complet : création des CSV pour le rapport.
        Benchmark benchmark = new Benchmark(graphe, dijkstra, astar);
        benchmark.lancer();
    }

    private static GrapheCSR chargerGraphe(){
        GrapheCSR graphe = new GrapheCSR();

        try{
            // Lecture du CSV et construction des tableaux CSR en une seule étape.
            graphe.charger(FICHIER_GRAPHE);

            // Chargement des coordonnées pour l'heuristique de A*.
            graphe.lireCoordonnees(FICHIER_COORDONNEES);
        }
        catch(Exception e){
            System.out.println("Erreur pendant le chargement : " + e.getMessage());
            return null;
        }

        return graphe;
    }

    private static void lancerDemo(Dijkstra dijkstra, AStar astar, ALT alt){
        /*
         * Ces requêtes servent à afficher des exemples complets dans le terminal.
         * C'est pratique pour une démonstration orale.
         */
        lancerComparaison(dijkstra, astar, alt, "25191346", "5574563073");
       // lancerComparaison(dijkstra, astar, alt, "10713114647", "24972336");
        //lancerComparaison(dijkstra, astar, alt, "25096406", "25001426");
    }

    private static void lancerComparaison(Dijkstra dijkstra, AStar astar, ALT alt, String depart, String arrivee){
        // Dijkstra en premier : il sert de référence car il donne le vrai plus court chemin.
        ResultatChemin resultatDijkstra = dijkstra.executer(depart, arrivee);
        resultatDijkstra.afficher();

        ResultatChemin resultatAStar = astar.executer(depart, arrivee);
        resultatAStar.afficher();

        ResultatChemin resultatALT = alt.executer(depart, arrivee);
        resultatALT.afficher();

        verifierDistances(resultatDijkstra, resultatAStar, resultatALT);
    }

    private static void verifierDistances(ResultatChemin dijkstra, ResultatChemin astar, ResultatChemin alt){
        System.out.println();
        System.out.println("--- Vérification des distances ---");

        if(!dijkstra.cheminTrouve){
            System.out.println("Dijkstra n'a pas trouvé de chemin, donc comparaison impossible.");
            return;
        }

        System.out.println("Distance Dijkstra = " + dijkstra.distance);
        System.out.println("Distance A*       = " + astar.distance);
        System.out.println("Distance ALT      = " + alt.distance);

        float tolerance = 0.01f;

        if(astar.cheminTrouve && Math.abs(dijkstra.distance - astar.distance) <= tolerance){
            System.out.println("A* OK : même distance que Dijkstra.");
        }
        else{
            System.out.println("Attention : A* ne donne pas la même distance que Dijkstra.");
        }

        if(alt.cheminTrouve && Math.abs(dijkstra.distance - alt.distance) <= tolerance){
            System.out.println("ALT OK : même distance que Dijkstra.");
        }
        else{
            System.out.println("Attention : ALT ne donne pas la même distance que Dijkstra.");
        }

        System.out.println("----------------------------------");
    }
}

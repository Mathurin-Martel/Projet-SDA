public class Main{

    private static final String FICHIER_GRAPHE      = "donnees_osmnx.csv";
    private static final String FICHIER_COORDONNEES = "coordinates_osmnx.csv";

    public static void main(String[] args){

        GrapheCSR graphe = chargerGraphe();

        if(graphe == null){
            return;
        }

        Dijkstra dijkstra = new Dijkstra(graphe);
        AStar    astar    = new AStar(graphe);
        ALT      altDemo  = new ALT(graphe);

        altDemo.pretraitement(4);

        // --- Prétraitement CH ---
        CH ch = new CH();
        try{
            ch.pretraitement(FICHIER_GRAPHE);
        }
        catch(Exception e){
            System.out.println("Erreur prétraitement CH : " + e.getMessage());
            ch = null;
        }

        lancerDemo(dijkstra, astar, altDemo, ch);

        Benchmark benchmark = new Benchmark(graphe, dijkstra, astar, ch);
        benchmark.lancer();
    }

    private static GrapheCSR chargerGraphe(){
        GrapheCSR graphe = new GrapheCSR();

        try{
            graphe.charger(FICHIER_GRAPHE);
            graphe.lireCoordonnees(FICHIER_COORDONNEES);
        }
        catch(Exception e){
            System.out.println("Erreur pendant le chargement : " + e.getMessage());
            return null;
        }

        return graphe;
    }

    private static void lancerDemo(Dijkstra dijkstra, AStar astar, ALT alt, CH ch){
        lancerComparaison(dijkstra, astar, alt, ch, "25191346", "5574563073");
    }

    private static void lancerComparaison(Dijkstra dijkstra, AStar astar, ALT alt, CH ch,
                                          String depart, String arrivee){

        ResultatChemin resultatDijkstra = dijkstra.executer(depart, arrivee);
        resultatDijkstra.afficher();

        ResultatChemin resultatAStar = astar.executer(depart, arrivee);
        resultatAStar.afficher();

        ResultatChemin resultatALT = alt.executer(depart, arrivee);
        resultatALT.afficher();

        ResultatChemin resultatCH = null;
        if(ch != null){
            resultatCH = ch.executer(depart, arrivee);
            resultatCH.afficher();
        }

        verifierDistances(resultatDijkstra, resultatAStar, resultatALT, resultatCH);
    }

    private static void verifierDistances(ResultatChemin dijkstra, ResultatChemin astar,
                                          ResultatChemin alt, ResultatChemin ch){
        System.out.println();
        System.out.println("--- Vérification des distances ---");

        if(!dijkstra.cheminTrouve){
            System.out.println("Dijkstra n'a pas trouvé de chemin, donc comparaison impossible.");
            return;
        }

        float tolerance = 0.01f;

        System.out.println("Distance Dijkstra = " + dijkstra.distance);
        System.out.println("Distance A*       = " + astar.distance);
        System.out.println("Distance ALT      = " + alt.distance);

        if(ch != null){
            System.out.println("Distance CH       = " + ch.distance);
        }

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

        if(ch != null){
            if(ch.cheminTrouve && Math.abs(dijkstra.distance - ch.distance) <= tolerance){
                System.out.println("CH OK : même distance que Dijkstra.");
            }
            else{
                System.out.println("Attention : CH ne donne pas la même distance que Dijkstra.");
            }
        }

        System.out.println("----------------------------------");
    }
}

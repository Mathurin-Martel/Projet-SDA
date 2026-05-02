import java.util.ArrayList;
import java.util.Collections;

/*
 * Cette classe sert juste à stocker proprement le résultat d'un algorithme.
 *
 * Au lieu que Dijkstra, A* ou ALT affichent directement tout n'importe comment,
 * ils remplissent un objet ResultatChemin.
 * Ensuite Main ou Benchmark peut utiliser cet objet pour :
 * - afficher un chemin dans le terminal ;
 * - écrire les résultats dans un CSV ;
 * - vérifier si A* et ALT donnent la même distance que Dijkstra.
 */
public class ResultatChemin{
    // Nom de l'algorithme : "DIJKSTRA", "A*", "ALT (4 landmarks)", etc.
    String nomAlgorithme;

    // Départ et arrivée sous forme de noms de noeuds, donc les identifiants OSM en String.
    String depart;
    String arrivee;

    // true si un chemin a été trouvé, false sinon.
    boolean cheminTrouve;

    // Distance totale du chemin trouvé.
    // Si aucun chemin n'est trouvé, on laisse -1.
    float distance;

    // Liste des noeuds du chemin, dans l'ordre départ -> arrivée.
    ArrayList<String> chemin;

    // Mesures demandées dans le sujet : extractions, relaxations, noeuds visités, temps.
    int nombreExtractions;
    int nombreRelaxations;
    int nombreNoeudsVisites;
    double tempsMs;

    public ResultatChemin(String nomAlgorithme, String depart, String arrivee){
        this.nomAlgorithme = nomAlgorithme;
        this.depart = depart;
        this.arrivee = arrivee;

        // Par défaut, on considère que rien n'est trouvé.
        // L'algorithme mettra cheminTrouve à true seulement à la fin si tout s'est bien passé.
        this.cheminTrouve = false;
        this.distance = -1f;
        this.chemin = new ArrayList<String>();

        // Les compteurs commencent à 0 et sont augmentés pendant l'algorithme.
        this.nombreExtractions = 0;
        this.nombreRelaxations = 0;
        this.nombreNoeudsVisites = 0;
        this.tempsMs = 0.0;
    }

    /*
     * Reconstruction du chemin à partir du tableau precedent[].
     *
     * Cette méthode est commune à Dijkstra, A* et ALT : ils ont tous
     * le même tableau precedent[] et le même besoin de remonter
     * jusqu'au départ pour obtenir le chemin dans l'ordre correct.
     *
     * On part de l'arrivée, on remonte jusqu'au départ, puis on inverse.
     */
    public void reconstruireChemin(int depart, int arrivee, int[] precedent, GrapheCSR graphe){
        chemin = new ArrayList<String>();
        int courant = arrivee;

        while(courant != -1){
            chemin.add(graphe.getNomNoeud(courant));

            if(courant == depart){
                break;
            }

            courant = precedent[courant];
        }

        // Comme on a construit arrivée -> départ, on inverse pour afficher départ -> arrivée.
        Collections.reverse(chemin);
    }

    public void afficher(){
        System.out.println();
        System.out.println("========================================");
        System.out.println("ALGORITHME : " + nomAlgorithme);
        System.out.println("Départ : " + depart);
        System.out.println("Arrivée : " + arrivee);
        System.out.println("========================================");

        if(!(cheminTrouve)){
            // Cas possible dans un graphe orienté : il peut exister une route A -> B,
            // mais pas forcément une route B -> A.
            System.out.println("Aucun chemin trouvé entre " + depart + " et " + arrivee);
        }
        else{
            System.out.println("Chemin :");
            System.out.println(String.join(" -> ", chemin));

            System.out.println("Distance = " + distance);
        }

        // Ces valeurs servent à comparer les algorithmes dans le rapport.
        System.out.println("Nombre d'extractions = " + nombreExtractions);
        System.out.println("Nombre de relaxations = " + nombreRelaxations);
        System.out.println("Nombre de noeuds visités = " + nombreNoeudsVisites);
        System.out.println("Temps total = " + tempsMs + " ms");
        System.out.println("========================================");
    }
}

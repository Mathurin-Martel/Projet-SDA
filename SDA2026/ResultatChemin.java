import java.util.ArrayList;
import java.util.Collections;

public class ResultatChemin{

    String nomAlgorithme;

    String depart;
    String arrivee;

    boolean cheminTrouve;

    float distance;

    ArrayList<String> chemin;

    int nombreExtractions;
    int nombreRelaxations;
    int nombreNoeudsVisites;
    double tempsMs;

    public ResultatChemin(String nomAlgorithme, String depart, String arrivee){
        this.nomAlgorithme = nomAlgorithme;
        this.depart = depart;
        this.arrivee = arrivee;

        this.cheminTrouve = false;
        this.distance = -1f;
        this.chemin = new ArrayList<String>();

        this.nombreExtractions = 0;
        this.nombreRelaxations = 0;
        this.nombreNoeudsVisites = 0;
        this.tempsMs = 0.0;
    }

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

            System.out.println("Aucun chemin trouvé entre " + depart + " et " + arrivee);
        }
        else{
            System.out.println("Chemin :");
            System.out.println(String.join(" -> ", chemin));

            System.out.println("Distance = " + distance);
        }

        System.out.println("Nombre d'extractions = " + nombreExtractions);
        System.out.println("Nombre de relaxations = " + nombreRelaxations);
        System.out.println("Nombre de noeuds visités = " + nombreNoeudsVisites);
        System.out.println("Temps total = " + tempsMs + " ms");
        System.out.println("========================================");
    }
}

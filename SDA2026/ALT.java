import java.util.ArrayList;
import java.util.PriorityQueue;

/*
 * Classe ALT.
 *
 * ALT signifie : A*, Landmarks, Triangle inequality.
 * En français simple : c'est A* avec des points repères (landmarks).
 *
 * L'idée :
 * - on choisit quelques noeuds appelés landmarks pendant le prétraitement ;
 * - on lance Dijkstra depuis chaque landmark pour connaître les distances
 *   vers tous les autres noeuds ;
 * - pendant les requêtes, ces distances permettent de calculer une estimation
 *   plus précise que la simple distance euclidienne de A*.
 *
 * ALT a donc un coût au départ (prétraitement + mémoire supplémentaire),
 * mais en échange les requêtes visitent moins de noeuds.
 *
 * Pour lire cet algorithme, commencez par pretraitement() puis executer().
 */
public class ALT{

    private GrapheCSR graphe;

    // Liste des landmarks sous forme d'ids entiers.
    private ArrayList<Integer> landmarks;

    // distancesLandmarks[i][v] = distance depuis le landmark i vers le noeud v.
    private float[][] distancesLandmarks;

    // Mesures utilisées pour resultats_profiling.csv.
    private double tempsPretraitementMs    = 0.0;
    private int    nombreLandmarksUtilises = 0;

    public ALT(GrapheCSR graphe){
        this.graphe     = graphe;
        this.landmarks  = new ArrayList<Integer>();
    }

    // =========================================================================
    // Prétraitement — à appeler avant executer()
    // =========================================================================

    /*
     * Choisit les landmarks et calcule les distances depuis chaque landmark
     * vers tous les autres noeuds (via Dijkstra complet).
     *
     * Cette étape est coûteuse mais se fait une seule fois.
     * Elle permet ensuite d'avoir une meilleure heuristique pendant les requêtes.
     */
    public void pretraitement(int nombreLandmarks){
        choisirLandmarks(nombreLandmarks);

        distancesLandmarks = new float[landmarks.size()][];

        System.out.println();
        System.out.println("--- Prétraitement ALT ---");
        System.out.println("Nombre de landmarks = " + landmarks.size());

        long tempsDebut = System.nanoTime();

        for(int i = 0; i < landmarks.size(); i = i + 1){
            int landmark = landmarks.get(i);
            System.out.println("Prétraitement du landmark : " + graphe.getNomNoeud(landmark));
            distancesLandmarks[i] = distancesDepuis(landmark);
        }

        long tempsFin          = System.nanoTime();
        tempsPretraitementMs   = (tempsFin - tempsDebut) / 1000000.0;
        nombreLandmarksUtilises = landmarks.size();

        System.out.println("Prétraitement ALT terminé en " + tempsPretraitementMs + " ms");
        System.out.println("--- Fin du prétraitement ALT ---");
    }

    private void choisirLandmarks(int nombreLandmarks){
        landmarks.clear();

        /*
         * On commence avec quelques landmarks fixes issus du jeu de données Paris OSM.
         * Ils sont stables et permettent des tests reproductibles.
         */
        String[] landmarksPossibles = {
            "12339318466",
            "247277351",
            "25075848",
            "268401942"
        };

        for(int i = 0; i < landmarksPossibles.length; i = i + 1){
            if(landmarks.size() >= nombreLandmarks){
                break;
            }

            if(graphe.contientNoeud(landmarksPossibles[i])){
                landmarks.add(graphe.getIdNoeud(landmarksPossibles[i]));
            }
        }

        /*
         * Si on demande plus de landmarks que ceux listés au-dessus,
         * on complète avec des noeuds espacés régulièrement.
         */
        int nombreNoeuds = graphe.getNombreNoeuds();

        if(nombreLandmarks <= 0){
            nombreLandmarks = 4;
        }

        int intervalle = nombreNoeuds / nombreLandmarks;

        if(intervalle <= 0){
            intervalle = 1;
        }

        for(int i = 0; i < nombreLandmarks; i = i + 1){
            if(landmarks.size() >= nombreLandmarks){
                break;
            }

            int id = i * intervalle;

            if(id >= nombreNoeuds){
                id = nombreNoeuds - 1;
            }

            if(!landmarks.contains(id)){
                landmarks.add(id);
            }
        }
    }

    /*
     * Dijkstra complet depuis un landmark, sans destination précise.
     *
     * On n'utilise pas d'heuristique ici car on veut explorer tout le graphe
     * pour connaître les distances vers tous les noeuds.
     */
    private float[] distancesDepuis(int idDepart){
        int     nombreNoeuds = graphe.getNombreNoeuds();
        int[]   offsetCSR    = graphe.getOffsetCSR();
        int[]   voisinsCSR   = graphe.getVoisinsCSR();
        float[] poidsCSR     = graphe.getPoidsCSR();

        float[]   distance   = new float[nombreNoeuds];
        boolean[] dejaVisite = new boolean[nombreNoeuds];

        for(int i = 0; i < nombreNoeuds; i = i + 1){
            distance[i]   = Float.POSITIVE_INFINITY;
            dejaVisite[i] = false;
        }

        PriorityQueue<NoeudPriorite> filePriorite = new PriorityQueue<NoeudPriorite>();
        distance[idDepart] = 0f;
        filePriorite.add(new NoeudPriorite(idDepart, 0f));

        while(!filePriorite.isEmpty()){
            NoeudPriorite noeud = filePriorite.poll();
            int u = noeud.id;

            if(dejaVisite[u]){
                continue;
            }

            if(noeud.priorite > distance[u]){
                continue;
            }

            dejaVisite[u] = true;

            for(int i = offsetCSR[u]; i < offsetCSR[u + 1]; i = i + 1){
                int   v              = voisinsCSR[i];
                float poids          = poidsCSR[i];
                float nouvelleDistance = distance[u] + poids;

                if(!dejaVisite[v] && nouvelleDistance < distance[v]){
                    distance[v] = nouvelleDistance;
                    filePriorite.add(new NoeudPriorite(v, nouvelleDistance));
                }
            }
        }

        return distance;
    }

    // =========================================================================
    // Requête — même boucle que Dijkstra, avec heuristique landmarks
    // =========================================================================

    public ResultatChemin executer(String intersectionDepart, String intersectionArrivee){
        ResultatChemin resultat = new ResultatChemin(
            "ALT (" + nombreLandmarksUtilises + " landmarks)",
            intersectionDepart, intersectionArrivee
        );

        if(!graphe.contientNoeud(intersectionDepart)){
            System.out.println("Erreur : noeud de départ inconnu : " + intersectionDepart);
            return resultat;
        }

        if(!graphe.contientNoeud(intersectionArrivee)){
            System.out.println("Erreur : noeud d'arrivée inconnu : " + intersectionArrivee);
            return resultat;
        }

        int depart       = graphe.getIdNoeud(intersectionDepart);
        int arrivee      = graphe.getIdNoeud(intersectionArrivee);
        int nombreNoeuds = graphe.getNombreNoeuds();

        int[]   offsetCSR  = graphe.getOffsetCSR();
        int[]   voisinsCSR = graphe.getVoisinsCSR();
        float[] poidsCSR   = graphe.getPoidsCSR();

        float[]   distance   = new float[nombreNoeuds];
        int[]     precedent  = new int[nombreNoeuds];
        boolean[] dejaVisite = new boolean[nombreNoeuds];

        for(int i = 0; i < nombreNoeuds; i = i + 1){
            distance[i]   = Float.POSITIVE_INFINITY;
            precedent[i]  = -1;
            dejaVisite[i] = false;
        }

        PriorityQueue<NoeudPriorite> filePriorite = new PriorityQueue<NoeudPriorite>();
        distance[depart] = 0f;
        filePriorite.add(new NoeudPriorite(depart, heuristique(depart, arrivee)));

        long tempsDebut = System.nanoTime();

        while(!filePriorite.isEmpty()){
            NoeudPriorite noeud = filePriorite.poll();
            resultat.nombreExtractions = resultat.nombreExtractions + 1;

            int   u        = noeud.id;
            float priorite = noeud.priorite;

            if(dejaVisite[u]){
                continue;
            }

            if(priorite > distance[u] + heuristique(u, arrivee)){
                continue;
            }

            dejaVisite[u] = true;
            resultat.nombreNoeudsVisites = resultat.nombreNoeudsVisites + 1;

            if(u == arrivee){
                break;
            }

            for(int i = offsetCSR[u]; i < offsetCSR[u + 1]; i = i + 1){
                int   v     = voisinsCSR[i];
                float poids = poidsCSR[i];

                if(dejaVisite[v]){
                    continue;
                }

                float nouvelleDistance = distance[u] + poids;

                if(nouvelleDistance < distance[v]){
                    distance[v]  = nouvelleDistance;
                    precedent[v] = u;
                    // Priorité = vraie distance + estimation via landmarks (plus précise que A*).
                    filePriorite.add(new NoeudPriorite(v, nouvelleDistance + heuristique(v, arrivee)));
                    resultat.nombreRelaxations = resultat.nombreRelaxations + 1;
                }
            }
        }

        long tempsFin    = System.nanoTime();
        resultat.tempsMs = (tempsFin - tempsDebut) / 1000000.0;

        if(distance[arrivee] == Float.POSITIVE_INFINITY){
            return resultat;
        }

        resultat.cheminTrouve = true;
        resultat.distance     = distance[arrivee];
        resultat.reconstruireChemin(depart, arrivee, precedent, graphe);

        return resultat;
    }

    /*
     * Heuristique ALT : meilleure borne obtenue grâce aux landmarks.
     *
     * Pour chaque landmark L, l'inégalité triangulaire donne :
     * d(v, arrivée) >= d(L, arrivée) - d(L, v)
     *
     * On prend le maximum sur tous les landmarks pour avoir la borne la plus serrée.
     */
    private float heuristique(int noeud, int arrivee){
        float valeurMax = 0f;

        for(int i = 0; i < landmarks.size(); i = i + 1){
            float distanceLandmarkNoeud  = distancesLandmarks[i][noeud];
            float distanceLandmarkArrivee = distancesLandmarks[i][arrivee];

            // Si un landmark ne peut pas atteindre l'un des deux noeuds, on l'ignore.
            if(distanceLandmarkNoeud == Float.POSITIVE_INFINITY || distanceLandmarkArrivee == Float.POSITIVE_INFINITY){
                continue;
            }

            float valeur = distanceLandmarkArrivee - distanceLandmarkNoeud;

            if(valeur > valeurMax){
                valeurMax = valeur;
            }
        }

        return valeurMax;
    }

    // =========================================================================
    // Accesseurs pour Benchmark et profiling
    // =========================================================================

    public String getNomAlgorithme(){
        return "ALT (" + nombreLandmarksUtilises + " landmarks)";
    }

    public double getTempsPretraitementMs(){
        return tempsPretraitementMs;
    }

    public int getNombreLandmarksUtilises(){
        return nombreLandmarksUtilises;
    }

    public long getMemoireAdditionnelleALTOctets(){
        // Mémoire estimée : nombreLandmarks × nombreNoeuds × taille d'un float.
        return (long) nombreLandmarksUtilises * graphe.getNombreNoeuds() * Float.BYTES;
    }
}

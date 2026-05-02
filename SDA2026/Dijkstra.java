import java.util.PriorityQueue;

/*
 * Classe Dijkstra.
 *
 * Dijkstra est notre algorithme de référence.
 * Il explore le graphe en partant toujours du noeud le plus proche du départ.
 * Il est sûr : il trouve le vrai plus court chemin si les poids sont positifs.
 *
 * Pour lire cet algorithme, commencez par executer() en haut.
 * Tout se lit de haut en bas, sans aller chercher ailleurs.
 */
public class Dijkstra{

    protected GrapheCSR graphe;

    public Dijkstra(GrapheCSR graphe){
        this.graphe = graphe;
    }

    public ResultatChemin executer(String intersectionDepart, String intersectionArrivee){
        ResultatChemin resultat = new ResultatChemin("DIJKSTRA", intersectionDepart, intersectionArrivee);

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

        // Récupération des tableaux CSR.
        // offsetCSR[u] donne le début des voisins de u dans voisinsCSR.
        // offsetCSR[u + 1] donne la fin (exclue).
        int[]   offsetCSR  = graphe.getOffsetCSR();
        int[]   voisinsCSR = graphe.getVoisinsCSR();
        float[] poidsCSR   = graphe.getPoidsCSR();

        // distance[v] = meilleure distance connue du départ vers v.
        float[] distance = new float[nombreNoeuds];

        // precedent[v] = noeud précédent v dans le plus court chemin trouvé.
        int[] precedent = new int[nombreNoeuds];

        // dejaVisite[v] = true quand la distance finale de v est fixée.
        boolean[] dejaVisite = new boolean[nombreNoeuds];

        for(int i = 0; i < nombreNoeuds; i = i + 1){
            distance[i]   = Float.POSITIVE_INFINITY;
            precedent[i]  = -1;
            dejaVisite[i] = false;
        }

        // NoeudPriorite permet à Java de savoir lequel de deux noeuds
        // est prioritaire dans la file (voir NoeudPriorite.java).
        PriorityQueue<NoeudPriorite> filePriorite = new PriorityQueue<NoeudPriorite>();
        distance[depart] = 0f;
        filePriorite.add(new NoeudPriorite(depart, 0f));

        long tempsDebut = System.nanoTime();

        while(!filePriorite.isEmpty()){
            NoeudPriorite noeud = filePriorite.poll();
            resultat.nombreExtractions = resultat.nombreExtractions + 1;

            int   u         = noeud.id;
            float priorite  = noeud.priorite;

            if(dejaVisite[u]){
                continue;
            }

            // On ignore les entrées obsolètes : si on a trouvé mieux depuis, on passe.
            if(priorite > distance[u]){
                continue;
            }

            dejaVisite[u] = true;
            resultat.nombreNoeudsVisites = resultat.nombreNoeudsVisites + 1;

            if(u == arrivee){
                break;
            }

            // Parcours des voisins de u via les tableaux CSR.
            for(int i = offsetCSR[u]; i < offsetCSR[u + 1]; i = i + 1){
                int   v     = voisinsCSR[i];
                float poids = poidsCSR[i];

                if(dejaVisite[v]){
                    continue;
                }

                // Relaxation de l'arête u -> v.
                float nouvelleDistance = distance[u] + poids;

                if(nouvelleDistance < distance[v]){
                    distance[v]  = nouvelleDistance;
                    precedent[v] = u;
                    // Priorité = vraie distance (pas d'heuristique dans Dijkstra).
                    filePriorite.add(new NoeudPriorite(v, nouvelleDistance));
                    resultat.nombreRelaxations = resultat.nombreRelaxations + 1;
                }
            }
        }

        long tempsFin   = System.nanoTime();
        resultat.tempsMs = (tempsFin - tempsDebut) / 1000000.0;

        if(distance[arrivee] == Float.POSITIVE_INFINITY){
            return resultat;
        }

        resultat.cheminTrouve = true;
        resultat.distance     = distance[arrivee];
        resultat.reconstruireChemin(depart, arrivee, precedent, graphe);

        return resultat;
    }
}

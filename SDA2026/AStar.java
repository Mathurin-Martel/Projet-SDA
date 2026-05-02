import java.util.PriorityQueue;

/*
 * Classe AStar.
 *
 * A* améliore Dijkstra en orientant la recherche vers l'arrivée.
 * Au lieu d'explorer uniquement selon la distance déjà parcourue,
 * A* ajoute une estimation du chemin restant (l'heuristique).
 *
 * La priorité d'un noeud v dans la file devient :
 * distance[v] + distanceEuclidienne(v, arrivée)
 *
 * Cette estimation ne surestime jamais la vraie distance restante,
 * ce qui garantit que A* trouve bien le plus court chemin.
 *
 * Pour lire cet algorithme, comparez-le avec Dijkstra.java.
 * La seule différence est la priorité ajoutée à la file.
 */
public class AStar{

    private GrapheCSR graphe;

    public AStar(GrapheCSR graphe){
        this.graphe = graphe;
    }

    public ResultatChemin executer(String intersectionDepart, String intersectionArrivee){
        ResultatChemin resultat = new ResultatChemin("A*", intersectionDepart, intersectionArrivee);

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
        // Priorité initiale = 0 + estimation de la distance vers l'arrivée.
        filePriorite.add(new NoeudPriorite(depart, graphe.distanceEuclidienne(depart, arrivee)));

        long tempsDebut = System.nanoTime();

        while(!filePriorite.isEmpty()){
            NoeudPriorite noeud = filePriorite.poll();
            resultat.nombreExtractions = resultat.nombreExtractions + 1;

            int   u        = noeud.id;
            float priorite = noeud.priorite;

            if(dejaVisite[u]){
                continue;
            }

            // On ignore les entrées obsolètes.
            if(priorite > distance[u] + graphe.distanceEuclidienne(u, arrivee)){
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
                    // Priorité = vraie distance + estimation du chemin restant.
                    // C'est la seule ligne qui change par rapport à Dijkstra.
                    filePriorite.add(new NoeudPriorite(v, nouvelleDistance + graphe.distanceEuclidienne(v, arrivee)));
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
}

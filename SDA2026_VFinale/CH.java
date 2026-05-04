import java.io.*;
import java.util.*;

/**
 * Contraction Hierarchies (CH)
 *
 * Regroupe le prétraitement (anciennement Java_CH_donnees.java) et
 * la requête bidirectionnelle (anciennement Java_CH.java) dans une
 * seule classe intégrée au reste du projet.
 *
 * Usage :
 *   CH ch = new CH();
 *   ch.pretraitement("donnees_osmnx.csv");
 *   ResultatChemin r = ch.executer("25191346", "5574563073");
 */
public class CH {

    private HashMap<String, HashMap<String, CheminCH>> map;
    private ArrayList<String>       rang_vers_noeud;
    private HashMap<String, Integer> noeud_vers_rang;

    private double tempsPretraitementMs = 0.0;

    public CH() {
        map             = new HashMap<>();
        rang_vers_noeud = new ArrayList<>();
        noeud_vers_rang = new HashMap<>();
    }

    // =========================================================
    //  PRÉTRAITEMENT
    // =========================================================

    /**
     * Lance le pipeline complet de prétraitement CH :
     * lecture CSV → attribution des rangs → contraction → orientation.
     *
     * @param fichierGraphe chemin vers donnees_osmnx.csv
     */
    public void pretraitement(String fichierGraphe) throws IOException {
        System.out.println();
        System.out.println("--- Prétraitement CH ---");

        long debut = System.nanoTime();

        readCsv(fichierGraphe);
        donner_rang();
        contraction();
        orientation();

        long fin = System.nanoTime();
        tempsPretraitementMs = (fin - debut) / 1000000.0;

        System.out.println("Prétraitement CH terminé en " + tempsPretraitementMs + " ms");
        System.out.println("--- Fin du prétraitement CH ---");
    }

    /** Lecture du graphe brut (arêtes non orientées). */
    private void readCsv(String fichier) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fichier));
        reader.readLine(); // skip en-tête

        String ligne;
        while ((ligne = reader.readLine()) != null) {
            String[] col = ligne.split(";");
            if (col.length < 3) continue;

            for (int x = 0; x < col.length; x++) {
                col[x] = col[x].trim();
            }

            String depuis   = col[0];
            String vers     = col[1];
            float  distance = Float.parseFloat(col[2]);

            map.putIfAbsent(depuis, new HashMap<>());
            map.putIfAbsent(vers,   new HashMap<>());

            map.get(depuis).put(vers,   new CheminCH(distance, false, ""));
            map.get(vers).put(depuis,   new CheminCH(distance, false, ""));
        }

        reader.close();
    }

    /** Attribue un rang à chaque nœud (ordre croissant de degré). */
    private void donner_rang() {
        HashMap<Integer, ArrayList<String>> map_degre = new HashMap<>();

        for (String noeud : map.keySet()) {
            int degre = map.get(noeud).size();
            map_degre.putIfAbsent(degre, new ArrayList<>());
            map_degre.get(degre).add(noeud);
        }

        int rang = 0;
        for (int degre : map_degre.keySet()) {
            ArrayList<String> noeuds = map_degre.get(degre);
            for (int x = 0; x < noeuds.size(); x++) {
                String noeud = noeuds.get(x);
                rang_vers_noeud.add(noeud);
                noeud_vers_rang.put(noeud, rang);
                rang++;
            }
        }
    }

    /** Contraction : ajoute les raccourcis nécessaires. */
    private void contraction() {
        for (int x = 0; x < rang_vers_noeud.size(); x++) {
            System.out.println("Contraction noeud de rang " + x);
            String v = rang_vers_noeud.get(x);

            // Les nœuds très connectés sont ignorés (trop coûteux)
            if (map.get(v).size() > 15) {
                System.out.println("Skip noeud de rang " + x);
                continue;
            }

            Set<String> voisins = new HashSet<>(map.get(v).keySet());

            for (String u : voisins) {
                float distance_max = -1f;

                for (String w : voisins) {
                    if (!u.equals(w)) {
                        float cout = map.get(v).get(u).getDistance()
                                   + map.get(v).get(w).getDistance();
                        if (distance_max == -1f || cout < distance_max) {
                            distance_max = cout;
                        }
                    }
                }

                HashMap<String, Float> distances = dijkstraLimite(u, v, distance_max);

                for (String w : voisins) {
                    if (u.equals(w)) continue;

                    float coutRaccourci = map.get(v).get(u).getDistance()
                                        + map.get(v).get(w).getDistance();
                    Float distUW = distances.get(w);

                    if (distUW == null || distUW > coutRaccourci) {
                        map.get(u).put(w, new CheminCH(coutRaccourci, true, v));
                        map.get(w).put(u, new CheminCH(coutRaccourci, true, v));
                    }
                }
            }
        }
    }

    /** Dijkstra limité utilisé pendant la contraction (évite le nœud contracté). */
    private HashMap<String, Float> dijkstraLimite(String depart,
                                                   String noeud_a_eviter,
                                                   float  distance_max) {
        HashMap<String, Float> dist = new HashMap<>();
        PriorityQueue<PaireCH> pq  = new PriorityQueue<>(Comparator.comparingDouble(p -> p.distance));

        dist.put(depart, 0f);
        pq.add(new PaireCH(depart, 0f));

        while (!pq.isEmpty()) {
            PaireCH current = pq.poll();

            if (current.distance > distance_max) break;
            if (current.distance > dist.get(current.noeud)) continue;

            for (String voisin : map.get(current.noeud).keySet()) {
                if (voisin.equals(noeud_a_eviter)) continue;

                float newDist = current.distance
                              + map.get(current.noeud).get(voisin).getDistance();

                if (newDist > distance_max) continue;

                if (!dist.containsKey(voisin) || dist.get(voisin) > newDist) {
                    dist.put(voisin, newDist);
                    pq.add(new PaireCH(voisin, newDist));
                }
            }
        }

        return dist;
    }

    /** Oriente le graphe : ne garde que les arêtes vers les nœuds de rang supérieur. */
    private void orientation() {
        HashMap<String, HashMap<String, CheminCH>> newMap = new HashMap<>();

        for (String u : map.keySet()) {
            newMap.put(u, new HashMap<>());
        }

        for (String u : map.keySet()) {
            for (String v : map.get(u).keySet()) {
                if (noeud_vers_rang.get(u) < noeud_vers_rang.get(v)) {
                    newMap.get(u).put(v, map.get(u).get(v));
                }
            }
        }

        map = newMap;
    }

    /**
     * Exporte le graphe contracté vers un fichier CSV
     * (même format que donnee_CH.csv produit par Java_CH_donnees).
     */
    public void exporterCSV(String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write("from;to;distance;isShortcut;via\n");

        for (String noeud1 : map.keySet()) {
            for (String noeud2 : map.get(noeud1).keySet()) {
                CheminCH c = map.get(noeud1).get(noeud2);
                writer.write(noeud1 + ";" + noeud2 + ";"
                           + c.getDistance()     + ";"
                           + c.estRaccourci()    + ";"
                           + c.getRaccourciPar() + "\n");
            }
        }

        writer.close();
        System.out.println("Fichier créé : " + filename);
    }

    // =========================================================
    //  REQUÊTE  — Dijkstra bidirectionnel sur le graphe CH
    // =========================================================

    public ResultatChemin executer(String depart, String arrivee) {
        ResultatChemin resultat = new ResultatChemin("CH", depart, arrivee);

        if (!noeud_vers_rang.containsKey(depart)) {
            System.out.println("Erreur : noeud de départ inconnu : " + depart);
            return resultat;
        }

        if (!noeud_vers_rang.containsKey(arrivee)) {
            System.out.println("Erreur : noeud d'arrivée inconnu : " + arrivee);
            return resultat;
        }

        HashMap<String, Float>  dist_avant     = new HashMap<>();
        HashMap<String, Float>  dist_arriere   = new HashMap<>();
        HashMap<String, String> parent_avant   = new HashMap<>();
        HashMap<String, String> parent_arriere = new HashMap<>();
        HashSet<String>         visites_avant  = new HashSet<>();
        HashSet<String>         visites_arriere = new HashSet<>();

        PriorityQueue<PaireCH> pq_avant   = new PriorityQueue<>(Comparator.comparingDouble(p -> p.distance));
        PriorityQueue<PaireCH> pq_arriere = new PriorityQueue<>(Comparator.comparingDouble(p -> p.distance));

        dist_avant.put(depart, 0f);
        dist_arriere.put(arrivee, 0f);
        parent_avant.put(depart, null);
        parent_arriere.put(arrivee, null);

        pq_avant.add(new PaireCH(depart,  0f));
        pq_arriere.add(new PaireCH(arrivee, 0f));

        float  meilleure   = Float.MAX_VALUE;
        String noeudMilieu = null;

        long tempsDebut = System.nanoTime();

        while (!pq_avant.isEmpty() || !pq_arriere.isEmpty()) {

            // --- Recherche avant ---
            if (!pq_avant.isEmpty()) {
                PaireCH p = pq_avant.poll();
                String  u = p.noeud;
                resultat.nombreExtractions++;

                if (!map.containsKey(u))   continue;
                if (!visites_avant.add(u)) continue;

                resultat.nombreNoeudsVisites++;

                float distU = dist_avant.get(u);
                if (distU > meilleure) continue;

                if (dist_arriere.containsKey(u)) {
                    float total = distU + dist_arriere.get(u);
                    if (total < meilleure) {
                        meilleure    = total;
                        noeudMilieu  = u;
                    }
                }

                for (String v : map.get(u).keySet()) {
                    float newDist = distU + map.get(u).get(v).getDistance();

                    if (!dist_avant.containsKey(v) || dist_avant.get(v) > newDist) {
                        dist_avant.put(v, newDist);
                        parent_avant.put(v, u);
                        pq_avant.add(new PaireCH(v, newDist));
                        resultat.nombreRelaxations++;
                    }
                }
            }

            // --- Recherche arrière ---
            if (!pq_arriere.isEmpty()) {
                PaireCH p = pq_arriere.poll();
                String  u = p.noeud;
                resultat.nombreExtractions++;

                if (!map.containsKey(u))     continue;
                if (!visites_arriere.add(u)) continue;

                resultat.nombreNoeudsVisites++;

                float distU = dist_arriere.get(u);
                if (distU > meilleure) continue;

                if (dist_avant.containsKey(u)) {
                    float total = distU + dist_avant.get(u);
                    if (total < meilleure) {
                        meilleure   = total;
                        noeudMilieu = u;
                    }
                }

                for (String v : map.get(u).keySet()) {
                    float newDist = distU + map.get(u).get(v).getDistance();

                    if (!dist_arriere.containsKey(v) || dist_arriere.get(v) > newDist) {
                        dist_arriere.put(v, newDist);
                        parent_arriere.put(v, u);
                        pq_arriere.add(new PaireCH(v, newDist));
                        resultat.nombreRelaxations++;
                    }
                }
            }
        }

        long tempsFin    = System.nanoTime();
        resultat.tempsMs = (tempsFin - tempsDebut) / 1000000.0;

        if (noeudMilieu == null) {
            return resultat;
        }

        resultat.cheminTrouve = true;
        resultat.distance     = meilleure;

        // Reconstruction du chemin
        ArrayList<String> chemin = new ArrayList<>();

        String cur = noeudMilieu;
        while (cur != null) {
            chemin.add(cur);
            cur = parent_avant.get(cur);
        }
        Collections.reverse(chemin);

        cur = parent_arriere.get(noeudMilieu);
        while (cur != null) {
            chemin.add(cur);
            cur = parent_arriere.get(cur);
        }

        resultat.chemin = chemin;

        return resultat;
    }

    // =========================================================
    //  ACCESSEURS
    // =========================================================

    public String getNomAlgorithme()        { return "CH"; }
    public double getTempsPretraitementMs() { return tempsPretraitementMs; }

    // =========================================================
    //  CLASSES INTERNES  (privées — aucun conflit de nommage)
    // =========================================================

    private static class CheminCH {
        private final float   distance;
        private final boolean estRaccourci;
        private final String  raccourciPar;

        CheminCH(float distance, boolean estRaccourci, String raccourciPar) {
            this.distance     = distance;
            this.estRaccourci = estRaccourci;
            this.raccourciPar = raccourciPar;
        }

        float   getDistance()    { return distance;     }
        boolean estRaccourci()   { return estRaccourci; }
        String  getRaccourciPar(){ return raccourciPar; }
    }

    private static class PaireCH {
        final String noeud;
        final float  distance;

        PaireCH(String noeud, float distance) {
            this.noeud    = noeud;
            this.distance = distance;
        }
    }
}

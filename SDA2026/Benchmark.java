import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.io.FileWriter;
import java.io.PrintWriter;

/*
 * Classe Benchmark.
 *
 * Son rôle est de tester les algorithmes sur un grand nombre de requêtes
 * et de produire trois fichiers CSV pour le rapport :
 *
 * 1) resultats_benchmark.csv : toutes les requêtes, ligne par ligne.
 * 2) resume_benchmark.csv    : moyennes, p50, p95 et débit par algo.
 * 3) resultats_profiling.csv : mémoire CSR + coût de prétraitement ALT.
 */
public class Benchmark{
    private GrapheCSR graphe;
    private Dijkstra dijkstra;
    private AStar astar;

    // Réglages du benchmark.
    private static final int[] TESTS_LANDMARKS    = {2, 4, 8}; // variantes ALT à tester
    private static final int   REQUETES_PAR_TYPE  = 30;        // 30 courtes, 30 moyennes, 30 longues
    private static final int   ESSAIS_MAXIMUM     = 50000;     // garde-fou contre une boucle infinie
    private static final int   GRAINE_RANDOM      = 42;        // graine fixe = résultats reproductibles

    public Benchmark(GrapheCSR graphe, Dijkstra dijkstra, AStar astar){
        this.graphe   = graphe;
        this.dijkstra = dijkstra;
        this.astar    = astar;
    }

    // -------------------------------------------------------------------------
    // Point d'entrée
    // -------------------------------------------------------------------------

    public void lancer(){
        System.out.println();
        System.out.println("--- Benchmark ---");

        try{
            ALT[] variantesALT = preparerVariantesALT();

            // On ouvre le CSV détaillé et on lance toutes les requêtes.
            // Les accumulateurs collectent les stats au fil des requêtes,
            // sans avoir à relire le CSV après coup.
            PrintWriter writer = new PrintWriter(new FileWriter("resultats_benchmark.csv"));
            writer.println("id_requete;type_requete;depart;arrivee;algorithme;nombre_landmarks;chemin_trouve;distance;temps_ms;noeuds_visites;relaxations;extractions;correct_vs_dijkstra");

            HashMap<String, StatAccumulateur> accumulateurs = new HashMap<>();
            lancerRequetes(writer, variantesALT, accumulateurs);
            writer.close();

            System.out.println("Fichier créé : resultats_benchmark.csv");

            // Le résumé se calcule directement depuis les accumulateurs en mémoire.
            ecrireResume(accumulateurs, variantesALT);
            ecrireProfiling(variantesALT);

            System.out.println("-----------------");
        }
        catch(Exception e){
            System.out.println("Erreur benchmark : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Préparation des variantes ALT
    // -------------------------------------------------------------------------

    private ALT[] preparerVariantesALT(){
        ALT[] variantes = new ALT[TESTS_LANDMARKS.length];

        System.out.println("Préparation des variantes ALT : 2, 4 et 8 landmarks.");

        for(int i = 0; i < TESTS_LANDMARKS.length; i = i + 1){
            variantes[i] = new ALT(graphe);
            variantes[i].pretraitement(TESTS_LANDMARKS[i]);
        }

        return variantes;
    }

    // -------------------------------------------------------------------------
    // Boucle principale des requêtes
    // -------------------------------------------------------------------------

    private void lancerRequetes(PrintWriter writer, ALT[] variantesALT, HashMap<String, StatAccumulateur> accumulateurs){
        Random random = new Random(GRAINE_RANDOM);

        // Compteur de requêtes validées par catégorie.
        HashMap<String, Integer> compteTypes = new HashMap<>();
        compteTypes.put("courte",  0);
        compteTypes.put("moyenne", 0);
        compteTypes.put("longue",  0);

        int idRequete = 0;
        int essais    = 0;

        while(!benchmarkComplet(compteTypes) && essais < ESSAIS_MAXIMUM){
            essais = essais + 1;

            int idDepart  = random.nextInt(graphe.getNombreNoeuds());
            int idArrivee = random.nextInt(graphe.getNombreNoeuds());

            if(idDepart == idArrivee){
                continue;
            }

            String depart  = graphe.getNomNoeud(idDepart);
            String arrivee = graphe.getNomNoeud(idArrivee);

            // Dijkstra sert de référence : si lui ne trouve pas de chemin, on ignore la requête.
            ResultatChemin resDijkstra = dijkstra.executer(depart, arrivee);

            if(!resDijkstra.cheminTrouve){
                continue;
            }

            // On classe la requête selon sa distance.
            String type = typeRequete(resDijkstra.distance);

            if(compteTypes.get(type) >= REQUETES_PAR_TYPE){
                continue;
            }

            // La requête est retenue : on lance les autres algorithmes.
            ResultatChemin resAStar = astar.executer(depart, arrivee);

            ArrayList<ResultatChemin> resALT = new ArrayList<>();
            for(int i = 0; i < variantesALT.length; i = i + 1){
                resALT.add(variantesALT[i].executer(depart, arrivee));
            }

            idRequete = idRequete + 1;
            compteTypes.put(type, compteTypes.get(type) + 1);

            // Écriture dans le CSV détaillé + accumulation des stats.
            ecrireLigneEtAccumuler(writer, accumulateurs, idRequete, type, depart, arrivee, 0, resDijkstra, resDijkstra);
            ecrireLigneEtAccumuler(writer, accumulateurs, idRequete, type, depart, arrivee, 0, resAStar,    resDijkstra);

            for(int i = 0; i < variantesALT.length; i = i + 1){
                ecrireLigneEtAccumuler(writer, accumulateurs, idRequete, type, depart, arrivee,
                    variantesALT[i].getNombreLandmarksUtilises(), resALT.get(i), resDijkstra);
            }
        }

        System.out.println("Requêtes retenues : " + idRequete
            + "  (courtes=" + compteTypes.get("courte")
            + ", moyennes=" + compteTypes.get("moyenne")
            + ", longues=" + compteTypes.get("longue") + ")");
    }

    // -------------------------------------------------------------------------
    // Écriture d'une ligne CSV + accumulation immédiate des stats
    // -------------------------------------------------------------------------

    /*
     * On écrit la ligne dans le CSV détaillé ET on met à jour l'accumulateur
     * correspondant à ce (type, algorithme) en une seule passe.
     * Ça évite d'avoir à relire le CSV après coup pour calculer les moyennes.
     */
    private void ecrireLigneEtAccumuler(PrintWriter writer, HashMap<String, StatAccumulateur> accumulateurs,
            int idRequete, String type, String depart, String arrivee,
            int nombreLandmarks, ResultatChemin resultat, ResultatChemin reference){

        boolean correct = resultat.cheminTrouve && reference.cheminTrouve
            && Math.abs(resultat.distance - reference.distance) <= 0.01f;

        writer.println(
            idRequete             + ";" +
            type                  + ";" +
            depart                + ";" +
            arrivee               + ";" +
            resultat.nomAlgorithme + ";" +
            nombreLandmarks       + ";" +
            resultat.cheminTrouve + ";" +
            resultat.distance     + ";" +
            resultat.tempsMs      + ";" +
            resultat.nombreNoeudsVisites + ";" +
            resultat.nombreRelaxations   + ";" +
            resultat.nombreExtractions   + ";" +
            correct
        );

        // Clé unique pour ce groupe (type × algorithme).
        String cle = type + "|" + resultat.nomAlgorithme;

        if(!accumulateurs.containsKey(cle)){
            accumulateurs.put(cle, new StatAccumulateur());
        }

        accumulateurs.get(cle).ajouter(resultat);
    }

    // -------------------------------------------------------------------------
    // CSV résumé
    // -------------------------------------------------------------------------

    private void ecrireResume(HashMap<String, StatAccumulateur> accumulateurs, ALT[] variantesALT) throws Exception{
        PrintWriter writer = new PrintWriter(new FileWriter("resume_benchmark.csv"));
        writer.println("type_requete;algorithme;nombre_lignes;temps_moyen_ms;p50_ms;p95_ms;debit_requetes_par_seconde;noeuds_moyens;relaxations_moyennes;extractions_moyennes");

        // On impose un ordre fixe pour que le CSV soit facile à lire.
        String[] types = {"courte", "moyenne", "longue"};

        // On construit la liste des noms d'algorithmes dans l'ordre voulu.
        ArrayList<String> algos = new ArrayList<>();
        algos.add("DIJKSTRA");
        algos.add("A*");
        for(int i = 0; i < variantesALT.length; i = i + 1){
            algos.add(variantesALT[i].getNomAlgorithme());
        }

        for(int i = 0; i < types.length; i = i + 1){
            for(int j = 0; j < algos.size(); j = j + 1){
                String cle = types[i] + "|" + algos.get(j);

                if(!accumulateurs.containsKey(cle)){
                    continue;
                }

                StatAccumulateur acc = accumulateurs.get(cle);
                int    n             = acc.compte;
                double tempsMoyen    = acc.sommeTemps       / n;
                double noeudsMoyens  = acc.sommeNoeuds      / n;
                double relaxMoyennes = acc.sommeRelaxations / n;
                double extrMoyennes  = acc.sommeExtractions / n;
                double p50           = calculerPercentile(acc.listeTemps, 50);
                double p95           = calculerPercentile(acc.listeTemps, 95);
                double debit         = tempsMoyen > 0 ? 1000.0 / tempsMoyen : 0.0;

                writer.println(
                    types[i]                   + ";" +
                    algos.get(j)               + ";" +
                    n                          + ";" +
                    fmt(tempsMoyen)            + ";" +
                    fmt(p50)                   + ";" +
                    fmt(p95)                   + ";" +
                    fmt(debit)                 + ";" +
                    fmt(noeudsMoyens)          + ";" +
                    fmt(relaxMoyennes)         + ";" +
                    fmt(extrMoyennes)
                );
            }
        }

        writer.close();
        System.out.println("Fichier créé : resume_benchmark.csv");
    }

    // -------------------------------------------------------------------------
    // CSV profiling
    // -------------------------------------------------------------------------

    private void ecrireProfiling(ALT[] variantesALT) throws Exception{
        PrintWriter writer = new PrintWriter(new FileWriter("resultats_profiling.csv"));
        writer.println("element;valeur;unite;details");

        long memoireCSR = graphe.getMemoireGrapheCSROctets();

        writer.println("nombre_noeuds;"  + graphe.getNombreNoeuds()  + ";noeuds;taille du graphe");
        writer.println("nombre_aretes;"  + graphe.getNombreAretes()  + ";aretes;taille du graphe");
        writer.println("memoire_csr;"    + memoireCSR                + ";octets;offsetCSR + voisinsCSR + poidsCSR");
        writer.println("memoire_csr_mo;" + fmt(memoireCSR / (1024.0 * 1024.0)) + ";Mo;memoire compacte du graphe");

        for(int i = 0; i < variantesALT.length; i = i + 1){
            ALT alt      = variantesALT[i];
            int nb       = alt.getNombreLandmarksUtilises();
            long memALT  = alt.getMemoireAdditionnelleALTOctets();

            writer.println("pretraitement_alt_" + nb + ";" + fmt(alt.getTempsPretraitementMs()) + ";ms;calcul distances depuis " + nb + " landmarks");
            writer.println("memoire_alt_"        + nb + ";" + memALT + ";octets;distances landmarks pour " + nb + " landmarks");
            writer.println("memoire_alt_"        + nb + "_mo;" + fmt(memALT / (1024.0 * 1024.0)) + ";Mo;memoire supplementaire ALT " + nb + " landmarks");
        }

        writer.close();
        System.out.println("Fichier créé : resultats_profiling.csv");
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    private boolean benchmarkComplet(HashMap<String, Integer> compteTypes){
        return compteTypes.get("courte")  >= REQUETES_PAR_TYPE
            && compteTypes.get("moyenne") >= REQUETES_PAR_TYPE
            && compteTypes.get("longue")  >= REQUETES_PAR_TYPE;
    }

    private String typeRequete(float distance){
        if(distance < 2000f)  return "courte";
        if(distance < 6000f)  return "moyenne";
        return "longue";
    }

    private double calculerPercentile(ArrayList<Double> valeurs, int pourcentage){
        ArrayList<Double> copie = new ArrayList<>(valeurs);
        Collections.sort(copie);

        int position = (int) Math.ceil((pourcentage / 100.0) * copie.size()) - 1;
        position = Math.max(0, Math.min(position, copie.size() - 1));

        return copie.get(position);
    }

    // fmt = formater un double avec 4 décimales et un point comme séparateur (pour les CSV).
    private String fmt(double valeur){
        return String.format(java.util.Locale.US, "%.4f", valeur);
    }

    // -------------------------------------------------------------------------
    // Classe interne : accumule les stats pour un groupe (type × algorithme)
    // -------------------------------------------------------------------------

    /*
     * Au lieu de relire le CSV après coup, on accumule les stats
     * directement pendant la boucle de requêtes.
     *
     * Chaque instance correspond à un groupe (ex : "courte | A*").
     */
    private static class StatAccumulateur{
        int    compte          = 0;
        double sommeTemps      = 0;
        double sommeNoeuds     = 0;
        double sommeRelaxations = 0;
        double sommeExtractions = 0;
        ArrayList<Double> listeTemps = new ArrayList<>();

        void ajouter(ResultatChemin r){
            compte           = compte + 1;
            sommeTemps       = sommeTemps       + r.tempsMs;
            sommeNoeuds      = sommeNoeuds      + r.nombreNoeudsVisites;
            sommeRelaxations = sommeRelaxations + r.nombreRelaxations;
            sommeExtractions = sommeExtractions + r.nombreExtractions;
            listeTemps.add(r.tempsMs);
        }
    }
}

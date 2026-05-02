import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * Classe GrapheCSR — représentation compacte du réseau routier.
 *
 * Le chargement se fait en deux appels depuis Main :
 *
 *   graphe.charger(fichierAretes)      — lit le CSV et construit les tableaux CSR.
 *   graphe.lireCoordonnees(fichierXY)  — charge les coordonnées pour A*.
 *
 * En interne, charger() utilise une structure temporaire (HashMap) pour lire
 * le CSV, puis la convertit en trois tableaux compacts (format CSR) :
 *
 *   offsetCSR  [N+1]  : offsetCSR[u] = indice de début des voisins de u
 *                        offsetCSR[u+1] = indice de fin
 *   voisinsCSR [E]    : tous les voisins bout à bout
 *   poidsCSR   [E]    : le poids de chaque arête correspondante
 *
 * Exemple avec 3 noeuds (0->1 poids 5, 0->2 poids 3, 1->2 poids 7) :
 *
 *   offsetCSR  = [ 0,  2,  3,  3 ]
 *   voisinsCSR = [ 1,  2,  2 ]
 *   poidsCSR   = [ 5,  3,  7 ]
 *
 * Pour lire les voisins du noeud u :
 *   de offsetCSR[u] à offsetCSR[u+1] (exclu) dans voisinsCSR / poidsCSR.
 */
public class GrapheCSR{

    // -------------------------------------------------------------------------
    // Correspondance identifiants OSM <-> indices entiers
    // -------------------------------------------------------------------------

    // idNoeud : identifiant OSM (String) -> indice entier dans les tableaux CSR.
    private HashMap<String, Integer> idNoeud;

    // nomNoeud : indice entier -> identifiant OSM (String). Inverse de idNoeud.
    private ArrayList<String> nomNoeud;

    // -------------------------------------------------------------------------
    // Tableaux CSR — la vraie représentation compacte du graphe
    // -------------------------------------------------------------------------

    private int[]   offsetCSR;
    private int[]   voisinsCSR;
    private float[] poidsCSR;

    // -------------------------------------------------------------------------
    // Coordonnées des noeuds (pour l'heuristique de A*)
    // -------------------------------------------------------------------------

    private HashMap<String, float[]> coordonnees;

    // -------------------------------------------------------------------------
    // Constructeur
    // -------------------------------------------------------------------------

    public GrapheCSR(){
        idNoeud     = new HashMap<>();
        nomNoeud    = new ArrayList<>();
        coordonnees = new HashMap<>();
    }

    // =========================================================================
    // Chargement du graphe (CSV arêtes -> tableaux CSR)
    // =========================================================================

    /*
     * Lit le fichier CSV des arêtes et construit directement les tableaux CSR.
     *
     * On passe par une HashMap temporaire (adjacenceTemp) pour lire le CSV,
     * puis on la convertit en tableaux compacts. La HashMap est supprimée
     * à la fin de cette méthode.
     *
     * Format CSV attendu (séparateur point-virgule) :
     *   Depuis ; Vers ; Distance ; NomRue (optionnel)
     */
    public void charger(String fichier) throws IOException{

        // --- Étape 1 : lecture du CSV dans la structure temporaire ---

        // adjacenceTemp : pour chaque noeud OSM, ses voisins OSM avec leur poids.
        // Si plusieurs arêtes relient les mêmes noeuds, on garde la plus courte.
        HashMap<String, HashMap<String, Float>> adjacenceTemp = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader(fichier));
        reader.readLine(); // ignorer l'en-tête

        String ligne;
        while((ligne = reader.readLine()) != null){
            String[] colonnes = ligne.split(";");

            if(colonnes.length < 3){
                continue;
            }

            String depuis   = colonnes[0].trim();
            String vers     = colonnes[1].trim();
            float  distance = Float.parseFloat(colonnes[2].trim());

            // On s'assure que les deux noeuds existent dans la map.
            adjacenceTemp.putIfAbsent(depuis, new HashMap<>());
            adjacenceTemp.putIfAbsent(vers,   new HashMap<>());

            // On garde uniquement la plus courte arête entre deux noeuds.
            HashMap<String, Float> voisins = adjacenceTemp.get(depuis);
            if(!voisins.containsKey(vers) || voisins.get(vers) > distance){
                voisins.put(vers, distance);
            }
        }

        reader.close();

        // --- Étape 2 : attribution d'un indice entier à chaque noeud ---

        idNoeud.clear();
        nomNoeud.clear();

        for(String noeud : adjacenceTemp.keySet()){
            idNoeud.put(noeud, nomNoeud.size());
            nomNoeud.add(noeud);
        }

        // --- Étape 3 : allocation et remplissage des tableaux CSR ---

        int nombreNoeuds = nomNoeud.size();
        int nombreAretes = 0;

        for(String noeud : adjacenceTemp.keySet()){
            nombreAretes = nombreAretes + adjacenceTemp.get(noeud).size();
        }

        offsetCSR  = new int  [nombreNoeuds + 1];
        voisinsCSR = new int  [nombreAretes];
        poidsCSR   = new float[nombreAretes];

        int position = 0;

        for(int u = 0; u < nomNoeud.size(); u = u + 1){
            offsetCSR[u] = position; // les voisins de u commencent ici

            HashMap<String, Float> voisins = adjacenceTemp.get(nomNoeud.get(u));

            for(String voisin : voisins.keySet()){
                voisinsCSR[position] = idNoeud.get(voisin);
                poidsCSR  [position] = voisins.get(voisin);
                position = position + 1;
            }
        }

        offsetCSR[nomNoeud.size()] = position; // sentinelle de fin

        // adjacenceTemp n'est plus nécessaire : les algorithmes utilisent les tableaux CSR.
        // Elle sera libérée par le ramasse-miettes à la fin de cette méthode.

        System.out.println();
        System.out.println("--- Construction CSR terminée ---");
        System.out.println("Noeuds  = " + nomNoeud.size());
        System.out.println("Arêtes  = " + voisinsCSR.length);
        System.out.println("---------------------------------");
    }

    // =========================================================================
    // Chargement des coordonnées (optionnel, pour A*)
    // =========================================================================

    /*
     * Lit le fichier CSV des coordonnées projetées (x, y en mètres).
     *
     * Format attendu :
     *   Intersection ; X ; Y
     */
    public void lireCoordonnees(String fichier) throws IOException{
        coordonnees.clear();

        BufferedReader reader = new BufferedReader(new FileReader(fichier));
        reader.readLine(); // ignorer l'en-tête

        String ligne;
        while((ligne = reader.readLine()) != null){
            String[] colonnes = ligne.split(";");

            if(colonnes.length < 3){
                continue;
            }

            String noeud = colonnes[0].trim();
            float  x     = Float.parseFloat(colonnes[1].trim());
            float  y     = Float.parseFloat(colonnes[2].trim());

            coordonnees.put(noeud, new float[]{x, y});
        }

        reader.close();
    }

    /*
     * Distance euclidienne entre deux noeuds (en mètres, coordonnées projetées).
     * Utilisée comme heuristique par A* et ALT.
     * Retourne 0 si les coordonnées sont absentes.
     */
    public float distanceEuclidienne(int id1, int id2){
        float[] c1 = coordonnees.get(nomNoeud.get(id1));
        float[] c2 = coordonnees.get(nomNoeud.get(id2));

        if(c1 == null || c2 == null){
            return 0f;
        }

        float dx = c1[0] - c2[0];
        float dy = c1[1] - c2[1];

        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // =========================================================================
    // Accesseurs
    // =========================================================================

    public boolean contientNoeud(String nom) { return idNoeud.containsKey(nom); }
    public int     getIdNoeud(String nom)    { return idNoeud.get(nom); }
    public String  getNomNoeud(int id)       { return nomNoeud.get(id); }
    public int     getNombreNoeuds()         { return nomNoeud.size(); }
    public int     getNombreAretes()         { return voisinsCSR.length; }
    public int[]   getOffsetCSR()            { return offsetCSR; }
    public int[]   getVoisinsCSR()           { return voisinsCSR; }
    public float[] getPoidsCSR()             { return poidsCSR; }

    // Estimation de la mémoire occupée par les trois tableaux CSR.
    public long getMemoireGrapheCSROctets(){
        return (long) offsetCSR.length  * Integer.BYTES
             + (long) voisinsCSR.length * Integer.BYTES
             + (long) poidsCSR.length   * Float.BYTES;
    }
}

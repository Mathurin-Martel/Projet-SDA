import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GrapheCSR{

    private HashMap<String, Integer> idNoeud;

    private ArrayList<String> nomNoeud;

    private int[]   offsetCSR;
    private int[]   voisinsCSR;
    private float[] poidsCSR;

    private HashMap<String, float[]> coordonnees;

    public GrapheCSR(){
        idNoeud     = new HashMap<>();
        nomNoeud    = new ArrayList<>();
        coordonnees = new HashMap<>();
    }

    public void charger(String fichier) throws IOException{

        HashMap<String, HashMap<String, Float>> adjacenceTemp = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader(fichier));
        reader.readLine();

        String ligne;
        while((ligne = reader.readLine()) != null){
            String[] colonnes = ligne.split(";");

            if(colonnes.length < 3){
                continue;
            }

            String depuis   = colonnes[0].trim();
            String vers     = colonnes[1].trim();
            float  distance = Float.parseFloat(colonnes[2].trim());

            adjacenceTemp.putIfAbsent(depuis, new HashMap<>());
            adjacenceTemp.putIfAbsent(vers,   new HashMap<>());

            HashMap<String, Float> voisins = adjacenceTemp.get(depuis);
            if(!voisins.containsKey(vers) || voisins.get(vers) > distance){
                voisins.put(vers, distance);
            }
        }

        reader.close();

        idNoeud.clear();
        nomNoeud.clear();

        for(String noeud : adjacenceTemp.keySet()){
            idNoeud.put(noeud, nomNoeud.size());
            nomNoeud.add(noeud);
        }

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
            offsetCSR[u] = position;

            HashMap<String, Float> voisins = adjacenceTemp.get(nomNoeud.get(u));

            for(String voisin : voisins.keySet()){
                voisinsCSR[position] = idNoeud.get(voisin);
                poidsCSR  [position] = voisins.get(voisin);
                position = position + 1;
            }
        }

        offsetCSR[nomNoeud.size()] = position;

        System.out.println();
        System.out.println("--- Construction CSR terminée ---");
        System.out.println("Noeuds  = " + nomNoeud.size());
        System.out.println("Arêtes  = " + voisinsCSR.length);
        System.out.println("---------------------------------");
    }

    public void lireCoordonnees(String fichier) throws IOException{
        coordonnees.clear();

        BufferedReader reader = new BufferedReader(new FileReader(fichier));
        reader.readLine();

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

    public boolean contientNoeud(String nom) { return idNoeud.containsKey(nom); }
    public int     getIdNoeud(String nom)    { return idNoeud.get(nom); }
    public String  getNomNoeud(int id)       { return nomNoeud.get(id); }
    public int     getNombreNoeuds()         { return nomNoeud.size(); }
    public int     getNombreAretes()         { return voisinsCSR.length; }
    public int[]   getOffsetCSR()            { return offsetCSR; }
    public int[]   getVoisinsCSR()           { return voisinsCSR; }
    public float[] getPoidsCSR()             { return poidsCSR; }

    public long getMemoireGrapheCSROctets(){
        return (long) offsetCSR.length  * Integer.BYTES
             + (long) voisinsCSR.length * Integer.BYTES
             + (long) poidsCSR.length   * Float.BYTES;
    }
}

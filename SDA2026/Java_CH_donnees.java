import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.lang.*;

public class Java_CH_donnees{
    HashMap<String, HashMap<String, Chemin>> map = new HashMap<>();
    ArrayList<String> rang_vers_noeud = new ArrayList<>();
    HashMap<String, Integer> noeud_vers_rang = new HashMap<>();
    HashMap<String, Float> tableau_dijkstra_limite=new HashMap<>();

    public static void main(String[] args){
        Java_CH_donnees ch= new Java_CH_donnees();
        ch.CH_Preparation();
    }

    public void CH_Preparation(){
        //Etape extraction des données
        try{ readCsv("donnees_osmnx.csv");}
        catch(IOException e){System.out.println("Erreur");}

        //Etape donner rang
        donner_rang();

        //Etape Contraction
        contraction();

        //Etape Orientation
        orientation();

        //Etape Exportation CSV
        try{exporter_CSV_chemins("donnee_CH.csv");}
        catch(IOException e){System.out.println("Erreur");}

    }

    public void readCsv(String fichier) throws IOException{
        File file = new File(fichier); //Mettre donnees_osmnx.csv pour le rendu final
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        String[] tokenizedLine={};
        
        line = bufferedReader.readLine(); //pour skip la première ligne

        while((line = bufferedReader.readLine()) != null){
            tokenizedLine = line.split(";");
            //tokenizedLine[0]=Intersection départ
            //tokenizedLine[1]=Intersection arrivée
            //tokenizedLine[2]=Distance
            //tokenizedLine[3]=Rue (Est des fois=Null)
            for(int x=0;x<tokenizedLine.length;x=x+1){
                tokenizedLine[x]=tokenizedLine[x].trim(); //.strip() si version de java supérieur a 1.11
            }

            if(!(map.containsKey(tokenizedLine[0]))){
                map.put(tokenizedLine[0], new HashMap<String, Chemin>()); //Dans le sens A vers B
            }
            if(!(map.containsKey(tokenizedLine[1]))){
                map.put(tokenizedLine[1], new HashMap<String, Chemin>()); //Dans le sens B vers A
            }

            map.get(tokenizedLine[0]).put(tokenizedLine[1], new Chemin(Float.parseFloat(tokenizedLine[2]), false, ""));
            map.get(tokenizedLine[1]).put(tokenizedLine[0], new Chemin(Float.parseFloat(tokenizedLine[2]), false, ""));
        }
        /*for (String key : map.keySet()){
            for (int x=0; x<map.get(key).size(); x=x+1){
                System.out.println(map.get(key).get(x).toString());
            }
        }*/
        bufferedReader.close();
    }

    public void donner_rang(){
        HashMap<Integer, ArrayList<String>> map_degre=new HashMap<>();
        for (String key : map.keySet()){
            int degre=map.get(key).size();
            if(!(map_degre.containsKey(degre))){
                map_degre.put(degre, new ArrayList<String>());
            }
            map_degre.get(degre).add(key);
        }
        int rang=0;
        for (int degre : map_degre.keySet()){
            for(int x=0;x<map_degre.get(degre).size();x=x+1){
                rang_vers_noeud.add(map_degre.get(degre).get(x));
                noeud_vers_rang.put(map_degre.get(degre).get(x), rang);
                rang=rang+1;
            }
        }
    }

    public void contraction(){
        for (int x = 0; x < rang_vers_noeud.size(); x++){
            System.out.println("Noeud de rang " + x);
            String v = rang_vers_noeud.get(x);
            if (map.get(v).size() > 15){//Si le noeud a plus de 15 voisins on s'en occupe pas. Ca prend trop de temps. Surtout qu'a la fin il peuvent avoir +1000 voisins.
                System.out.println("Skip noeud de rang " + x);
                continue;
            }
            Set<String> voisins = new HashSet<>(map.get(v).keySet());
            for (String u : voisins){
                float distance_max = -1f;
                for (String w : voisins){
                    if (!u.equals(w)){
                        float cout = map.get(v).get(u).get_distance() + map.get(v).get(w).get_distance();
                        if (distance_max==-1f || (cout < distance_max)){
                            distance_max = cout;
                        }
                    }
                }
                HashMap<String, Float> distances = dijkstraLimite(u, v, distance_max);
                for (String w : voisins){
                    if (u.equals(w)) continue;
                    float shortcutCost = map.get(v).get(u).get_distance() + map.get(v).get(w).get_distance();
                    Float distUW = distances.get(w);
                    if (distUW == null || distUW > shortcutCost){
                        //System.out.println("Création raccourci");
                        map.get(u).put(w, new Chemin(shortcutCost, true, v));
                        map.get(w).put(u, new Chemin(shortcutCost, true, v));
                    }
                }
            }
        }
    }

    public HashMap<String, Float> dijkstraLimite(String depart, String noeud_a_eviter, float distance_max){
        HashMap<String, Float> dist = new HashMap<>();
        PriorityQueue<Pair> pq = new PriorityQueue<>(Comparator.comparingDouble(p -> p.distance));
        pq.add(new Pair(depart, 0f));
        dist.put(depart, 0f);
        while (!pq.isEmpty()){
            Pair current = pq.poll();
            if (current.distance >  distance_max) break;
            if (current.distance > dist.get(current.node)) continue;
            for (String voisin : map.get(current.node).keySet()){
                if (voisin.equals(noeud_a_eviter)) continue;
                float newDist = current.distance + map.get(current.node).get(voisin).get_distance();
                if (newDist >  distance_max) continue;
                if (!dist.containsKey(voisin) || dist.get(voisin) > newDist){
                    dist.put(voisin, newDist);
                    pq.add(new Pair(voisin, newDist));
                }
            }
        }
        return dist;
    }

    public void orientation(){
        HashMap<String, HashMap<String, Chemin>> newMap = new HashMap<>();
        for (String u : map.keySet()){
            newMap.put(u, new HashMap<>());
        }
        for (String u : map.keySet()){
            for (String v : map.get(u).keySet()){
                if (noeud_vers_rang.get(u) < noeud_vers_rang.get(v)){
                    newMap.get(u).put(v, map.get(u).get(v));
                }
            }
        }
        map = newMap;
    }

    public void exporter_CSV_chemins(String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write("from;to;distance;isShortcut;via\n");
        for (String noeud1 : map.keySet()){
            for (String noeud2 : map.get(noeud1).keySet()){
                Chemin c = map.get(noeud1).get(noeud2);
                writer.write(
                        noeud1 + ";" + noeud2 + ";" + c.get_distance() + ";" + c.get_est_raccourci() + ";" + c.get_raccourci_par() + "\n"
                );
            }
        }
        writer.close();
    }
}

class Chemin{
    private Float distance;
    private Boolean est_raccourci;
    private String raccourci_par;

    public Chemin(Float distance, Boolean est_raccourci, String raccourci_par){
        this.distance = distance;
        this.est_raccourci = est_raccourci;
        this.raccourci_par = raccourci_par;
    }

    public Float get_distance(){
        return distance;
    }

    public Boolean get_est_raccourci(){
        return est_raccourci;
    }

    public String get_raccourci_par(){
        return raccourci_par;
    }

    public String toString(){
        if (est_raccourci){
            return "Est un raccourci qui passe par "+raccourci_par+". "+distance+" mètres. ";
        }
        else{
            return "N'est pas un raccourci. "+distance+" mètres.";
        }
    }
}

class Pair {
    String node;
    float distance;

    Pair(String node, float distance){
        this.node = node;
        this.distance = distance;
    }
}

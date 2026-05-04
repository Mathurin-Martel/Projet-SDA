import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.lang.*;
import java.util.Collections;
public class Java_CH{
    HashMap<String, HashMap<String, Chemin>> map = new HashMap<>();
    HashMap<String, Integer> noeud_vers_rang = new HashMap<>();

    public static void main(String[] args){
        Java_CH ch= new Java_CH();
        ch.CH("125730", "125745");
    }

    public void CH(String depart, String arrivee){
        //Etape extraction des données
        try{ lire_Csv_chemin("donnee_CH.csv");}
        catch(IOException e){System.out.println("Erreur");}

        //Etape requete
        requete(depart, arrivee);

        //Pour le programme final, separer la préparation de la requete
    }

    public void lire_Csv_chemin(String fichier) throws IOException{
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
            //tokenizedLine[3]=Raccourci ou non
            //tokenizedLine[3]=Raccourci par quelle intersection
            for(int x=0;x<tokenizedLine.length;x=x+1){
                tokenizedLine[x]=tokenizedLine[x].trim(); //.strip() si version de java supérieur a 1.11
            }

            if(!(map.containsKey(tokenizedLine[0]))){
                map.put(tokenizedLine[0], new HashMap<String, Chemin>()); //Dans le sens A vers B
            }
            if(Boolean.parseBoolean(tokenizedLine[3])){
                map.get(tokenizedLine[0]).put(tokenizedLine[1], new Chemin(Float.parseFloat(tokenizedLine[2]), Boolean.parseBoolean(tokenizedLine[3]), tokenizedLine[4]));
            }
            else{
                map.get(tokenizedLine[0]).put(tokenizedLine[1], new Chemin(Float.parseFloat(tokenizedLine[2]), Boolean.parseBoolean(tokenizedLine[3]), ""));
            }
        }
        bufferedReader.close();
    }

    public void requete(String depart, String arrivee){

        HashMap<String, Float> dist_avant = new HashMap<>();
        HashMap<String, Float> dist_arriere = new HashMap<>();

        HashMap<String, String> parent_avant = new HashMap<>();
        HashMap<String, String> parent_arriere = new HashMap<>();

        HashSet<String> visited_avant = new HashSet<>();
        HashSet<String> visited_arriere = new HashSet<>();

        PriorityQueue<Pair> pq_avant =new PriorityQueue<>(Comparator.comparingDouble(p -> p.distance));
        PriorityQueue<Pair> pq_arriere =new PriorityQueue<>(Comparator.comparingDouble(p -> p.distance));

        dist_avant.put(depart, 0f);
        dist_arriere.put(arrivee, 0f);

        pq_avant.add(new Pair(depart, 0f));
        pq_arriere.add(new Pair(arrivee, 0f));

        float best = Float.MAX_VALUE;
        String meeting = null;

        while (!pq_avant.isEmpty() || !pq_arriere.isEmpty()){
            if (!pq_avant.isEmpty()){
                Pair p = pq_avant.poll();
                String u = p.get_noeud();

                if (!map.containsKey(u)) continue;
                if (!visited_avant.add(u)) continue;

                float distU = dist_avant.get(u);

                if (distU > best) continue;

                if (dist_arriere.containsKey(u)){
                    float total = distU + dist_arriere.get(u);
                    if (total < best){
                        best = total;
                        meeting = u;
                    }
                }

                for (String v : map.get(u).keySet()){
                    float poids = map.get(u).get(v).get_distance();
                    float newDist = distU + poids;
                    if (!dist_avant.containsKey(v) || dist_avant.get(v) > newDist){
                        dist_avant.put(v, newDist);
                        parent_avant.put(v, u);
                        pq_avant.add(new Pair(v, newDist));
                    }
                }
            }

            if (!pq_arriere.isEmpty()){
                Pair p = pq_arriere.poll();
                String u = p.get_noeud();
                if (!map.containsKey(u)) continue;
                if (!visited_arriere.add(u)) continue;
                float distU = dist_arriere.get(u);
                if (distU > best) continue;
                if (dist_avant.containsKey(u)){
                    float total = distU + dist_avant.get(u);
                    if (total < best){
                        best = total;
                        meeting = u;
                    }
                }

                for (String v : map.get(u).keySet()){
                    float poids = map.get(u).get(v).get_distance();
                    float newDist = distU + poids;
                    if (!dist_arriere.containsKey(v) || dist_arriere.get(v) > newDist){
                        dist_arriere.put(v, newDist);
                        parent_arriere.put(v, u);
                        pq_arriere.add(new Pair(v, newDist));
                    }
                }
            }
        }

        if (meeting == null){
            System.out.println("Pas de chemin");
            return;
        }

        System.out.println("Distance = " + best);

        ArrayList<String> chemin = new ArrayList<>();

        String cur = meeting;
        while (cur != null){
            chemin.add(cur);
            cur = parent_avant.get(cur);
        }
        Collections.reverse(chemin);

        cur = parent_arriere.get(meeting);
        while (cur != null){
            chemin.add(cur);
            cur = parent_arriere.get(cur);
        }

        System.out.println("Chemin :");

        for (int i = 0; i < chemin.size(); i++){
            if (i != chemin.size() - 1)
                System.out.print(chemin.get(i) + " -> ");
            else
                System.out.println(chemin.get(i));
        }
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
    String noeud;
    float distance;

    Pair(String noeud, float distance){
        this.noeud = noeud;
        this.distance = distance;
    }

    public String get_noeud(){
        return noeud;
    }

    public float get_distance(){
        return distance;
    }
}

import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.*;

public class Java_CH{

    HashMap<String, HashMap<String, Float>> paire_debut_fin_distance = new HashMap<>();
    HashMap<String, Float> tableau_dijkstra=new HashMap<>();
    HashMap<String, String> tableau_dijkstra_chemin=new HashMap<>();
    HashMap<String, Float> tableau_AStar=new HashMap<>();
    HashMap<String, String> tableau_Astar_chemin=new HashMap<>();
    HashMap<String, Float> AStar_distance=new HashMap<>();

    HashMap<String, HashMap<String, Chemin>> map = new HashMap<>();
    ArrayList<String> rang_vers_noeud = new ArrayList<>();
    HashMap<String, Integer> noeud_vers_rang = new HashMap<>();

    public static void main(String[] args){
        Java_CH ch= new Java_CH();
        ch.CH("125730", "125745");
    }

    public void CH(String depart, String arrivee){
        //Etape extraction des données
        try{ readCsv("donnees_osmnx.csv");}
        catch(IOException e){System.out.println("Erreur");}

        //Etape donner rang
        donner_rang();

        //Etape Contraction
        contraction();

        //Etape Requete
        requete(depart, arrivee);

        //Pour le programme final, separer la préparation de la requete
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
                System.out.println("Test put");
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
        /*for (int x=0;x<rang_vers_noeud.size();x=x+1){
            System.out.println(x+": "+rang_vers_noeud.get(x));
        }*/
    }

    public void contraction(){
        for (int x=0; x<rang_vers_noeud.size();x=x+1){
            String noeud_contractable=rang_vers_noeud.get(x);
            for (String voisin1 : map.get(noeud_contractable).keySet()){
                for (String voisin2 : map.get(noeud_contractable).keySet()){
                    if (!(voisin1.equals(voisin2))){
                        Float distance=map.get(noeud_contractable).get(voisin1).get_distance()+map.get(noeud_contractable).get(voisin2).get_distance();
                        if(est_ce_que_meilleur_chemin(voisin1,voisin2,distance,noeud_contractable)){
                            map.get(voisin1).put(voisin2, new Chemin(distance, true, noeud_contractable));
                            map.get(voisin2).put(voisin1, new Chemin(distance, true, noeud_contractable));
                        }
                    }
                }
            }
        }
    }

    public boolean est_ce_que_meilleur_chemin(String depart, String arrivee, Float distance, String noeud_a_eviter){
        //Ici il faut faire Dijkstra pour trouver le chemin de depart vers arrivee en evitant de passer par noeud_a_eviter. Si depart->noeud_a_eviter->arrivee est le meilleur chemin alors on renvoie true, sinon on renvoie false
        //Si la distance commence a depasser la variable distance alors on peut renvoyer true tout de suite
        return true;
    }

    public void orientation(){
        for (String noeud1 : map.keySet()){
            for (String noeud2 : map.get(noeud1).keySet()){
                if (noeud_vers_rang.get(noeud1)<noeud_vers_rang.get(noeud2)){
                    map.get(noeud1).remove(noeud2);
                }
                else{
                    map.get(noeud2).remove(noeud1);
                }
            }
        }
    }
    
    public void requete(String depart, String arrivee){
        return;
        //Faire la recherche Dijkstra depuis le depart, si on ne trouve pas ca veut dire que le chemin a été supprimé lors de l'orientation du graphe
        //Du coup il faudra recommencer la recherche depuis arrivee
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

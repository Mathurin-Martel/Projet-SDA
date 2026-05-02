/*
 * NoeudPriorite représente un noeud dans la file de priorité.
 *
 * L'interface Comparable permet à Java de savoir lequel de deux noeuds
 * est prioritaire dans la file : celui avec la plus petite priorité passe en premier.
 */
public class NoeudPriorite implements Comparable<NoeudPriorite>{

    int   id;
    float priorite;

    public NoeudPriorite(int id, float priorite){
        this.id       = id;
        this.priorite = priorite;
    }

    public int compareTo(NoeudPriorite autre){
        return Float.compare(this.priorite, autre.priorite);
    }
}

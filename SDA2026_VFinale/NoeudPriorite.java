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

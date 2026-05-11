package main.nobles;

/**
 * A named leader with skills and motivations.
 * Houses hold 3; only one is active at a time.
 */
public class NobleCharacter {

    private final String     name;
    private final String     personality;
    private final Motivation dominantMotivation;
    private final Motivation secondaryMotivation;
    private final double     dominantWeight;
    private final double     secondaryWeight;

    // Skills 0-3
    private final int diplomacy; // demand/alliance bonus
    private final int military;  // combat multiplier
    private final int cunning;   // claim fabrication / scheme success

    public NobleCharacter(String name, String personality,
                          Motivation dominant, Motivation secondary,
                          double dominantWeight, double secondaryWeight,
                          int diplomacy, int military, int cunning) {
        this.name               = name;
        this.personality        = personality;
        this.dominantMotivation  = dominant;
        this.secondaryMotivation = secondary;
        this.dominantWeight      = dominantWeight;
        this.secondaryWeight     = secondaryWeight;
        this.diplomacy           = diplomacy;
        this.military            = military;
        this.cunning             = cunning;
    }

    public String     getName()               { return name; }
    public String     getPersonality()        { return personality; }
    public Motivation getDominantMotivation() { return dominantMotivation; }
    public Motivation getSecondaryMotivation(){ return secondaryMotivation; }
    public double     getDominantWeight()     { return dominantWeight; }
    public double     getSecondaryWeight()    { return secondaryWeight; }
    public int        getDiplomacy()          { return diplomacy; }
    public int        getMilitary()           { return military; }
    public int        getCunning()            { return cunning; }
}
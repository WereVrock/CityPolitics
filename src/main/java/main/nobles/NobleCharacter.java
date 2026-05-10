package main.nobles;

/**
 * A named leader with motivations and personality weights.
 * Houses hold 3; only one is active at a time.
 */
public class NobleCharacter {

    private final String     name;
    private final String     personality;
    private final Motivation dominantMotivation;
    private final Motivation secondaryMotivation;
    private final double     dominantWeight;   // 0.0–1.0
    private final double     secondaryWeight;  // 0.0–1.0

    public NobleCharacter(String name, String personality,
                          Motivation dominant, Motivation secondary,
                          double dominantWeight, double secondaryWeight) {
        this.name               = name;
        this.personality        = personality;
        this.dominantMotivation  = dominant;
        this.secondaryMotivation = secondary;
        this.dominantWeight      = dominantWeight;
        this.secondaryWeight     = secondaryWeight;
    }

    public String     getName()               { return name; }
    public String     getPersonality()        { return personality; }
    public Motivation getDominantMotivation() { return dominantMotivation; }
    public Motivation getSecondaryMotivation(){ return secondaryMotivation; }
    public double     getDominantWeight()     { return dominantWeight; }
    public double     getSecondaryWeight()    { return secondaryWeight; }
}
package main.politics;

/**
 * A minor leader within a party. Can be targeted for side deals later.
 */
public class SideLeader {

    private final String name;
    private final String personality;

    public SideLeader(String name, String personality) {
        this.name        = name;
        this.personality = personality;
    }

    public String getName()        { return name; }
    public String getPersonality() { return personality; }
}
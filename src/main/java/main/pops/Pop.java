package main.pops;

import main.politics.PoliticalAffiliation;

/**
 * A single population group: a race with a count and a political affiliation.
 * Economic output is derived from PopType stats multiplied by count.
 */
public class Pop {

    private final PopType type;
    private int count;
    private PoliticalAffiliation affiliation;

    public Pop(PopType type, PoliticalAffiliation affiliation) {
        if (!type.getEligibleAffiliations().contains(affiliation)
                && affiliation != PoliticalAffiliation.NONE) {
            throw new IllegalArgumentException(
                type.getDisplayName() + " cannot belong to " + affiliation.getDisplayName()
            );
        }
        this.type        = type;
        this.count       = type.getStartingCount();
        this.affiliation = affiliation;
    }

    // ─── Derived economics (per turn) ────────────────────────────────────────

    public int getFoodConsumption() {
        return (int) Math.ceil(type.getFoodConsumptionPerUnit() * count);
    }

    public int getMoneyGeneration() {
        return (int) (type.getMoneyGenerationPerUnit() * count);
    }

    public int getInfluenceGeneration() {
        return (int) (type.getInfluenceGenerationPerUnit() * count);
    }

    public int getManpowerContribution() {
        return (int) (type.getManpowerContributionPerUnit() * count);
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public PopType getType()                          { return type; }
    public int getCount()                             { return count; }
    public PoliticalAffiliation getAffiliation()      { return affiliation; }

    public void setCount(int count)                   { this.count = Math.max(0, count); }
    public void setAffiliation(PoliticalAffiliation a){ this.affiliation = a; }

    @Override
    public String toString() {
        return count + " " + type.getDisplayName() + "s [" + affiliation.getDisplayName() + "]";
    }
}
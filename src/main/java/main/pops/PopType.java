package main.pops;

import main.parameters.GameParameters;
import main.politics.PoliticalAffiliation;

import java.util.List;

/**
 * Defines the four playable pop races and their per-unit economic stats.
 */
public enum PopType {

    HUMAN(
        "Human",
        GameParameters.STARTING_HUMANS,
        GameParameters.HUMAN_FOOD_CONSUMPTION,
        GameParameters.HUMAN_MONEY_GENERATION,
        GameParameters.HUMAN_INFLUENCE_GENERATION,
        GameParameters.HUMAN_MANPOWER_CONTRIBUTION,
        List.of(
            PoliticalAffiliation.HUMAN_SUPREMACISTS,
            PoliticalAffiliation.ENVIRONMENTALISTS,
            PoliticalAffiliation.WAR_MONGERERS
        )
    ),

    DWARF(
        "Dwarf",
        GameParameters.STARTING_DWARVES,
        GameParameters.DWARF_FOOD_CONSUMPTION,
        GameParameters.DWARF_MONEY_GENERATION,
        GameParameters.DWARF_INFLUENCE_GENERATION,
        GameParameters.DWARF_MANPOWER_CONTRIBUTION,
        List.of(
            PoliticalAffiliation.WAR_MONGERERS
        )
    ),

    ORC(
        "Orc",
        GameParameters.STARTING_ORCS,
        GameParameters.ORC_FOOD_CONSUMPTION,
        GameParameters.ORC_MONEY_GENERATION,
        GameParameters.ORC_INFLUENCE_GENERATION,
        GameParameters.ORC_MANPOWER_CONTRIBUTION,
        List.of(
            PoliticalAffiliation.WAR_MONGERERS,
            PoliticalAffiliation.ENVIRONMENTALISTS
        )
    ),

    ELF(
        "Elf",
        GameParameters.STARTING_ELVES,
        GameParameters.ELF_FOOD_CONSUMPTION,
        GameParameters.ELF_MONEY_GENERATION,
        GameParameters.ELF_INFLUENCE_GENERATION,
        GameParameters.ELF_MANPOWER_CONTRIBUTION,
        List.of(
            PoliticalAffiliation.ENVIRONMENTALISTS
        )
    );

    private final String displayName;
    private final int startingCount;
    private final double foodConsumptionPerUnit;
    private final double moneyGenerationPerUnit;
    private final double influenceGenerationPerUnit;
    private final double manpowerContributionPerUnit;
    private final List<PoliticalAffiliation> eligibleAffiliations;

    PopType(
            String displayName,
            int startingCount,
            double foodConsumptionPerUnit,
            double moneyGenerationPerUnit,
            double influenceGenerationPerUnit,
            double manpowerContributionPerUnit,
            List<PoliticalAffiliation> eligibleAffiliations) {
        this.displayName                 = displayName;
        this.startingCount               = startingCount;
        this.foodConsumptionPerUnit      = foodConsumptionPerUnit;
        this.moneyGenerationPerUnit      = moneyGenerationPerUnit;
        this.influenceGenerationPerUnit  = influenceGenerationPerUnit;
        this.manpowerContributionPerUnit = manpowerContributionPerUnit;
        this.eligibleAffiliations        = eligibleAffiliations;
    }

    public String getDisplayName()                          { return displayName; }
    public int getStartingCount()                           { return startingCount; }
    public double getFoodConsumptionPerUnit()               { return foodConsumptionPerUnit; }
    public double getMoneyGenerationPerUnit()               { return moneyGenerationPerUnit; }
    public double getInfluenceGenerationPerUnit()           { return influenceGenerationPerUnit; }
    public double getManpowerContributionPerUnit()          { return manpowerContributionPerUnit; }
    public List<PoliticalAffiliation> getEligibleAffiliations() { return eligibleAffiliations; }
}
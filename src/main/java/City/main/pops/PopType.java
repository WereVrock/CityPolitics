package City.main.pops;

 
import City.main.parameters.PopulationStatsParams;
import City.main.parameters.StartingParams;
import City.main.politics.PolitcalView;

import java.util.List;

/**
 * Defines the four playable pop races and their per-unit economic stats.
 */
public enum PopType {

    HUMAN(
        "Human",
        StartingParams.STARTING_HUMANS,
        PopulationStatsParams.HUMAN_FOOD_CONSUMPTION,
        PopulationStatsParams.HUMAN_MONEY_GENERATION,
        PopulationStatsParams.HUMAN_INFLUENCE_GENERATION,
        PopulationStatsParams.HUMAN_MANPOWER_CONTRIBUTION,
        List.of(PolitcalView.HUMAN_SUPREMACIST,
            PolitcalView.ENVIRONMENTALIST,
            PolitcalView.WARMONGERING
        )
    ),

    DWARF(
        "Dwarf",
        StartingParams.STARTING_DWARVES,
        PopulationStatsParams.DWARF_FOOD_CONSUMPTION,
        PopulationStatsParams.DWARF_MONEY_GENERATION,
        PopulationStatsParams.DWARF_INFLUENCE_GENERATION,
        PopulationStatsParams.DWARF_MANPOWER_CONTRIBUTION,
        List.of(PolitcalView.WARMONGERING
        )
    ),

    ORC(
        "Orc",
        StartingParams.STARTING_ORCS,
        PopulationStatsParams.ORC_FOOD_CONSUMPTION,
        PopulationStatsParams.ORC_MONEY_GENERATION,
        PopulationStatsParams.ORC_INFLUENCE_GENERATION,
        PopulationStatsParams.ORC_MANPOWER_CONTRIBUTION,
        List.of(PolitcalView. WARMONGERING,
            PolitcalView. ENVIRONMENTALIST
        )
    ),

    ELF(
        "Elf",
        StartingParams.STARTING_ELVES,
        PopulationStatsParams.ELF_FOOD_CONSUMPTION,
        PopulationStatsParams.ELF_MONEY_GENERATION,
        PopulationStatsParams.ELF_INFLUENCE_GENERATION,
        PopulationStatsParams.ELF_MANPOWER_CONTRIBUTION,
        List.of(PolitcalView. ENVIRONMENTALIST
        )
    );

    private final String displayName;
    private final int startingCount;
    private final double foodConsumptionPerUnit;
    private final double moneyGenerationPerUnit;
    private final double influenceGenerationPerUnit;
    private final double manpowerContributionPerUnit;
    private final List<PolitcalView> eligibleAffiliations;

    PopType(
            String displayName,
            int startingCount,
            double foodConsumptionPerUnit,
            double moneyGenerationPerUnit,
            double influenceGenerationPerUnit,
            double manpowerContributionPerUnit,
            List<PolitcalView> eligibleAffiliations) {
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
    public List<PolitcalView> getEligibleAffiliations() { return eligibleAffiliations; }
}
package main.politics;

/**
 * A single condition that contributes to a party's vote score.
 *
 * Evaluation:
 *   if (variable [relation] threshold) → contribution = weight * party.getViewStrength(view).multiplier
 *
 * If view is null the weight is applied flat regardless of party views.
 */
public class VoteCondition {

    public enum Variable {
        MONEY, FOOD, INFLUENCE, CORRUPTION, HAPPINESS, MANPOWER
    }

    public enum Relation {
        GREATER_THAN, LESS_THAN
    }

    private final Variable           variable;
    private final Relation           relation;
    private final double             threshold;
    private final double             weight;
    private final PolitcalView view; // null = flat, no view scaling

    public VoteCondition(Variable variable, Relation relation, double threshold,
                         double weight, PolitcalView view) {
        this.variable  = variable;
        this.relation  = relation;
        this.threshold = threshold;
        this.weight    = weight;
        this.view      = view;
    }

    public Variable            getVariable()  { return variable; }
    public Relation            getRelation()  { return relation; }
    public double              getThreshold() { return threshold; }
    public double              getWeight()    { return weight; }
    public PolitcalView getView()     { return view; }
}
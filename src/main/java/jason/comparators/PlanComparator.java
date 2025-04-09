package jason.comparators;

import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Plan;

import java.util.Comparator;

public class PlanComparator implements Comparator<Plan> {

    private static String COMPARATOR = "poss(";

    public PlanComparator() {}

    public PlanComparator(String comparator) {
        COMPARATOR = comparator;
    }

    @Override
    public int compare(Plan plan1, Plan plan2) {
        LogicalFormula context1 = plan1.getContext();
        LogicalFormula context2 = plan2.getContext();

        if (context1 == null || context1.toString().isEmpty()) {
            return 1; //if plan1 has a null condition he goes after
        }
        if (context2 == null || context2.toString().isEmpty()) {
            return -1; //if plan2 has a null condition he goes after
        }

        boolean isPoss1 = context1.toString().startsWith(COMPARATOR);
        boolean isPoss2 = context2.toString().startsWith(COMPARATOR);

        if (isPoss1 && !isPoss2) {
            return 1; //if plan1 has a poss condition he goes after
        } else if (!isPoss1 && isPoss2) {
            return -1; //if plan1 has a poss condition he goes after
        }

        return 0; //else just keep the same order
    }
}

package epistemic_jason.asSyntax;


import epistemic_jason.asSemantics.AgentEpistemic;
import epistemic_jason.asSemantics.RewriteUnifier;
import epistemic_jason.formula.Formula;
import epistemic_jason.formula.NotFormula;
import epistemic_jason.formula.PropFormula;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import static epistemic_jason.asSyntax.ASUnifierSyntax.EMPTY_REWRITE_UNIF_LIST;
import static epistemic_jason.asSyntax.ASUnifierSyntax.EMPTY_UNIF_MAPPING_LIST;

public class BaseInternalAction extends InternalActionLiteral implements EpistemicFormula{

    private static Logger logger = Logger.getLogger(BaseInternalAction.class.getName());

    public BaseInternalAction(String functor) {
        super(functor);
    }

    public BaseInternalAction(InternalActionLiteral l) {
        super(l);
    }

    public LogicalFormula toFormulaWithoutModalitities(){
        return (LogicalFormula) this;
    }

    @Override
    public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList){
        if (this.isGround()){
            Literal simplified = (Literal) this.simplify();
            Formula litFormula = new PropFormula(new Pred(simplified));
            if (negated())
                return new NotFormula(litFormula);

            return litFormula;
        } else {
            return BaseLiteral.LBaseFalse.toPropFormula(null);
        }
    }

    public Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> logicalConsequenceMapping(final AgentEpistemic ag, final Unifier un){
        var iter = super.logicalConsequence(ag, un);
        if (iter == null)
            return EMPTY_UNIF_MAPPING_LIST.iterator();

        List<List<Pair<LogicalFormula, RewriteUnifier>>> list = new ArrayList<>();

        while(iter.hasNext())
        {
            Unifier unif = iter.next();
            BaseInternalAction intC = new BaseInternalAction((InternalActionLiteral) this.capply(unif));

            RewriteUnifier ru = new RewriteUnifier(intC, unif);
            Pair<LogicalFormula, RewriteUnifier> p = new Pair<LogicalFormula, RewriteUnifier>(this.toFormulaWithoutModalitities(), ru);
            // Only rewrite to true if it is ground (this depends on the IA, some will only ground the result term, leaving others unground)
//            if(intC.isGround())
            list.add(List.of(p));

//            list.add(new RewriteUnifier(LTrue, iter.next()));
        }

        return list.iterator();
    }

//    public Unifier evaluateAllFormulas(final AgentEpistemic ag, final Unifier un);

    @Override
    public Iterator<RewriteUnifier> rewriteConsequences(AgentEpistemic ag, Unifier un) {
        var iter = super.logicalConsequence(ag, un);
        if (iter == null)
            return EMPTY_REWRITE_UNIF_LIST.iterator();

        List<RewriteUnifier> list = new ArrayList<>();

        while(iter.hasNext())
        {
            Unifier unif = iter.next();
            BaseInternalAction intC = new BaseInternalAction((InternalActionLiteral) this.capply(unif));

            // Only rewrite to true if it is ground (this depends on the IA, some will only ground the result term, leaving others unground)
//            if(intC.isGround())
            list.add(new RewriteUnifier(intC, unif));

//            list.add(new RewriteUnifier(LTrue, iter.next()));
        }

        return list.iterator();

    }

    @Override
    public EpistemicFormula simplify() {
        // The expression has already been evaluated using log. consequences.
        // I.e., can be propositionalized as 'true'
        return BaseLiteral.LBaseTrue;
    }
}

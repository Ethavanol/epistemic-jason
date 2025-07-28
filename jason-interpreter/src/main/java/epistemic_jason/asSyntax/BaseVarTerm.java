package epistemic_jason.asSyntax;

import epistemic_jason.asSemantics.AgentEpistemic;
import epistemic_jason.asSemantics.RewriteUnifier;
import epistemic_jason.formula.Formula;
import epistemic_jason.formula.PropFormula;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.util.Pair;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class BaseVarTerm extends VarTerm implements EpistemicFormula {

    public BaseVarTerm(String s) {
        super(s);
    }

    public BaseVarTerm(Atom namespace, String functor) {
        super(namespace, functor);
    }

    public BaseVarTerm(Atom namespace, Literal v) {
        super(namespace, v);
    }

    public LogicalFormula toFormulaWithoutModalitities(){
        return new VarTerm(this.getFunctor());
    }


    @Override
    public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList){
        for(Pair<LogicalFormula, RewriteUnifier> pair: mappingList){
            if(pair.getFirst().equals(this)){
                if(pair.getSecond() != null){
                    return new PropFormula(new Pred(pair.getSecond().getFormula().toString()));
                }
                return new PropFormula(new Pred(LFalse));
            }
        }
        return new PropFormula(new Pred(LFalse));
    }



    public Unifier evaluateAllFormulas(final AgentEpistemic ag, final Unifier un) {
        Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> il = this.logicalConsequenceMapping(ag, un);
        while (il.hasNext()) {
            List<Pair<LogicalFormula, RewriteUnifier>> pairsList = il.next();
            Formula formula = toPropFormula(pairsList);
            if (ag.getEpistemicExtension().evaluate((Literal) this.toFormulaWithoutModalitities(), formula, ag.getTS().getAgArch().getAgName())) {
                return pairsList.get(0).getSecond().getUnifier();
            }
        }
        return null;
    }


    public Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> logicalConsequenceMapping(final AgentEpistemic ag, final Unifier un){
        BaseLiteral literalForm = new BaseLiteral((Literal) this.toFormulaWithoutModalitities());

        Iterator<RewriteUnifier> ir = literalForm.rewriteConsequences(ag,un);

        return new Iterator<List<Pair<LogicalFormula, RewriteUnifier>>>() {
            RewriteUnifier current = null;

            public boolean hasNext() {
                if (current == null)
                    get();
                return current != null;
            }

            public List<Pair<LogicalFormula, RewriteUnifier>> next() {
                if (current != null) {
                    Pair p = new Pair<>(literalForm, current);
                    current = null;
                    return Collections.singletonList(p);
                }
                return Collections.emptyList();
            }

            private void get() {
                current = null;

                if(!ir.hasNext()){
                    return;
                }

                current = ir.next();
                return;
            }

            public void remove() {}
        };
    }


    @Override
    public Iterator<RewriteUnifier> rewriteConsequences(AgentEpistemic ag, Unifier un) {
        // try to apply
        EpistemicParser parser = new EpistemicParser();
        Term t = this.capply(un);
        if (t.equals(this) ) {
            // the variable is still a Var, find all bels that unify.
            return (new BaseLiteral((Literal) t)).rewriteConsequences(ag, un);
        } else {
            // the clone is still a var
            return (parser.parseFormulaToEpistemicFormula((LogicalFormula) t, false)).rewriteConsequences(ag, un);
        }
    }


    @Override
    public EpistemicFormula simplify() {
        return (BaseVarTerm) this.clearAnnots();
    }

}

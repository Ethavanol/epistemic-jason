package epistemic_jason.asSyntax;

import epistemic_jason.asSemantics.AgentEpistemic;
import epistemic_jason.asSemantics.RewriteUnifier;
import epistemic_jason.formula.Formula;
import epistemic_jason.formula.PropFormula;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Pred;
import jason.util.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static epistemic_jason.asSyntax.ASUnifierSyntax.EMPTY_REWRITE_UNIF_LIST;
import static epistemic_jason.asSyntax.ASUnifierSyntax.EMPTY_UNIF_MAPPING_LIST;


public class BaseListTerm extends ListTermImpl implements EpistemicFormula {

    private static Logger logger = Logger.getLogger(BaseListTerm.class.getName());

    @Override
    public Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> logicalConsequenceMapping(final AgentEpistemic ag, final Unifier un){
        logger.log(Level.WARNING, "ListTermImpl cannot be used for logical consequence!", new Exception());
        return EMPTY_UNIF_MAPPING_LIST.iterator();  // empty iterator for unifier
    }

    @Override
    public Iterator<RewriteUnifier> rewriteConsequences(AgentEpistemic ag, Unifier un){
        logger.log(Level.WARNING, "ListTermImpl cannot be used for rewrite consequence!", new Exception());
        return EMPTY_REWRITE_UNIF_LIST.iterator();  // empty iterator for unifier
    }

    @Override
    public LogicalFormula toFormulaWithoutModalitities(){
        return this;
    }

    @Override
    public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList){
        return new PropFormula(new Pred(this.toString()));
    }

    @Override
    public EpistemicFormula simplify(){
        return this;
    }
}

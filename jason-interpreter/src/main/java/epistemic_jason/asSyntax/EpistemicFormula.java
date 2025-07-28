package epistemic_jason.asSyntax;

import epistemic_jason.asSemantics.AgentEpistemic;
import epistemic_jason.asSemantics.RewriteUnifier;
import epistemic_jason.formula.Formula;
import jason.asSemantics.Unifier;
import jason.asSyntax.LogicalFormula;
import jason.util.Pair;

import java.util.Iterator;
import java.util.List;

public interface EpistemicFormula {
    /**
     * Returns the corresponding traditional Jason LogicalFormula without any modalities functor
     */
    public LogicalFormula toFormulaWithoutModalitities();

    /**
     * Returns the adapted Formula format to be sent to the reasoner
     */
    public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList);

    /**
     * Returns an Iteraotr of List of Pairs. The pairs are composed of LogicalFormula, that correspond to the formula associated to the RewriteUnifier.
     * Ex : For know(lit(X)), a pair contained could be <lit(X), {Un = {X = a}, formula = b}, assuming that we would have "lit(a) :- b" in the BB.
     */
    public Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> logicalConsequenceMapping(final AgentEpistemic ag, final Unifier un);

    /**
     * Does the same as the Jason logicalConsequences(ag,un) function but return an Iterator of RewriteUnifiers instead and
     * builds the formulas associated to those with body of rules so we can evaluate them.
     */
    public Iterator<RewriteUnifier> rewriteConsequences(AgentEpistemic ag, Unifier un);

    /**
     * Simplifies the expression, if possible, to provide a more compact representation. Simplifies to a literal (or some sub-class).
     * This really only applies to LogExpr, but is needed here.
     */
    EpistemicFormula simplify();
}

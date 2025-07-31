package epistemic_jason.asSyntax;

import epistemic_jason.asSemantics.AgentEpistemic;
import epistemic_jason.asSemantics.RewriteUnifier;
import epistemic_jason.formula.Formula;
import epistemic_jason.formula.PropFormula;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static epistemic_jason.asSyntax.ASUnifierSyntax.EMPTY_REWRITE_UNIF_LIST;
import static epistemic_jason.asSyntax.ASUnifierSyntax.EMPTY_UNIF_MAPPING_LIST;


public class BaseRelExpr extends RelExpr implements EpistemicFormula {

    private static Logger logger = Logger.getLogger(BaseRelExpr.class.getName());

    public BaseRelExpr(Term t1, RelationalOp oper, Term t2) {
        super(t1,oper,t2);
    }

    public BaseRelExpr(RelExpr relExpr){
        super(relExpr.getTerm(0), relExpr.getOp(), relExpr.getTerm(1));
    }

    @Override
    public LogicalFormula toFormulaWithoutModalitities(){
        return new RelExpr(this.getTerm(0), this.getOp(), this.getTerm(1));
    }

    @Override
    public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList){
        if (mappingList != null && !mappingList.isEmpty()) {
            for(Pair<LogicalFormula, RewriteUnifier> pair : mappingList) {
                if (((LogicalFormula)pair.getFirst()).equals(this) && pair.getSecond() != null) {
                    return new PropFormula(new Pred((LTrue)));
                }
            }

            return new PropFormula(new Pred(LFalse));
        } else {
            return new PropFormula(new Pred(LFalse));
        }
    }


    public Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> logicalConsequenceMapping(final AgentEpistemic ag, final Unifier un){
        final BaseRelExpr literalForm = new BaseRelExpr((RelExpr) this.toFormulaWithoutModalitities());
        final Iterator<RewriteUnifier> ir = literalForm.rewriteConsequences(ag, un);
        return new Iterator<List<Pair<LogicalFormula, RewriteUnifier>>>() {
            RewriteUnifier current = null;

            public boolean hasNext() {
                if (this.current == null) {
                    this.get();
                }

                return this.current != null;
            }

            public List<Pair<LogicalFormula, RewriteUnifier>> next() {
                if (this.current != null) {
                    Pair p = new Pair(literalForm, this.current);
                    this.current = null;
                    return Collections.singletonList(p);
                } else {
                    return Collections.emptyList();
                }
            }

            private void get() {
                this.current = null;
                if (ir.hasNext()) {
                    this.current = (RewriteUnifier)ir.next();
                }
            }

            public void remove() {
            }
        };
    }


    @Override
    public Iterator<RewriteUnifier> rewriteConsequences(AgentEpistemic ag, Unifier un) {
        var iter = evalConsequence(ag, un);

        if (iter == null || !iter.hasNext())
            return EMPTY_REWRITE_UNIF_LIST.iterator();

        // (For RelExp): If the logical consequences have a unifier, the expression is true.
        List<RewriteUnifier> list = new ArrayList<>();

        while(iter.hasNext())
        {
            Unifier unif = iter.next();
            BaseRelExpr expC = new BaseRelExpr((RelExpr) this.capply(unif)) ;

            // Only rewrite to true if it is ground
            if(expC.isGround())
                list.add(new RewriteUnifier(expC, unif));
        }

        return list.iterator();
    }


    public Iterator<Unifier> evalConsequence(final AgentEpistemic ag, Unifier un) {
        Term xp = getTerm(0).capply(un);
        Term yp = getTerm(1).capply(un);

        Iterator<Unifier> answer = null;

        switch (getOp()) {

            case none:
                break;

            case gt :
                if (xp.compareTo(yp)  >  0) answer = LogExpr.createUnifIterator(un);
                break;
            case gte:
                if (xp.compareTo(yp)  >= 0) answer = LogExpr.createUnifIterator(un);
                break;
            case lt :
                if (xp.compareTo(yp)  <  0) answer = LogExpr.createUnifIterator(un);
                break;
            case lte:
                if (xp.compareTo(yp)  <= 0) answer = LogExpr.createUnifIterator(un);
                break;
            case eq :
                if (xp.equals(yp))          answer = LogExpr.createUnifIterator(un);
                break;
            case dif:
                if (!xp.equals(yp))         answer = LogExpr.createUnifIterator(un);
                break;
            case unify:
                if (un.unifies(xp,yp))      answer = LogExpr.createUnifIterator(un);
                break;

            case literalBuilder:
                try {
                    Literal  p = (Literal)xp;  // lhs clone
                    ListTerm l = (ListTerm)yp; // rhs clone
                    //logger.info(p+" test "+l+" un="+un);

                    // both are not vars, using normal unification
                    if (!p.isVar() && !l.isVar()) {
                        ListTerm palt = p.getAsListOfTerms();
                        if (l.size() == 3) // list without name space
                            palt = palt.getNext();
                        if (un.unifies(palt, l)) {
                            answer = LogExpr.createUnifIterator(un);
                        }
                    } else {

                        // first is var, second is list, var is assigned to l transformed in literal
                        if (p.isVar() && l.isList()) {
                            Term t = null;
                            if (l.size() == 4 && l.get(3).isPlanBody()) // the case where the list is for a plan
                                t = Plan.newFromListOfTerms(l);
                            else
                                t = Literal.newFromListOfTerms(l);
                            if (un.unifies(p, t))
                                answer = LogExpr.createUnifIterator(un);
                        } else {
                            // first is literal, second is var, var is assigned to l transformed in list
                            if (p.isLiteral() && l.isVar()) {
                                if (un.unifies(p.getAsListOfTerms(), l))
                                    answer = LogExpr.createUnifIterator(un);
                            } else {
                                // both are vars, error
                                logger.log(Level.SEVERE, "Both arguments of "+getTerm(0)+" =.. "+getTerm(1)+" are variables!");
                            }
                        }
                    }

                } catch (Exception e) {
                    logger.log(Level.SEVERE, "The arguments of operator =.. are not Literal and List.", e);
                }
                break;
        }

        if (answer == null) {
            if (ag != null && ag.getLogger().isLoggable(Level.FINE)) ag.getLogger().log(Level.FINE, "     | "+this+" failed "+ " -- "+un);
            return LogExpr.EMPTY_UNIF_LIST.iterator();  // empty iterator for unifier
        } else {
            if (ag != null && ag.getLogger().isLoggable(Level.FINE)) ag.getLogger().log(Level.FINE, "     | "+this+" succeeded "+ " -- "+un);
            return answer;
        }
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


    @Override
    public EpistemicFormula simplify() {
        if(!this.isGround())
            return BaseLiteral.LBaseFalse;

        // The expression has already been evaluated using log. consequences.
        // I.e., can be propositionalized as 'true'
        return BaseLiteral.LBaseTrue;
    }
}

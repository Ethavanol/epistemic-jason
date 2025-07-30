package epistemic_jason.asSyntax;

import epistemic_jason.asSemantics.AgentEpistemic;
import epistemic_jason.asSemantics.RewriteUnifier;
import epistemic_jason.formula.Formula;
import epistemic_jason.formula.NotFormula;
import epistemic_jason.formula.PropFormula;
import jason.architecture.AgArch;
import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.util.Pair;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static epistemic_jason.asSyntax.ASUnifierSyntax.EMPTY_REWRITE_UNIF_LIST;
import static epistemic_jason.asSyntax.ASUnifierSyntax.EMPTY_UNIF_MAPPING_LIST;


//this class is hre to handle the function we need a basic Literal to do in the case of EpistemicExtension use
// for example, in the case of a nested Epistemic : know(dir(test(know(X))), we need to be able to handle the Literals
// nested in the expression that are not epistemics.
public class BaseLiteral extends LiteralImpl implements EpistemicFormula {

    private static Logger logger = Logger.getLogger(BaseLiteral.class.getName());
    public static final BaseTrueLiteral LBaseTrue = new BaseTrueLiteral();
    public static final BaseFalseLiteral LBaseFalse = new BaseFalseLiteral();

    public BaseLiteral(String functor) {
        super(functor);
    }

    public BaseLiteral(String functor, Term ...terms) {
        super(functor);
        this.addTerms(terms);
    }

    public BaseLiteral(boolean pos, String functor) {
        super(pos, functor);
    }

    public BaseLiteral(Literal l) {
        super(l);
        if(l.hasTerm()){
            this.clearTerms();
            this.addTerms(l.getTerms().toArray(new Term[0]));
        }
    }


    protected BaseLiteral(Literal l, Unifier u) {
        super(l, u);
        if(l.hasTerm()){
            this.addTerms(l.getTerms().toArray(new Term[0]));
        }
    }

    /** if pos == true, the literal is positive, otherwise it is negative */
    public BaseLiteral(boolean pos, Literal l) {
        super(pos, l);
        if(l.hasTerm()){
            this.addTerms(l.getTerms().toArray(new Term[0]));
        }
    }

    /** if pos == true, the literal is positive, otherwise it is negative */
    public BaseLiteral(Atom namespace, boolean pos, String functor) {
        super(namespace, pos, functor);
    }

    /** creates a literal based on another but in another name space and signal */
    public BaseLiteral(Atom namespace, boolean pos, Literal l) {
        super(namespace, pos, l);
        if(l.hasTerm()){
            this.addTerms(l.getTerms().toArray(new Term[0]));
        }
    }

    @Override
    public BaseLiteral addTerms(Term ... terms){
        for (Term term: terms){
            if(term instanceof VarTerm){
                this.addTerm(new BaseVarTerm(((Literal) term).getFunctor()));
            } else if(term instanceof ListTerm){
                this.addTerm(ASEpistemicSyntax.toBaseListTerm((ListTermImpl) term));
            } else if (term instanceof NumberTerm || term instanceof BaseLiteral){
                this.addTerm(term);
            } else if (term instanceof Literal) {
                this.addTerm(new BaseLiteral((Literal) term));
            } else {
                this.addTerm(term.clone());
            }
        }
        return this;
    }

    public void clearTerms(){
        List<Term> terms = this.getTerms();
        while (terms != null && !terms.isEmpty()){
            this.delTerm(0);
        }
    }


    @Override
    public LogicalFormula toFormulaWithoutModalitities(){
        if(this.hasTerm()){
            Term[] termsArray = this.getTerms().stream()
                    .map(term -> {
                        if(term instanceof NumberTerm){
                            return term;
                        } else {
                            return ((EpistemicFormula) term).toFormulaWithoutModalitities();
                        }
                    })
                    .toArray(Term[]::new);
            return ASSyntax.createLiteral(!this.negated(), this.getFunctor(),termsArray);
        } else {
            return ASSyntax.createLiteral(!this.negated(), this.getFunctor());
        }
    }

    @Override
    public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList){
        if (mappingList == null || mappingList.isEmpty()){
            if (this.isGround()){
                Literal simplified = (Literal) this.simplify();
                Formula litFormula = new PropFormula(new Pred(simplified));
                if (negated())
                    return new NotFormula(litFormula);

                return litFormula;
            } else {
                return new PropFormula(new Pred(LFalse));
            }
        }
        for(Pair<LogicalFormula, RewriteUnifier> pair: mappingList){
            if(pair.getFirst().equals(this)){
                if(pair.getSecond() != null){
                    return new PropFormula(new Pred(pair.getSecond().getFormula().toString()));
                }
//                return new PropFormula(new Pred(LFalse));
            }
        }
        return new PropFormula(new Pred(LFalse));
    }


    @Override
    public Iterator<RewriteUnifier> rewriteConsequences(AgentEpistemic ag, Unifier un) {
        // Rewrite consequences where we use all rules
        return this.rewriteConsequences(ag, un, (u) -> true);
    }


    public Iterator<RewriteUnifier> rewriteConsequences(AgentEpistemic ag, Unifier un, Function<Literal, Boolean> shouldUseRule) {
        final boolean isInDebug = ag.getLogger().isLoggable(Level.FINE);

        final Literal literalWithoutModalities = (Literal) this.toFormulaWithoutModalitities();

        final Iterator<Literal> il = ag.getBB().getCandidateBeliefs(this, un);
        if (il == null) { // no relevant bels
            if (isInDebug) ag.getLogger().log(Level.FINE, "     | no candidate belief for " + this + " with " + un);
            return EMPTY_REWRITE_UNIF_LIST.iterator();
        }

        final AgArch arch = (ag != null && ag.getTS() != null ? ag.getTS().getAgArch() : null);
        final int nbAnnots = (hasAnnot() && getAnnots().getTail() == null ? getAnnots().size() : 0); // if annots contains a tail (as in p[A|R]), do not backtrack on annots

        return new Iterator<RewriteUnifier>() {
            RewriteUnifier current = null;
            Iterator<RewriteUnifier> ruleIt = null; // current rule solutions iterator
            Literal cloneAnnon = null; // a copy of the literal with makeVarsAnnon
            Rule rule; // current rule
            boolean needsUpdate = true;

            Iterator<List<Term>> annotsOptions = null;
            Literal belInBB = null;

            public boolean hasNext() {
                if (needsUpdate)
                    get();
                return current != null;
            }

            public RewriteUnifier next() {
                if (needsUpdate)
                    get();
                if (current != null)
                    needsUpdate = true;
                return current;
            }

            /*

                lit(X) => N unifications => O(N) time

                r(X) :- lit(X) & ... & litM(X) => O(N*M)

                r2(X) :- r(X) & ... & rL(X) => O(L * N * M) =>



             */

            private void get() {
                needsUpdate = false;
                current = null;

                beginloop:
                while (current == null) { // usually quits by returns when a solutino is found (I use this loop to avoid a bit recursion and stack overflow)

                    if (arch != null && !arch.isRunning()) return;

                    // try annots iterator
                    if (annotsOptions != null) {
                        while (annotsOptions.hasNext()) {
                            Literal belToTry = belInBB.copy().setAnnots(null).addAnnots(annotsOptions.next());
                            Unifier u = un.clone();
                            if (u.unifiesNoUndo(literalWithoutModalities, belToTry)) {
                                // Add current entry to iterator
                                current = new RewriteUnifier(new BaseLiteral((Literal) literalWithoutModalities.capply(u)), u);
                                if (isInDebug)
                                    ag.getLogger().log(Level.FINE, "     | for " + literalWithoutModalities + ", belief annotation " + belToTry + " is an option -- " + u);
                                return;
                            } else {
                                if (isInDebug)
                                    ag.getLogger().log(Level.FINE, "     | for " + literalWithoutModalities + ", belief annotation " + belToTry + " is NOT an option -- " + u);
                            }
                        }
                        annotsOptions = null;
                    }

                    // try rule iterator
                    if (ruleIt != null) {
                        while (ruleIt.hasNext()) {
                            // unifies the rule head with the result of rule evaluation
                            RewriteUnifier ruleUn = ruleIt.next(); // evaluation result
                            //Literal rhead  = rule.headClone();
                            //rhead = (Literal)rhead.capply(ruleUn);
                            Literal rhead = rule.headCApply(ruleUn.getValue());
                            useDerefVars(rhead, ruleUn.getValue()); // replace vars by the bottom in the var clusters (e.g. X=_2; Y=_2, a(X,Y) ===> A(_2,_2))
                            rhead.makeVarsAnnon(); // to remove vars in head with original names

                            RewriteUnifier unC = new RewriteUnifier(ruleUn.getFormula(), un.clone());
                            if (unC.getUnifier().unifiesNoUndo(literalWithoutModalities, rhead)) {
                                // Set current rule body formula iterator as cur iterator
                                if (shouldUseRule == null || shouldUseRule.apply(rhead))
                                    current = unC;
                                if (isInDebug)
                                    ag.getLogger().log(Level.FINE, "     | for " + literalWithoutModalities + ", rule " + rhead + " is an option -- " + unC);
                                return;
                            } else {
                                if (isInDebug)
                                    ag.getLogger().log(Level.FINE, "     | for " + literalWithoutModalities + ", rule " + rhead + " is NOT an option -- " + unC);
                            }
                        }
                        //if (isInDebug) ag.getLogger().log(Level.FINE, "     | rule "+rule+" has NO more options for "+ literalWithoutModalities);
                        ruleIt = null;
                    }

                    // try literal iterator
                    while (il.hasNext()) {
                        belInBB = il.next(); // b is the relevant entry in BB
                        if (belInBB.isRule()) {
                            rule = (Rule) belInBB;

                            // create a copy of this literal, ground it and
                            // make its vars anonymous,
                            // it is used to define what will be the unifier used
                            // inside the rule.
                            if (cloneAnnon == null) {
                                cloneAnnon = (Literal) literalWithoutModalities.capply(un);
                                cloneAnnon.makeVarsAnnon();
                            }
                            Unifier ruleUn = new Unifier();
                            if (ruleUn.unifiesNoUndo(cloneAnnon, rule)) { // the rule head unifies with the literal
                                if (isInDebug)
                                    ag.getLogger().log(Level.FINE, "     | for " + cloneAnnon + ", rule " + rule + " is an option -- " + ruleUn);

                                EpistemicParser parser = new EpistemicParser();
                                EpistemicFormula body = parser.parseFormulaToEpistemicFormula(rule.getBody(), false);

                                ruleIt = body.rewriteConsequences(ag, ruleUn);
                                //get(); // just to avoid a bit of recursion, I am using goto
                                continue beginloop;
                                //if (current != null) // if it get a value
                                //    return;
                            } else {
                                if (isInDebug)
                                    ag.getLogger().log(Level.FINE, "     | for " + cloneAnnon + ", rule " + rule + " is an NOT option -- " + ruleUn);
                            }
                        } else { // not rule
                            if (nbAnnots > 0) { // try annots backtracking
                                if (belInBB.hasAnnot()) {
                                    int nbAnnotsB = belInBB.getAnnots().size();
                                    if (nbAnnotsB >= nbAnnots) {
                                        annotsOptions = belInBB.getAnnots().subSets(nbAnnots);
                                        continue beginloop;
                                        //get();
                                        //if (current != null) // if it get a value
                                        //    return;
                                    }
                                }
                            } else { // it is an ordinary query on a belief
                                Unifier u = un.clone();
                                if (u.unifiesNoUndo(literalWithoutModalities, belInBB)) {
                                    if (isInDebug)
                                        ag.getLogger().log(Level.FINE, "     | for " + literalWithoutModalities + ", belief " + belInBB + " is an option -- " + u);
                                    current = new RewriteUnifier(new BaseLiteral((Literal) literalWithoutModalities.capply(u)), u);
                                    return;
                                } else {
                                    //if (isInDebug) ag.getLogger().log(Level.FINE, "     | belief "+belInBB+" is NOT an option for "+ literalWithoutModalities+ " -- "+u);
                                }
                            }
                        }
                    }
                    if (isInDebug) ag.getLogger().log(Level.FINE, "     | NO more options for " + literalWithoutModalities);
                    break; // do not repeat! the loop is used by 'continue' only
                } // while
            }
        };
    }


    private void useDerefVars(Term p, Unifier un) {
        if (p instanceof Literal l) {
            for (int i=0; i<l.getArity(); i++) {
                var t = l.getTerm(i);
                if (t.isVar()) {
                    l.setTerm(i, un.deref( (VarTerm)t));
                } else {
                    useDerefVars(t, un);
                }
            }
        }
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
        return (BaseLiteral) this.clearAnnots();
    }


    static final class BaseTrueLiteral extends BaseLiteral {
        public BaseTrueLiteral() {
            super("true");
        }

        @Override
        public Literal cloneNS(Atom newnamespace) {
            return this;
        }

        @Override
        public Term capply(Unifier u) {
            return this;
        }

        @Override
        public Iterator<Unifier> logicalConsequence(final Agent ag, final Unifier un) {
            return LogExpr.createUnifIterator(un);
        }

        @Override
        public Iterator<RewriteUnifier> rewriteConsequences(AgentEpistemic ag, Unifier un){
            return List.of(
                    new RewriteUnifier(LBaseTrue, un)
            ).iterator();
        }

        @Override
        public Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> logicalConsequenceMapping(final AgentEpistemic ag, final Unifier un){
            return EMPTY_UNIF_MAPPING_LIST.iterator();  // empty iterator for unifier
        }

        @Override
        public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList){
            return new PropFormula(new Pred(this));
        }

        protected Object readResolve() {
            return BaseLiteral.LBaseTrue;
        }
    }

    static final class BaseFalseLiteral extends BaseLiteral {
        public BaseFalseLiteral() {
            super("false");
        }

        @Override
        public Literal cloneNS(Atom newnamespace) {
            return this;
        }

        @Override
        public Term capply(Unifier u) {
            return this;
        }

        @Override
        public Iterator<Unifier> logicalConsequence(final Agent ag, final Unifier un) {
            return LogExpr.EMPTY_UNIF_LIST.iterator();
        }

        @Override
        public Iterator<RewriteUnifier> rewriteConsequences(AgentEpistemic ag, Unifier un) {
            return List.of(
                    new RewriteUnifier(LBaseFalse, un)
            ).iterator();
        }

        @Override
        public Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> logicalConsequenceMapping(final AgentEpistemic ag, final Unifier un){
            return EMPTY_UNIF_MAPPING_LIST.iterator();  // empty iterator for unifier
        }

        @Override
        public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList){
            return new PropFormula(new Pred(this));
        }


        protected Object readResolve() {
            return BaseLiteral.LBaseFalse;
        }
    }
}

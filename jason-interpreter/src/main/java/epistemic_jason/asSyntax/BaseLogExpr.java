package epistemic_jason.asSyntax;

import epistemic_jason.asSemantics.AgentEpistemic;
import epistemic_jason.asSemantics.RewriteUnifier;
import epistemic_jason.formula.*;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static epistemic_jason.asSyntax.ASUnifierSyntax.*;

public class BaseLogExpr extends LogExpr implements EpistemicFormula {

    private static Logger logger = Logger.getLogger(BaseLogExpr.class.getName());

    public BaseLogExpr(LogicalFormula f1, LogicalOp oper, LogicalFormula f2) {
        super(f1, oper, f2);
    }

    public BaseLogExpr(LogicalOp oper, LogicalFormula f) {
        super(oper, f);
    }

    public BaseLogExpr(LogExpr logExpr) {
        super(logExpr.getLHS(), logExpr.getOp(), logExpr.getRHS());
    }


    @Override
    public LogicalFormula toFormulaWithoutModalitities() {
        LogicalFormula lhs = this.getLHS();
        if (lhs instanceof EpistemicFormula) {
            lhs = ((EpistemicFormula) lhs).toFormulaWithoutModalitities();
        }
        switch (getOp()) {
            case none, not -> {
                return new LogExpr(this.getOp(), lhs);
            }
            case and,or -> {
                LogicalFormula rhs = this.getRHS();
                if (rhs instanceof EpistemicFormula) {
                    rhs = ((EpistemicFormula) rhs).toFormulaWithoutModalitities();
                }
                return new LogExpr(lhs, this.getOp(), rhs);
            }
            default -> {
                throw new UnsupportedOperationException("Unsupported log operator: " + getOp());
            }
        }
    }

    @Override
    public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList) {
        if (mappingList != null && !mappingList.isEmpty()) {
            for(Pair<LogicalFormula, RewriteUnifier> pair : mappingList) {
                if (pair.getFirst().equals(this) && pair.getSecond() != null) {
                    return new PropFormula(new Pred(pair.getSecond().getFormula().toString()));
                }
            }
        }
        switch (getOp()) {
            case not -> {
                return new NotFormula(getLHSEpistemic().toPropFormula(mappingList));
            }
            case and -> {
                return new AndFormula(getLHSEpistemic().toPropFormula(mappingList), getRHSEpistemic().toPropFormula(mappingList));
            }
            case or -> {
                return new OrFormula(getLHSEpistemic().toPropFormula(mappingList), getRHSEpistemic().toPropFormula(mappingList));
            }
            default -> {
                return getLHSEpistemic().toPropFormula(mappingList);
            }
        }
    }


    public EpistemicFormula getLHSEpistemic() {
        LogicalFormula lhs = this.getLHS();
        if(lhs instanceof EpistemicFormula){
            return (EpistemicFormula) lhs;
        } else {
            EpistemicParser parser = new EpistemicParser();
            return parser.parseFormulaToEpistemicFormula(lhs, false);
        }
    }

    public EpistemicFormula getRHSEpistemic() {
        LogicalFormula rhs = this.getRHS();
        if(rhs instanceof EpistemicFormula){
            return (EpistemicFormula) rhs;
        } else {
            EpistemicParser parser = new EpistemicParser();
            return parser.parseFormulaToEpistemicFormula(rhs, false);
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


    public Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> logicalConsequenceMapping(final AgentEpistemic ag, final Unifier un) {
        final LogicalFormula lhs = this.getLHSEpistemic().toFormulaWithoutModalitities();
        final LogicalFormula rhs = this.getArity() > 1 ? this.getRHSEpistemic().toFormulaWithoutModalitities() : null;
        try {
            switch (this.getOp()) {

                case none:
                    break;

                case not:
                    Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> result = getLHSEpistemic().logicalConsequenceMapping(ag, un);
                    if (!result.hasNext()) {
                        Pair p = new Pair<LogicalFormula, RewriteUnifier>(lhs, new RewriteUnifier(getLHSEpistemic(), un));
                        return createRewriteUnifListPairIterator(p);
                    } else {
                        return result;
                    }

                case and:
                    return new Iterator<List<Pair<LogicalFormula, RewriteUnifier>>>() {
                        Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> ileft = getLHSEpistemic().logicalConsequenceMapping(ag, un);
                        Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> iright  = null;
                        List<Pair<LogicalFormula, RewriteUnifier>> left = null;
                        List<Pair<LogicalFormula, RewriteUnifier>> right = null;
                        boolean needsUpdate = true;

                        public boolean hasNext() {
                            if (needsUpdate)
                                get();
                            return right != null;
                        }

                        public List<Pair<LogicalFormula, RewriteUnifier>> next() {
                            if (needsUpdate)
                                get();
                            if (right != null)
                                needsUpdate = true;
                            if (right==null || left == null) {
                                return Collections.emptyList();
                            }
                            List<Pair<LogicalFormula, RewriteUnifier>> result = new ArrayList<>();
                            result.addAll(left);
                            result.addAll(right);
                            return result;
                        }

                        private void get() {
                            needsUpdate = false;
                            right = null;
                            while ((iright == null || !iright.hasNext()) && ileft.hasNext()) {
                                left = ileft.next();
                                Unifier concatUnif = new Unifier();
                                for (Pair<LogicalFormula, RewriteUnifier> pair : left) {
                                    concatUnif.compose(pair.getSecond().getUnifier());
                                }
                                iright = getRHSEpistemic().logicalConsequenceMapping(ag, concatUnif);
                            }
                            if (iright != null && iright.hasNext()) {
                                right = iright.next();
                            }
                        }

                        public void remove() {}
                    };

                case or:
                    Unifier originalUn = un.clone();
                    return new Iterator<List<Pair<LogicalFormula, RewriteUnifier>>>() {
                        Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> ileft = getLHSEpistemic().logicalConsequenceMapping(ag, un);
                        Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> iright  = null;
                        List<Pair<LogicalFormula, RewriteUnifier>> left = null;
                        List<Pair<LogicalFormula, RewriteUnifier>> right = null;
                        boolean needsUpdate = true;

                        public boolean hasNext() {
                            if (needsUpdate)
                                get();
                            return left != null || right != null;
                        }

                        public List<Pair<LogicalFormula, RewriteUnifier>> next() {
                            if (needsUpdate)
                                get();
                            if (left != null || right != null)
                                needsUpdate = true;
                            if (left==null && right == null) {
                                return Collections.emptyList();
                            }
                            List<Pair<LogicalFormula, RewriteUnifier>> result = new ArrayList<>();
                            if (left != null) {
                                result.addAll(left);
                            }
                            if (right != null) {
                                result.addAll(right);
                            }
//                            Pair<LogicalFormula, RewriteUnifier> pleft = new Pair(lhs, left);
//                            Pair<LogicalFormula, RewriteUnifier> pright = new Pair(rhs, right);
//                            result.add(pleft);
//                            result.add(pright);
                            return result;
                        }

                        private void get() {
                            needsUpdate = false;
                            left = null;
                            right = null;
                            if (ileft != null && ileft.hasNext()) {
                                left = ileft.next();
                            } else {
                                if(iright == null)
                                    iright = getRHSEpistemic().logicalConsequenceMapping(ag, originalUn);
                                if(iright != null && iright.hasNext()) {
                                    right = iright.next();
                                }
                            }

                        }

                        public void remove() {}
                    };
            }
        } catch (Exception e) {
            String slhs = "is null ";
            Iterator<Unifier> i = lhs.logicalConsequence(ag,un);
            if (i != null) {
                slhs = "";
                while (i.hasNext()) {
                    slhs += i.next().toString()+", ";
                }
            } else {
                slhs = "iterator is null";
            }
            String srhs = "is null ";
            if (!isUnary()) {
                i = rhs.logicalConsequence(ag,un);
                if (i != null) {
                    srhs = "";
                    while (i.hasNext()) {
                        srhs += i.next().toString()+", ";
                    }
                } else {
                    srhs = "iterator is null";
                }
            }

            logger.log(Level.SEVERE, "Error evaluating expression "+this+". \nlhs elements="+slhs+". \nrhs elements="+srhs,e);
        }
        return EMPTY_UNIF_MAPPING_LIST.iterator();  // empty iterator for unifier
    }


    @Override
    public Iterator<RewriteUnifier> rewriteConsequences(AgentEpistemic ag, Unifier un) {
        try {
            switch (getOp()) {
                case none:
                    break;

                case not:
                    // unifier should be separate when rewriting 'not' formulae.
                    Unifier unClone = un.clone();
                    var cons = getLHSEpistemic().rewriteConsequences(ag, unClone);
                    if (!cons.hasNext()) {
                        // if there are no consequences of inside formula, return a true formula
                        // E.g., not X (where X is not true) ==> TRUE
                        return createRewriteUnifIterator(new RewriteUnifier(new BaseLiteral.BaseTrueLiteral(), un));
                    }
                    // If there are consequences, we rewrite the formula(s) to include the consequences.
                    // If x :- a or b, then "not x" should be replaced with "not (a or b)" rather than "not a" or "not b"

                    // Create OR containing all consequences
                    EpistemicFormula curForm = cons.next().getFormula();

                    while (cons.hasNext()) {
                        curForm = new BaseLogExpr((LogicalFormula) cons.next().getFormula(), LogicalOp.or, (LogicalFormula) curForm);
                    }

                    // I don't think we should return the 'not' formula unifiers. Jason does not do this, and it will impact the evaluation of later formulas.
                    return createRewriteUnifIterator(new RewriteUnifier(new BaseLogExpr(LogicalOp.not, (LogicalFormula) curForm), un));

                case and:
                    return new Iterator<RewriteUnifier>() {
                        Iterator<RewriteUnifier> ileft = getLHSEpistemic().rewriteConsequences(ag, un);
                        RewriteUnifier left = null;
                        ;
                        Iterator<RewriteUnifier> iright = null;
                        RewriteUnifier current = null;
                        boolean needsUpdate = true;

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

                        private void get() {
                            needsUpdate = false;
                            current = null;
                            while ((iright == null || !iright.hasNext()) && ileft.hasNext()) {
                                left = ileft.next();
                                iright = getRHSEpistemic().rewriteConsequences(ag, left.getUnifier());
                            }
                            if (iright != null && iright.hasNext()) {
                                var right = iright.next();
                                current = new RewriteUnifier(new BaseLogExpr((LogicalFormula) left.getFormula(), LogicalOp.and, (LogicalFormula) right.getFormula()), right.getUnifier());
                            }
                        }

                        public void remove() {
                        }
                    };

                case or:
                    Unifier originalUn = un.clone();
                    return new Iterator<RewriteUnifier>() {
                        Iterator<RewriteUnifier> ileft = getLHSEpistemic().rewriteConsequences(ag, un);
                        Iterator<RewriteUnifier> iright = null;
                        RewriteUnifier current = null;
                        boolean needsUpdate = true;

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

                        private void get() {
                            needsUpdate = false;
                            current = null;
                            if (ileft != null && ileft.hasNext())
                                current = ileft.next();
                            else {
                                if (iright == null)
                                    iright = getRHSEpistemic().rewriteConsequences(ag, originalUn);
                                if (iright != null && iright.hasNext())
                                    current = iright.next();
                            }
                        }

                        public void remove() {
                        }
                    };
            }
        } catch (Exception e) {
            String slhs = "is null ";
            Iterator<RewriteUnifier> i = getLHSEpistemic().rewriteConsequences(ag, un);
            if (i != null) {
                slhs = "";
                while (i.hasNext()) {
                    slhs += i.next().toString() + ", ";
                }
            } else {
                slhs = "iterator is null";
            }
            String srhs = "is null ";
            if (!isUnary()) {
                i = getRHSEpistemic().rewriteConsequences(ag, un);
                if (i != null) {
                    srhs = "";
                    while (i.hasNext()) {
                        srhs += i.next().toString() + ", ";
                    }
                } else {
                    srhs = "iterator is null";
                }
            }

            logger.log(Level.SEVERE, "Error evaluating rewrite expression " + this + ". \nlhs elements=" + slhs + ". \nrhs elements=" + srhs, e);
        }
        return EMPTY_REWRITE_UNIF_LIST.iterator();  // empty iterator for unifier
    }

    /**
     * Simplifies the expression, if possible, to provide a more compact representation.
     */
    public EpistemicFormula simplify() {
        if (getOp() == LogExpr.LogicalOp.and) {
            var formLeft = getLHSEpistemic().simplify();
            var formRight = getRHSEpistemic().simplify();

            if (formLeft == BaseLiteral.LBaseTrue) return formRight;

            if (formRight == BaseLiteral.LBaseTrue) return formLeft;

            // Otherwise, return simplified expression.
            return new BaseLogExpr((LogicalFormula) formLeft, LogExpr.LogicalOp.and, (LogicalFormula) formRight);
        }

        if (getOp() == LogExpr.LogicalOp.or) {
            var formLeft = getLHSEpistemic().simplify();
            var formRight = getRHSEpistemic().simplify();

            if (formLeft == Literal.LFalse || !((Literal) formLeft).isGround()) return formRight;

            if (formRight == Literal.LFalse || !((Literal) formRight).isGround()) return formLeft;

            // Otherwise, return pared expression.
            return new BaseLogExpr((LogicalFormula) formLeft, LogExpr.LogicalOp.or, (LogicalFormula) formRight);
        }


        if (getOp() == LogExpr.LogicalOp.not) {
            return new BaseLogExpr(LogicalOp.not, (LogicalFormula) getLHSEpistemic().simplify());
        } else {
            // E.g.: not(~test(X, Y)) = for all possible unifiers (X, Y), ~(~test(X, Y)) holds true
            // Special case, where we need to potentially ground all literals
            // More complicated: not(test(X, Y) & not(other(X, Z) OR other(Z, X)))
            //      With gs = {test(1, 2), test(3, 4), test(5, 5), other(1, 4), other(4, 1), other(5, 5)}
            //      Meaning => not([test(1, 2), test(3, 4), ] ???
            // TODO: Handle this, but for now, we assume only strong negation in constraint rules... Handled I think?
            throw new RuntimeException("Unsupported simplify for: " + getOp().toString());
        }
    }
}

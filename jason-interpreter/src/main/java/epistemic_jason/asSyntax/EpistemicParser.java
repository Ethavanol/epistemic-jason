package epistemic_jason.asSyntax;

import jason.asSyntax.*;

import java.util.logging.Logger;

public class EpistemicParser {

    private static Logger logger = Logger.getLogger(EpistemicParser.class.getName());

    public EpistemicFormula parseFormulaToEpistemicFormula(Term formula, boolean defaultToKnow) {
        if(formula == null){
            return null;
        }
        if (formula instanceof InternalActionLiteral){
            return new BaseInternalAction((InternalActionLiteral) formula);
        } else if (formula instanceof LogExpr){
            return parseLogExprToEpistemicFormula((LogExpr) formula, defaultToKnow, null);
        } else if(formula instanceof RelExpr){
            RelExpr relExpr = (RelExpr) formula;
            return new BaseRelExpr(relExpr.getTerm(0), relExpr.getOp(), relExpr.getTerm(1));
        } else {
            return parseLiteralToEpistemicFormula((Literal) formula, defaultToKnow);
        }
    }


    public EpistemicFormula parseLiteralToEpistemicFormula(Literal literal, Boolean defaultToKnow) {
        EpistemicModality modality = null;

        if(literal.getFunctor().equals("true") || literal.equals(Literal.LTrue)){
            return BaseLiteral.LBaseTrue;
        }

        if (literal.getFunctor().equals("false") || literal.equals(Literal.LFalse)){
            return BaseLiteral.LBaseFalse;
        }

        if(defaultToKnow){
            modality = EpistemicModality.KNOW;
        }
        Literal consequenceLit = literal;

        if(EpistemicModality.KNOW.isFunctor(literal.getFunctor())) {
            if (literal.getArity() != 1)
            {
                logger.warning("Invalid arity of know formula: " + literal);
                throw new RuntimeException("Invalid arity of know formula: " + literal);
            }

            modality = EpistemicModality.KNOW;
            consequenceLit = (Literal) literal.getTerm(0);
            defaultToKnow = false;
        } else if(EpistemicModality.POSSIBLE.isFunctor(literal.getFunctor())) {
            if (literal.getArity() != 1)
            {
                logger.warning("Invalid arity of poss formula: " + literal);
                throw new RuntimeException("Invalid arity of poss formula: " + literal);
            }

            modality = EpistemicModality.POSSIBLE;
            consequenceLit = (Literal) literal.getTerm(0);
            defaultToKnow = false;
        }

        if(modality == null){
            if(consequenceLit instanceof VarTerm){
                return new BaseVarTerm(consequenceLit.getFunctor());
            }
            return new BaseLiteral(literal);
        } else {
            if(consequenceLit instanceof VarTerm){
                return new EpistemicVarTerm(modality, consequenceLit.getFunctor());
            } else if(consequenceLit instanceof LogExpr){
                return parseLogExprToEpistemicFormula((LogExpr) consequenceLit, defaultToKnow, modality);
            } else if(consequenceLit instanceof RelExpr){
                throw new RuntimeException("You can't put modality on a RelExpr : " + consequenceLit);
            } else {
                Literal result = (Literal) parseFormulaToEpistemicFormula(consequenceLit, false);
                if(result instanceof EpistemicLiteral){
                    return new EpistemicLiteral(modality, (EpistemicLiteral) result);
                } else {
                    return new EpistemicLiteral(modality, result);
                }
            }
        }
    }


    public EpistemicFormula parseLogExprToEpistemicFormula(LogExpr logExpr, Boolean defaultToKnow, EpistemicModality modality) {
        // if it's the first call, no matter what, we are gonna put the modality to know if no modality is written as a functor
        switch (logExpr.getOp()){
            // if it is not for example, we want know(not X) if not recursing. So we wan the modality outside.
            case none, not -> {
                // so here we put true in recursing, so it won't put a modality by default
                EpistemicFormula LHS = parseFormulaToEpistemicFormula(logExpr.getLHS(), false);
                // here we want the modality to be bydefault if it's the first time we cycle in it
                if (modality == null){
                    return new BaseLogExpr(logExpr.getOp(), (LogicalFormula) logExpr.getLHS());
                } else {
                    return new EpistemicLogExpr(modality, logExpr.getOp(), (LogicalFormula) LHS);
                }
            }
            //if it there are two terms (| or &), we want know(X) & know(Y) for ex. So we want the modality inside both.
            case and, or -> {
                EpistemicFormula LHS = parseFormulaToEpistemicFormula(logExpr.getLHS(), defaultToKnow);
                EpistemicFormula RHS = parseFormulaToEpistemicFormula(logExpr.getRHS(), defaultToKnow);

                if (modality == null){
                    return new BaseLogExpr((LogicalFormula) LHS, logExpr.getOp(), (LogicalFormula) RHS);
                } else {
                    return new EpistemicLogExpr(modality, (LogicalFormula) LHS, logExpr.getOp(), (LogicalFormula) RHS);
                }
            }
        }
        throw new RuntimeException("Invalid logical operation in expression: " + logExpr);
    }


}

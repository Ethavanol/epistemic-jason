package epistemic_jason.asSyntax;

import epistemic_jason.asSemantics.RewriteUnifier;
import epistemic_jason.formula.Formula;
import epistemic_jason.formula.ModalPropFormula;
import jason.asSyntax.LogExpr;
import jason.asSyntax.LogicalFormula;
import jason.util.Pair;

import java.util.List;
import java.util.logging.Logger;


public class EpistemicLogExpr extends BaseLogExpr {

    private static Logger logger = Logger.getLogger(EpistemicLogExpr.class.getName());

    EpistemicModality modality;

    public EpistemicLogExpr(EpistemicModality modality, LogicalFormula f1, LogicalOp oper, LogicalFormula f2) {
        super(f1, oper, f2);
        this.modality = modality;
    }

    public EpistemicLogExpr(EpistemicModality modality, LogicalOp oper, LogicalFormula f1) {
        super(oper, f1);
        this.modality = modality;
    }

    public EpistemicLogExpr(EpistemicModality modality, LogExpr logExpr) {
        super(logExpr);
        this.modality = modality;
    }

    @Override
    public String toString() {
        if (this.modality == null){
            return super.toString();
        } else {
            return '<' + modality.toString() + ',' + super.toString() + '>';
        }
    }


    @Override
    public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList){
        if (this.modality != null){
            return new ModalPropFormula(modality, super.toPropFormula(mappingList));
        } else {
            logger.warning("Watchout there is an EpistemicLogExpr with null modality");
            return super.toPropFormula(mappingList);
        }
    }

}

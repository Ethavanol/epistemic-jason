package epistemic_jason.asSyntax;

import epistemic_jason.asSemantics.RewriteUnifier;
import epistemic_jason.formula.Formula;
import epistemic_jason.formula.ModalPropFormula;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.util.Pair;

import java.util.List;
import java.util.logging.Logger;

public class EpistemicVarTerm extends BaseVarTerm {

    EpistemicModality modality;

    private static Logger logger = Logger.getLogger(EpistemicVarTerm.class.getName());

    public EpistemicVarTerm(EpistemicModality modality, String s) {
        super(s);
        this.modality = modality;
    }

    public EpistemicVarTerm(EpistemicModality modality, Atom namespace, String functor) {
        super(namespace, functor);
        this.modality = modality;
    }

    public EpistemicVarTerm(EpistemicModality modality, Atom namespace, Literal v) {
        super(namespace, v);
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
            logger.warning("Watchout there is an EpistemicVarTerm with null modality");
            return super.toPropFormula(mappingList);
        }
    }
}

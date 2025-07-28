package epistemic_jason.asSyntax;

import epistemic_jason.asSemantics.RewriteUnifier;
import epistemic_jason.formula.Formula;
import epistemic_jason.formula.ModalPropFormula;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.util.Pair;

import java.util.List;
import java.util.logging.Logger;

public class EpistemicLiteral extends BaseLiteral {

    private static Logger logger = Logger.getLogger(EpistemicLiteral.class.getName());

    EpistemicModality modality;

    public EpistemicLiteral(EpistemicModality modality, String functor){
        super(functor);
        this.modality = modality;
    }

    public EpistemicLiteral(EpistemicModality modality, String functor, Term ...terms){
        super(functor, terms);
        this.modality = modality;
    }


    public EpistemicLiteral(EpistemicModality modality, boolean pos, String functor) {
        super(pos, functor);
        this.modality = modality;
    }

    public EpistemicLiteral(EpistemicModality modality, Literal l) {
        super(l);
        this.modality = modality;
    }

    public EpistemicLiteral(EpistemicModality modality, EpistemicLiteral el) {
        super("");
        this.addTerm(el);
        this.modality = modality;
    }

    protected EpistemicLiteral(EpistemicModality modality, Literal l, Unifier u) {
        super(l, u);
        this.modality = modality;
    }


    /** if pos == true, the literal is positive, otherwise it is negative */
    public EpistemicLiteral(EpistemicModality modality, boolean pos, Literal l) {
        super(pos, l);
        this.modality = modality;
    }

    /** if pos == true, the literal is positive, otherwise it is negative */
    public EpistemicLiteral(EpistemicModality modality, Atom namespace, boolean pos, String functor) {
        super(namespace, pos, functor);
        this.modality = modality;
    }

    /** creates a literal based on another but in another name space and signal */
    public EpistemicLiteral(EpistemicModality modality, Atom namespace, boolean pos, Literal l) {
        super(namespace, pos, l);
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
            if(this.getFunctor().equals("") && this.getArity() == 1){
                // nested modality expression
                return (LogicalFormula) termsArray[0];
            }
            return ASSyntax.createLiteral(!this.negated(),this.getFunctor(),termsArray);
        } else {
            return ASSyntax.createLiteral(!this.negated(),this.getFunctor());
        }
    }


    @Override
    public Formula toPropFormula(List<Pair<LogicalFormula, RewriteUnifier>> mappingList){
        Term term = this.getTerm(0);
        if(term instanceof EpistemicFormula && this.getFunctor().equals("")){
            return new ModalPropFormula(modality, ((EpistemicFormula) term).toPropFormula(mappingList));
        } else {
            return new ModalPropFormula(modality, super.toPropFormula(mappingList));
        }
    }

}

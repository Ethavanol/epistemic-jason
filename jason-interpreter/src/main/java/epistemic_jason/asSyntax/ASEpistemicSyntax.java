package epistemic_jason.asSyntax;

import jason.asSyntax.Atom;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;

public class ASEpistemicSyntax {


    // ************** WRITE ALL THE SYNTAX TO CREATE THE TYPES

    public static BaseLiteral createBaseLiteral(String functor, Term... terms) {
        BaseLiteral cl = new BaseLiteral(functor);
        // we have to do it this way for our terms to do not be catsted as LiteralImpl
        cl.addTerms(terms);
        return cl;
    }
    public static BaseLiteral createBaseLiteral(Atom namespace, String functor, Term... terms) {
        BaseLiteral cl = new BaseLiteral(namespace, Literal.LPos, functor);
        cl.addTerms(terms);
        return cl;
    }

    /**
     * Creates a new literal, the first argument is either Literal.LPos or Literal.LNeg,
     * the second is the functor (a string),
     * and the n remainder arguments are terms. see documentation of this
     * class for examples of use.
     */
    public static BaseLiteral createBaseLiteral(boolean positive, String functor, Term... terms) {
        BaseLiteral cl = new BaseLiteral(positive, functor);
        cl.addTerms(terms);
        return cl;
    }

    /**
     * Creates a new literal, the first argument is the namespace, the second is either Literal.LPos or Literal.LNeg,
     * the third is the functor (a string),
     * and the n remainder arguments are terms. see documentation of this
     * class for examples of use.
     */
    public static BaseLiteral createBaseLiteral(Atom namespace, boolean positive, String functor, Term... terms) {
        BaseLiteral cl = new BaseLiteral(namespace, positive, functor);
        cl.addTerms(terms);
        return cl;
    }


    public static EpistemicLiteral createEpistemicLiteral(EpistemicModality modality, String functor, Term... terms) {
        return new EpistemicLiteral(modality, createBaseLiteral(functor,terms));
    }

    public static EpistemicLiteral createEpistemicLiteral(EpistemicModality modality, Atom namespace, String functor, Term... terms) {
        return new EpistemicLiteral(modality, createBaseLiteral(namespace, functor,terms));
    }

    /**
     * Creates a new literal, the first argument is either Literal.LPos or Literal.LNeg,
     * the second is the functor (a string),
     * and the n remainder arguments are terms. see documentation of this
     * class for examples of use.
     */
    public static EpistemicLiteral createEpistemicLiteral(EpistemicModality modality, boolean positive, String functor, Term... terms) {
        return new EpistemicLiteral(modality, createBaseLiteral(positive, functor,terms));
    }

    /**
     * Creates a new literal, the first argument is the namespace, the second is either Literal.LPos or Literal.LNeg,
     * the third is the functor (a string),
     * and the n remainder arguments are terms. see documentation of this
     * class for examples of use.
     */
    public static EpistemicLiteral createEpistemicLiteral(EpistemicModality modality, Atom namespace, boolean positive, String functor, Term... terms) {
        return new EpistemicLiteral(modality, createBaseLiteral(namespace, positive, functor,terms));
    }

    public static BaseListTerm toBaseListTerm(ListTermImpl term) {
        BaseListTerm result = new BaseListTerm();
        if(term.hasTerm()){
            result.addTerms(term.getAsList().toArray(new Term[0]));
        }

        // Gestion du tail éventuel
        if (term instanceof ListTermImpl && ((ListTermImpl) term).isTail()) {
            result.setTail(((ListTermImpl) term).getTail());
        }

        return result;
    }
}

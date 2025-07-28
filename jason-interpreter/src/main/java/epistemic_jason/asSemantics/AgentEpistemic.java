package epistemic_jason.asSemantics;

import epistemic_jason.asSemantics.modelListener.ModelChangedListener;
import epistemic_jason.asSemantics.modelListener.ModelResponse;
import epistemic_jason.asSyntax.EpistemicFormula;
import epistemic_jason.asSyntax.EpistemicParser;
import epistemic_jason.formula.Formula;
import jason.JasonException;
import jason.architecture.AgArch;
import jason.asSemantics.*;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Trigger;
import jason.bb.BeliefBase;
import jason.bb.StructureWrapperForLiteral;
import jason.util.Pair;

import java.util.*;
import java.util.logging.Level;

public class AgentEpistemic extends Agent implements ModelChangedListener {

    private EpistemicExtension epistemic;

    public AgentEpistemic() {
        super();
    }

    @Override
    public void initAg() {
        // Sets up BB instance.
        super.initAg();

        // normally, this AgARch should be epistemic too
        AgArch agentArchEpistemic = this.getTS().getAgArch();

        // normally, this TransitionSystem should be epistemic too
        TransitionSystemEpistemic tse = new TransitionSystemEpistemic(this, this.getTS().getC(), this.getTS().getSettings(), agentArchEpistemic);

        agentArchEpistemic.setTS(tse);
        super.setTS(tse);

        try {
            epistemic = new EpistemicExtension(this.getTS());
        } catch (JasonException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void loadInitialAS(String asSrc) throws Exception {
        super.loadInitialAS(asSrc);
        this.epistemic.modelCreateSem(this.getTS().getAgArch().getAgName());
    }


    // here we need to override this, cause line.72 basically uses a Set, which doesn't conserve the order of the percepts.
    // In our case, as the percepts are events that are gonna be applied to the distant model, we want them to be send in the same order
    // they have been perceived
    @Override
    public int buf(Collection<Literal> percepts) {
        if (percepts == null) {
            return 0;
        }

        // stat
        int adds = 0;
        int dels = 0;
        //long startTime = qProfiling == null ? 0 : System.nanoTime();

        // Here is where we changed the Set to a List, to maintain the order
        List<StructureWrapperForLiteral> perW = new ArrayList<>();
        Iterator<Literal> iper = percepts.iterator();
        while (iper.hasNext()) {
            Literal l = iper.next();
            if (l != null)
                perW.add(new StructureWrapperForLiteral(l));
        }

        // deleting percepts in the BB that are not perceived anymore
        Iterator<Literal> perceptsInBB = getBB().getPercepts();
        while (perceptsInBB.hasNext()) {
            Literal l = perceptsInBB.next();
            if (l.subjectToBUF() && ! perW.remove(new StructureWrapperForLiteral(l))) { // l is not perceived anymore
                dels++;
                perceptsInBB.remove(); // remove l as perception from BB

                // new version (it is certain that l is in BB, only clone l when the event is relevant)
                Trigger te = new Trigger(Trigger.TEOperator.del, Trigger.TEType.belief, l);
                if (ts.getC().hasListener() || pl.hasCandidatePlan(te)) {
                    l = ASSyntax.createLiteral(l.getFunctor(), l.getTermsArray());
                    l.addAnnot(BeliefBase.TPercept);
                    te.setLiteral(l);
                    ts.getC().addEvent(new Event(te));
                }
            }
        }

        for (StructureWrapperForLiteral lw: perW) {
            try {
                Literal lp = lw.getLiteral().copy().forceFullLiteralImpl();
                lp.addAnnot(BeliefBase.TPercept);
                if (getBB().add(lp)) {
                    adds++;
                    ts.updateEvents(new Event(new Trigger(Trigger.TEOperator.add, Trigger.TEType.belief, lp)));
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error adding percetion " + lw.getLiteral(), e);
            }
        }

        return adds + dels;
    }

    // this is the function where the contexts of plans are parsed to an epistemic version,
    // after this we call the evaluation on those changed contexts
    @Override
    public List<Option> applicablePlans(List<Option> rp) throws JasonException {
        EpistemicParser parser = new EpistemicParser();

        getTS().getC().syncApPlanSense.lock();
        try {
            List<Option> ap = null;
            if (rp != null) {
                for (Option opt : rp) {
                    LogicalFormula context = opt.getPlan().getContext();
                    EpistemicFormula newContext = parser.parseFormulaToEpistemicFormula(context, true);

                    if (getLogger().isLoggable(Level.FINE))
                        getLogger().log(Level.FINE, "option for " + opt.getEvt().getTrigger() + " is plan " + opt.getPlan().getLabel() + " " + opt.getPlan().getTrigger() + " : " + context + " -- with unification " + opt.getUnifier());

                    if (context == null) { // context is true
                        if (ap == null) ap = new LinkedList<>();
                        ap.add(opt);
                        if (getLogger().isLoggable(Level.FINE))
                            getLogger().log(Level.FINE, "     " + opt.getPlan().getLabel() + " is applicable with unification " + opt.getUnifier());
                    } else {
                        boolean allUnifs = opt.getPlan().isAllUnifs();

                        Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> il = newContext.logicalConsequenceMapping(this, opt.getUnifier());
                        boolean isApplicable = false;
                        if (il != null) {
                            while (il.hasNext()) {
                                List<Pair<LogicalFormula, RewriteUnifier>> pairsList = il.next();
                                Formula formula = newContext.toPropFormula(pairsList);

                                if (this.epistemic.evaluate((Literal) newContext.toFormulaWithoutModalitities(), formula, this.getTS().getAgArch().getAgName())) {
                                    isApplicable = true;

                                    Unifier unif = new Unifier();
                                    for (Pair<LogicalFormula, RewriteUnifier> pair : pairsList) {
                                        unif.compose(pair.getSecond().getUnifier());
                                    }
                                    opt.setUnifier(unif);

                                    if (ap == null) ap = new LinkedList<>();
                                    ap.add(opt);

                                    if (getLogger().isLoggable(Level.FINE))
                                        getLogger().log(Level.FINE, "     " + opt.getPlan().getLabel() + " is applicable with unification " + opt.getUnifier());

                                    if (!allUnifs) break; // returns only the first unification
                                    if (il.hasNext()) {
                                        // create a new option for the next loop step
                                        opt = new Option(opt.getPlan(), null);
                                    }
                                }
                            }

                            if (!isApplicable && getLogger().isLoggable(Level.FINE))
                                getLogger().log(Level.FINE, "     " + opt.getPlan().getLabel() + " is not applicable");
                        }
                    }
                }
            }
            return ap;
        } finally {
            getTS().getC().syncApPlanSense.unlock();
        }
    }


    public TransitionSystemEpistemic getTS() {
        if (ts instanceof TransitionSystemEpistemic) {
            return (TransitionSystemEpistemic) ts;
        } else {
            return new TransitionSystemEpistemic(this, ts.getC(), ts.getSettings(), ts.getAgArch());
        }
    }


    public EpistemicExtension getEpistemicExtension() {
        return epistemic;
    }

    public ModelResponse getWorldsResponseModel(){
        return this.epistemic.getWorldsModelSem(this.ts.getAgArch().getAgName());
    }

    @Override
    public Option selectOption(List<Option> options) throws NoOptionException {
        return super.selectOption(options);
    }

    //called once the model has been created
    @Override
    public void modelCreated() {

    };

    //called every time an event has been applied to the model
    @Override
    public void eventModelApplied(Event event) {

    };
}

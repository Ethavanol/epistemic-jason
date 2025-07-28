package epistemic_jason.asSemantics;

import jason.architecture.AgArch;
import jason.asSemantics.*;
import jason.runtime.Settings;


public class TransitionSystemEpistemic extends TransitionSystem {

    public TransitionSystemEpistemic(AgentEpistemic a, Circumstance c, Settings s, AgArch ar) {
        super(a, c, s, ar);
    }

    public AgentEpistemic getAg() {
        return (AgentEpistemic) super.getAg();
    }
}

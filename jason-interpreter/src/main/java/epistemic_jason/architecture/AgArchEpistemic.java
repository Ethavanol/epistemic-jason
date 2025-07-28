package epistemic_jason.architecture;

import epistemic_jason.asSemantics.TransitionSystemEpistemic;
import jason.architecture.AgArch;

public class AgArchEpistemic extends AgArch {

    public AgArchEpistemic() {
        super();
    }

    @Override
    public void init() {
        this.setTS((TransitionSystemEpistemic) this.getTS());
    }
}

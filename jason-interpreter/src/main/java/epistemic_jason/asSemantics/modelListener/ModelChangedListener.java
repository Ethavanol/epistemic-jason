package epistemic_jason.asSemantics.modelListener;

import jason.asSemantics.Event;

//this interface will allow us to signal each time the model has been changed
// (so each time we use a function that would create the model or apply an event to it).
public interface ModelChangedListener {

    void modelCreated();
    void eventModelApplied(Event event);

}

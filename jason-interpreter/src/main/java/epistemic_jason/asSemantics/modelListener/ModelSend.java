package epistemic_jason.asSemantics.modelListener;

import java.util.HashMap;
import java.util.List;

public class ModelSend {

    private HashMap<World, List<String>> worldsWithNewProps = new HashMap<>();

    public void addPropsToWorld(World world, List<String> props){
        worldsWithNewProps.put(world, props);
    }

    public void toJson(){

    }
}

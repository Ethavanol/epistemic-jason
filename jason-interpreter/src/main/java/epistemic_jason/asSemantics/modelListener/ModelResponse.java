package epistemic_jason.asSemantics.modelListener;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ModelResponse {
    @SerializedName("model")
    private List<World> worlds;

    public List<World> getWorlds() {
        return worlds;
    }

    public void setModel(List<World> model) {
        this.worlds = model;
    }
}


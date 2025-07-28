package epistemic_jason.asSemantics.modelListener;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class World {
    @SerializedName("valuation")
    private ValuationWrapper valuationWrapper;

    public Map<String, Boolean> getPropositions() {
        return valuationWrapper != null ? valuationWrapper.getPropositions() : null;
    }

    public JsonElement toJson() {
        Gson gson = new Gson();
        return gson.toJsonTree(this);
    }

    @Override
    public String toString() {
        return "World{" +
                "propositions=" + getPropositions() +
                '}';
    }

    private static class ValuationWrapper {
        @SerializedName("propositions")
        private Map<String, Boolean> propositions;

        public Map<String, Boolean> getPropositions() {
            return propositions;
        }

        public void setPropositions(Map<String, Boolean> propositions) {
            this.propositions = propositions;
        }

        @Override
        public String toString() {
            return "ValuationWrapper{" +
                    "propositions=" + propositions +
                    '}';
        }
    }
}

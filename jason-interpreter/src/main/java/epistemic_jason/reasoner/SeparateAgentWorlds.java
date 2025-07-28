package epistemic_jason.reasoner;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Logger;

public class SeparateAgentWorlds {
    private static final String REASONERTYPE_CONFIG_JSON = "reasonertype-config.json";
    private static final Boolean DEFAULT_VALUE = true;
    private static SeparateAgentWorlds instance;
    private static final Logger LOGGER = Logger.getLogger("SeparateAgentWorlds");

    private Boolean separateAgentWorlds;

    protected SeparateAgentWorlds() {
        this.separateAgentWorlds = DEFAULT_VALUE;
    }

    public Boolean getSeparateAgentWorlds() {
        return separateAgentWorlds;
    }

    public static SeparateAgentWorlds getInstance() {
        if (instance == null) {
            try {
                Gson gson = new Gson();
                JsonReader reader = new JsonReader(new FileReader(REASONERTYPE_CONFIG_JSON));
                instance = gson.fromJson(reader, SeparateAgentWorlds.class);
            } catch (FileNotFoundException e) {
                LOGGER.info("No reasoner configuration file '" + REASONERTYPE_CONFIG_JSON + "' found. Using default reasonerType : " + DEFAULT_VALUE);
                instance = new SeparateAgentWorlds();
            }
        }
        return instance;
    }
}

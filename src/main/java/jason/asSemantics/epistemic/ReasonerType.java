package jason.asSemantics.epistemic;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import jason.asSemantics.epistemic.reasoner.ReasonerConfiguration;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Logger;

public class ReasonerType {
    private static final String REASONERTYPE_CONFIG_JSON = "reasonertype-config.json";
    private static final ReasonerTypeEnum DEFAULT_REASONER_TYPE = ReasonerTypeEnum.DEL;
    private static ReasonerType instance;
    private static final Logger LOGGER = Logger.getLogger("Reasoner Type");

    private ReasonerTypeEnum reasonerType;

    protected ReasonerType() {
        this.reasonerType = DEFAULT_REASONER_TYPE;
    }

    public ReasonerTypeEnum getReasonerType() {
        return reasonerType;
    }

    public static ReasonerType getInstance() {

        if (instance == null) {
            try {
                Gson gson = new Gson();
                JsonReader reader = new JsonReader(new FileReader(REASONERTYPE_CONFIG_JSON));
                instance = gson.fromJson(reader, ReasonerType.class);
            } catch (FileNotFoundException e) {
                LOGGER.info("No reasoner configuration file '" + REASONERTYPE_CONFIG_JSON + "' found. Using default reasonerType : " + DEFAULT_REASONER_TYPE);
                instance = new ReasonerType();
            }
        }
        return instance;
    }
}

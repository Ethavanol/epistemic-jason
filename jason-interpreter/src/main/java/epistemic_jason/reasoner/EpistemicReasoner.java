package epistemic_jason.reasoner;

import com.google.gson.*;
import epistemic_jason.asSemantics.DEL.DELEventModel;
import epistemic_jason.asSemantics.modelListener.ModelResponse;
import epistemic_jason.asSemantics.modelListener.World;
import epistemic_jason.formula.EpistemicFormulaLiteral;
import epistemic_jason.formula.Formula;
import epistemic_jason.formula.PropFormula;
import jason.util.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EpistemicReasoner {
    private static final String UPDATE_PROPS_SUCCESS_KEY = "success";
    private static final String EVALUATION_FORMULA_RESULTS_KEY = "result";
    private static final int NS_PER_MS = 1000000;
    private static final int MAX_CONSTRAINTS_LOG = 5000;
    private final CloseableHttpClient client;
    private static final Logger LOGGER = Logger.getLogger(EpistemicReasoner.class.getName());
    private final Logger metricsLogger = Logger.getLogger(getClass().getName() + " - Metrics");
    private final ReasonerConfiguration reasonerConfiguration;

    public EpistemicReasoner(CloseableHttpClient client) {
        this.client = client;
        this.reasonerConfiguration = ReasonerConfiguration.getInstance();
    }

    public EpistemicReasoner() {
        this(HttpClients.createDefault());
    }


    public boolean createModel(Collection<Formula> constraints, Boolean separateWorlds, String agentName) {
        metricsLogger.info("Creating model with " + constraints.size() + " constraints");

        // Maybe have the managed worlds object be event-driven for information updates.
        JsonObject managedJson = new JsonObject();
        managedJson.add("constraints", StringListToJsonArray(constraints));

        managedJson.add("separateWorlds", new JsonPrimitive(separateWorlds));

        if(separateWorlds) {
            managedJson.add("id", new JsonPrimitive(agentName));
        }

        if (constraints.size() > MAX_CONSTRAINTS_LOG)
            LOGGER.info("Over " + MAX_CONSTRAINTS_LOG + " constraints. Not printing model creation request");
        else {
//            LOGGER.info("Model Creation (Req. Body): " + managedJson.toString());
        }

        var jsonBody = managedJson.toString();

        long initialTime = System.nanoTime();

        var request = RequestBuilder
                .post(reasonerConfiguration.getModelCreateEndpoint())
                .setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON))
                .build();

        LOGGER.info("Sending Model Creation Request");

        JsonObject resultJson = sendRequest(request, EpistemicReasoner::jsonTransform).getAsJsonObject();

        boolean success = resultJson.get("success").getAsBoolean();

        if (!success) {
            String errorMsg = resultJson.has("error") ? resultJson.get("error").getAsString() : "500 Server Error";
            throw new RuntimeException("Model Creation Error: " + errorMsg);
        }

        String worldsCreated = resultJson.has("worlds") ? resultJson.get("worlds").getAsString() : "Unknown number";
        long creationTime = System.nanoTime() - initialTime;
        LOGGER.info("Model creation succeeded: " + worldsCreated + " worlds created ");
        metricsLogger.info("Model creation time (ms): " + (creationTime / NS_PER_MS));
        return success;
    }


    public ModelResponse getWorldsModel(Boolean separateWorlds, String agentName) {
        metricsLogger.info("Getting model");

        var request = RequestBuilder
                .get(reasonerConfiguration.getModelGetEndpoint())
                .addParameter("id", agentName)
                .build();

        long initialTime = System.nanoTime();
        LOGGER.info("Sending Model Getting Request");

        ModelResponse modelResponse = sendRequest(request, EpistemicReasoner::responseToModelResponse);

        long durationMs = (System.nanoTime() - initialTime) / 1_000_000;
        metricsLogger.info("Model getting time (ms): " + durationMs);

        return modelResponse;
    }


    private static JsonArray StringListToJsonArray(Collection<Formula> constraints) {
        JsonArray res = new JsonArray();
        constraints.parallelStream()
                .map(Formula::toJson)
                .forEachOrdered(res::add);
        return res;
    }

    public boolean applyEventModel(DELEventModel eventModel, Boolean separateWorlds, String agentName) {
        var json = new JsonObject();
        var arr = new JsonArray();

        for (var entry : eventModel.getDelEvents()) {
            var entryJson = new JsonObject();

            entryJson.add("id", new JsonPrimitive(entry.getEventId()));
            entryJson.add("pre", entry.getPreCondition().toJson());


            var jsonPost = new JsonObject();

            for (var postEntry : entry.getPostCondition().entrySet()) {
                jsonPost.add(postEntry.getKey().toPropString(), postEntry.getValue().toJson());
            }

            entryJson.add("post", jsonPost);

            arr.add(entryJson);
        }

        json.add("events", arr);

        if(separateWorlds){
            json.add("id", new JsonPrimitive(agentName));
        }

        var req = RequestBuilder
                .post(reasonerConfiguration.getTransitionUpdateEndpoint())
                .setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON))
                .build();

        var resultJson = sendRequest(req, EpistemicReasoner::jsonTransform).getAsJsonObject();

        System.out.println(resultJson);
        return resultJson.get(UPDATE_PROPS_SUCCESS_KEY).getAsBoolean();
    }


    public boolean replacePropsInModel(List<Pair<World, List<PropFormula>>> props, Boolean separateWorlds, String agentName) {
        var json = new JsonObject();
        var arr = new JsonArray();

        for (var entry : props) {
            var entryJson = new JsonObject();

            entryJson.add("world", entry.getFirst().toJson());
            entryJson.add("newprops", StringListToJsonArray(entry.getSecond().stream().collect(Collectors.toList())));

            arr.add(entryJson);
        }

        json.add("props", arr);

        if(separateWorlds){
            json.add("id", new JsonPrimitive(agentName));
        }

        var req = RequestBuilder
                .post(reasonerConfiguration.getReplacePropsEndpoint())
                .setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON))
                .build();

        var resultJson = sendRequest(req, EpistemicReasoner::jsonTransform).getAsJsonObject();

        System.out.println(resultJson);
        return resultJson.get(UPDATE_PROPS_SUCCESS_KEY).getAsBoolean();
    }


    public Boolean evaluateFormula(Formula formula, Boolean separateWorlds, String agentName) {
        long initialTime = System.nanoTime();
        metricsLogger.info("Evaluating formula: " + formula.toString());

        long jsonStringTime = System.nanoTime() - initialTime;
        metricsLogger.info("Formula JSON build time (ms): " + (jsonStringTime / NS_PER_MS));


        var jsonBody = new JsonObject();
        jsonBody.add("formula", formula.toJson());

        if(separateWorlds){
            jsonBody.add("id", new JsonPrimitive(agentName));
        }

        var req = RequestBuilder
                .post(reasonerConfiguration.getSingleEvaluateEndpoint())
                .setEntity(new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON))
                .build();

        var resultJson = sendRequest(req, EpistemicReasoner::jsonTransform).getAsJsonObject();

        long sendTime = System.nanoTime() - initialTime;
        metricsLogger.info("Reasoner single formula evaluation time (ms): " + ((sendTime - jsonStringTime) / NS_PER_MS));

        // If the result is null, success == false, or there is no result entry, then return an empty set.
        if (resultJson == null || !resultJson.has(EVALUATION_FORMULA_RESULTS_KEY)) {
            System.out.println("Could not read single formula evaluation response");
            return false;
        }

        return resultJson.getAsJsonPrimitive(EVALUATION_FORMULA_RESULTS_KEY).getAsBoolean();
    }


    /**
     * Sends the request without closing the response.
     *
     * @param request
     * @return
     */
    CloseableHttpResponse sendRequest(HttpUriRequest request, boolean shouldClose) {

        try {
            var res = client.execute(request);

            if (shouldClose)
                res.close();

            return res;
        } catch (IOException e) {

            throw new RuntimeException("Failed to connect to the reasoner!", e);
        }
    }

    /**
     * Sends a request, processes the response and closes the response stream.
     *
     * @param request
     * @param responseProcessFunc
     * @param <R>
     * @return
     */
    private <R> R
    sendRequest(HttpUriRequest request, @NotNull Function<CloseableHttpResponse, R> responseProcessFunc) {
        try (var res = sendRequest(request, false)) {
            return responseProcessFunc.apply(res);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    static JsonElement jsonTransform(CloseableHttpResponse response) {
        try {
            BufferedInputStream bR = new BufferedInputStream(response.getEntity().getContent());
            String jsonStr = new String(bR.readAllBytes());
            return (new JsonParser()).parse(jsonStr).getAsJsonObject();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    static ModelResponse responseToModelResponse(CloseableHttpResponse response) {
        try {
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            return gson.fromJson(responseBody, ModelResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse ModelResponse", e);
        }
    }


    /**
     * Returns a JSON element containing data for a formula.
     * The JSON element should encode:
     * - ID of formula
     * - Epistemic Modality Type ("know" or "possible")
     * - Negation of Modality (i.e. "~possible")
     * - Contained Proposition (i.e. cards["Alice", "AA"])
     * - Proposition Negation (i.e. ~cards["Alice", "AA"])
     *
     * @param formula
     * @return
     */
    JsonElement toFormulaJSON(EpistemicFormulaLiteral formula) {
        var jsonElement = new JsonObject();
        jsonElement.addProperty("id", formula.getUniqueId());

        jsonElement.addProperty("modalityNegated", formula.isModalityNegated());
        jsonElement.addProperty("modality", formula.getEpistemicModality().getFunctor());

        jsonElement.addProperty("propNegated", formula.isPropositionNegated());
        jsonElement.addProperty("prop", formula.getRootLiteral().toPropFormula(null).toPropString());


        return jsonElement;
    }
}

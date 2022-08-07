package i5.las2peer.services.apiTestingBot.context;

import i5.las2peer.apiTestModel.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static i5.las2peer.services.apiTestingBot.context.TestModelingState.*;

/**
 * Stores the information about a test case that gets modeled.
 */
public class TestModelingContext {

    /**
     * Current state within the modeling process.
     */
    private TestModelingState state;

    /**
     * CAE projects that are linked to the current channel.
     */
    private List<JSONObject> projectsLinkedToChannel;

    /**
     * CAE project that the test case should be added to.
     */
    private JSONObject project;

    /**
     * Microservice that the test case should be added to.
     */
    private JSONObject microserviceComponent;

    /**
     * Name of the test case.
     */
    private String testCaseName;

    /**
     * OpenAPI documentation of the microservice.
     */
    private OpenAPI openAPI;
    private List<Map.Entry<PathItem.HttpMethod, String>> availableMethods;

    /**
     * Request method of the test case request.
     */
    private String requestMethod;

    /**
     * Request path of the test case request.
     */
    private String requestPath;

    /**
     * Values assigned to the path parameters of the request path.
     */
    private HashMap<String, String> pathParamValues = null;

    /**
     * Request body of the test case request.
     */
    private String requestBody;

    /**
     * Response assertions.
     */
    private List<RequestAssertion> assertions = new ArrayList<>();

    /**
     * If a body assertion is modeled, its type is stored here.
     */
    private BodyAssertionType wipBodyAssertionType;

    /**
     * If a body assertion is modeled, it is stored here.
     */
    private BodyAssertion wipBodyAssertion;

    /**
     * Initializes the context state.
     */
    public TestModelingContext() {
        this.state = INIT;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public TestModelingState getState() {
        return state;
    }

    public JSONObject getProject() {
        return project;
    }

    public void setProject(JSONObject project) {
        this.project = project;
    }

    public void setState(TestModelingState state) {
        this.state = state;
    }

    public List<JSONObject> getProjectsLinkedToChannel() {
        return projectsLinkedToChannel;
    }

    public void setProjectsLinkedToChannel(List<JSONObject> projectsLinkedToChannel) {
        this.projectsLinkedToChannel = projectsLinkedToChannel;
    }

    public JSONObject getMicroserviceComponent() {
        return microserviceComponent;
    }

    public void setMicroserviceComponent(JSONObject microserviceComponent) {
        this.microserviceComponent = microserviceComponent;
    }

    public int getComponentVersionedModelId() {
        if (microserviceComponent == null) return -1;
        return ((Long) microserviceComponent.get("versionedModelId")).intValue();
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public List<Map.Entry<PathItem.HttpMethod, String>> getAvailableMethods() {
        return availableMethods;
    }

    public void setAvailableMethods(List<Map.Entry<PathItem.HttpMethod, String>> availableMethods) {
        this.availableMethods = availableMethods;
    }

    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    public void setOpenAPI(String openAPIDocStr) {
        SwaggerParseResult parsed = new OpenAPIV3Parser().readContents(openAPIDocStr);
        this.openAPI = parsed.getOpenAPI();
    }

    public List<PathParameter> getPathParams() {
        if (getOperation().getParameters() == null) return new ArrayList<>();
        return getOperation().getParameters().stream().filter(param -> param instanceof PathParameter).map(PathParameter.class::cast).toList();
    }

    private Operation getOperation() {
        return this.openAPI.getPaths().get(this.requestPath).readOperationsMap().get(PathItem.HttpMethod.valueOf(this.requestMethod));
    }

    public HashMap<String, String> getPathParamValues() {
        return pathParamValues;
    }

    public void setPathParamValues(HashMap<String, String> pathParamValues) {
        this.pathParamValues = pathParamValues;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public List<RequestAssertion> getAssertions() {
        return assertions;
    }

    public BodyAssertion getWipBodyAssertion() {
        return wipBodyAssertion;
    }

    public void setWipBodyAssertion(BodyAssertion wipBodyAssertion) {
        this.wipBodyAssertion = wipBodyAssertion;
    }

    public void saveWipBodyAssertion() {
        getAssertions().add(getWipBodyAssertion());
        setWipBodyAssertion(null);
    }

    public BodyAssertionType getWipBodyAssertionType() {
        return wipBodyAssertionType;
    }

    public BodyAssertionOperator getWipBodyAssertionLastOperator() {
        BodyAssertionOperator lastOperator = getWipBodyAssertion().getOperator();
        while (lastOperator.getFollowingOperator() != null) {
            lastOperator = lastOperator.getFollowingOperator();
        }
        return lastOperator;
    }

    public void setWipBodyAssertionType(BodyAssertionType wipBodyAssertionType) {
        this.wipBodyAssertionType = wipBodyAssertionType;
    }

    public List<JSONObject> getMicroserviceComponentsOfProject() {
        JSONObject projectMetadata = (JSONObject) getProject().get("metadata");
        JSONArray components = (JSONArray) projectMetadata.get("components");
        return components.stream().filter(component -> ((JSONObject) component).get("type").equals("microservice")).toList();
    }

    public TestModel toTestModel() {
        JSONObject pathParams = new JSONObject();
        if(pathParamValues != null) {
            for (Map.Entry<String, String> entry : pathParamValues.entrySet()) {
                pathParams.put(entry.getKey(), entry.getValue());
            }
        }

        int operatorId = 0;

        // assertions need to have different ids
        for(int i = 0; i < assertions.size(); i++) {
            RequestAssertion a = assertions.get(i);
            a.setId(i);

            if(a instanceof BodyAssertion) {
                BodyAssertion b = (BodyAssertion) a;
                BodyAssertionOperator operator = b.getOperator();
                while(operator != null) {
                    operator.setId(operatorId);
                    operatorId++;
                    operator = operator.getFollowingOperator();
                }
            }
        }

        TestRequest request = new TestRequest(requestMethod, requestPath, pathParams, -1, requestBody, assertions);
        TestCase testCase = new TestCase(testCaseName, List.of(request));
        return new TestModel(List.of(testCase));
    }
}

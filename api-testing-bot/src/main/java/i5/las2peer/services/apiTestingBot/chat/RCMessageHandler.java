package i5.las2peer.services.apiTestingBot.chat;

import i5.las2peer.services.apiTestingBot.context.TestModelingContext;
import i5.las2peer.services.apiTestingBot.util.ProjectServiceHelper;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.PathParameter;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static i5.las2peer.services.apiTestingBot.chat.MessageHandlerUtil.getFirstUnsetPathParam;
import static i5.las2peer.services.apiTestingBot.chat.MessageHandlerUtil.handleNumberSelectionQuestion;
import static i5.las2peer.services.apiTestingBot.chat.Messages.*;
import static i5.las2peer.services.apiTestingBot.chat.Messages.SELECT_PROJECT_FOR_TEST_CASE;
import static i5.las2peer.services.apiTestingBot.context.TestModelingState.*;

/**
 * Handles messages if the bot is used in RocketChat with the CAE.
 */
public class RCMessageHandler extends MessageHandler {

    private String caeBackendURL;

    public RCMessageHandler(String caeBackendURL) {
        this.caeBackendURL = caeBackendURL;
    }

    /**
     * Reacts to the initial message of the user that starts a test modeling conversation.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @return Whether the next state should be handled too.
     */
    @Override
    public boolean handleInit(StringBuilder responseMessageSB, TestModelingContext context) {
        responseMessageSB.append(MODEL_TEST_CASE_INTRO);
        context.setState(RC_SELECT_PROJECT);
        return true;
    }

    /**
     * Loads projects that are linked to the given channel.
     * If there is no project linked to the channel, no test case can be modeled.
     * If there is one project linked to the channel, chooses this project.
     * If there are multiple projects linked to the channel, asks the user which one should be chosen.
     *
     * @param responseMessageSB StringBuilder
     * @param context Current test modeling context
     * @param channel Channel name
     * @return Whether the next state should be handled too.
     */
    public boolean handleProjectSelectionQuestion(StringBuilder responseMessageSB, TestModelingContext context,
                                                  String channel) {
        // get projects that are linked to the channel
        List<JSONObject> projectsLinkedToChannel = ProjectServiceHelper.getProjectsLinkedToChannel(channel);
        context.setProjectsLinkedToChannel(projectsLinkedToChannel);

        if (projectsLinkedToChannel.size() == 0) {
            responseMessageSB.append("\n" + NO_PROJECT_LINKED_TO_CHANNEL);
            context.setState(FINAL);
            return false;
        } else if (projectsLinkedToChannel.size() == 1) {
            // there's only one project, use that
            JSONObject project = projectsLinkedToChannel.get(0);
            context.setProject(project);
            context.setState(RC_SELECT_MICROSERVICE);
            responseMessageSB.append(" " + TEST_ADD_TO_PROJECT((String) project.get("name")));
            return true;
        } else {
            // there are multiple projects, need to select one
            responseMessageSB.append(" " + SELECT_PROJECT_FOR_TEST_CASE);
            int i = 1;
            for (JSONObject project : projectsLinkedToChannel) {
                String projectName = (String) project.get("name");
                responseMessageSB.append("\n" + i + ". " + projectName);
                i++;
            }
            return false;
        }
    }

    /**
     * If there are multiple projects linked to the current channel, then the project selection is handled.
     * After that, sets state to SELECT_MICROSERVICE.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleProjectSelection(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        // there were multiple projects linked to the channel and the user needed to select one of them
        // user should have entered a number
        List<JSONObject> projectsLinkedToChannel = context.getProjectsLinkedToChannel();
        boolean error = handleNumberSelectionQuestion(responseMessageSB, message, projectsLinkedToChannel.size(), (num) -> {
            // select this project
            JSONObject selectedProject = projectsLinkedToChannel.get(num - 1);
            context.setProject(selectedProject);
            responseMessageSB.append(TEST_ADD_TO_PROJECT((String) selectedProject.get("name")));
        });

        // error might have occurred if user needed to enter a number to choose a project but input was invalid
        if (!error) {
            context.setState(RC_SELECT_MICROSERVICE);
            return true;
        }

        return false;
    }

    /**
     * If project contains no microservice, no test case can be modeled.
     * If project contains one microservice, chooses this microservice.
     * If project contains multiple microservices, asks the user which one should be chosen.
     *
     * @param responseMessageSB StringBuilder
     * @param context Current test modeling context
     * @return Whether the next state should be handled too.
     */
    public boolean handleMicroserviceSelectionQuestion(StringBuilder responseMessageSB, TestModelingContext context) {
        // get components of project
        List<JSONObject> microserviceComponents = context.getMicroserviceComponentsOfProject();
        if (microserviceComponents.size() == 0) {
            responseMessageSB.append("\n" + NO_MICROSERVICE_IN_PROJECT);
            context.setState(FINAL);
            return false;
        } else if (microserviceComponents.size() == 1) {
            // there's only one microservice => use that
            JSONObject component = microserviceComponents.get(0);
            context.setMicroserviceComponent(component);
            responseMessageSB.append(" " + TEST_ADD_TO_MICROSERVICE((String) component.get("name")));
            context.setState(NAME_TEST_CASE);
            return true;
        } else {
            // there are multiple microservices, need to select one
            responseMessageSB.append(" " + SELECT_MICROSERVICE_FOR_TEST_CASE);
            int i = 1;
            for (JSONObject service : microserviceComponents) {
                String serviceName = (String) service.get("name");
                responseMessageSB.append("\n" + i + ". " + serviceName);
                i++;
            }
            return false;
        }
    }

    /**
     * If there are multiple microservices in the selected project, then the microservice selection is handled.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleMicroserviceSelection(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        // user should have entered a number
        List<JSONObject> microserviceComponents = context.getMicroserviceComponentsOfProject();
        boolean error = handleNumberSelectionQuestion(responseMessageSB, message, microserviceComponents.size(), (num) -> {
            // select this microservice
            JSONObject selectedService = microserviceComponents.get(num - 1);
            context.setMicroserviceComponent(selectedService);
            responseMessageSB.append(TEST_ADD_TO_MICROSERVICE((String) selectedService.get("name")));
        });

        // error might have occurred if user needed to enter a number to choose a microservice but input was invalid
        if (!error) {
            context.setState(NAME_TEST_CASE);
            return true;
        }
        return false;
    }

    /**
     * Stores the previously entered test case name to the context.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    @Override
    public boolean handleTestCaseName(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        context.setState(RC_SELECT_METHOD);
        return super.handleTestCaseName(responseMessageSB, context, message);
    }

    /**
     * Lists the methods (from OpenAPI doc) that can be tested and asks the user to choose one of them.
     *
     * @param responseMessageSB StringBuilder
     * @param context Current test modeling context
     * @return Whether the next state should be handled too.
     */
    public boolean handleMethodSelectionQuestion(StringBuilder responseMessageSB, TestModelingContext context) {
        // ask which method should be tested
        responseMessageSB.append(SELECT_METHOD_TO_TEST);

        // fetch OpenAPI doc of microservice from CAE
        int versionedModelId = context.getComponentVersionedModelId();
        HttpResponse<String> response = Unirest.get(caeBackendURL + "/docs/component/" + versionedModelId).asString();
        if (!response.isSuccess()) {
            responseMessageSB.append("\n" + ERROR_LOADING_AVAILABLE_METHODS);
            context.setState(FINAL);
            return false;
        }

        JSONObject body = (JSONObject) JSONValue.parse(response.getBody());
        String openAPIDocStr = (String) body.get("docString");
        context.setOpenAPI(openAPIDocStr);

        // list available methods in chat
        int i = 1;
        List<Map.Entry<PathItem.HttpMethod, String>> availableMethods = new ArrayList<>();
        for (Map.Entry<String, PathItem> entry : context.getOpenAPI().getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();
            for (Map.Entry<PathItem.HttpMethod, Operation> entry2 : pathItem.readOperationsMap().entrySet()) {
                PathItem.HttpMethod method = entry2.getKey();
                availableMethods.add(Map.entry(method, path));
                responseMessageSB.append("\n" + i + ". " + method.name() + " " + path);
                i++;
            }
        }
        context.setAvailableMethods(availableMethods);
        return false;
    }

    /**
     * Handles the selection of the method that should be tested.
     * Then switches to state ENTER_PATH_PARAMS.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleMethodSelection(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        // user should have entered a number
        List<Map.Entry<PathItem.HttpMethod, String>> availableMethods = context.getAvailableMethods();
        boolean error = handleNumberSelectionQuestion(responseMessageSB, message, availableMethods.size(), (num) -> {
            // select this method
            Map.Entry<PathItem.HttpMethod, String> selectedMethod = availableMethods.get(num - 1);
            context.setRequestMethod(selectedMethod.getKey().name());
            context.setRequestPath(selectedMethod.getValue());
            responseMessageSB.append(TEST_METHOD_INFO(selectedMethod.getKey().name(), selectedMethod.getValue()));
        });

        // error might have occurred if user needed to enter a number to choose a method but input was invalid
        if (!error) {
            context.setState(RC_ENTER_PATH_PARAMS);
            return true;
        }
        return false;
    }

    /**
     * If method contains path parameters, sends info message that values for these parameters need to be set.
     * Otherwise switches to state BODY_QUESTION.
     *
     * @param responseMessageSB StringBuilder
     * @param context Current test modeling context
     * @return Whether the next state should be handled too.
     */
    public boolean handlePathParamsQuestion(StringBuilder responseMessageSB, TestModelingContext context) {
        // check if there are path parameters
        if (context.getPathParams().size() > 0) {
            responseMessageSB.append(SET_PATH_PARAM_VALUES);
            return true;
        } else {
            // no path params
            context.setState(BODY_QUESTION);
            return true;
        }
    }

    /**
     * If user message contains path parameter value, it is stored to context.
     * Then the user is asked to enter the value of the next path parameter (if there is any).
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handlePathParams(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        // get first unset param
        PathParameter unsetParam = getFirstUnsetPathParam(context);

        if (context.getPathParamValues() != null) {
            // last message should contain the value for a path parameter
            responseMessageSB.append(PATH_PARAM_SET_INFO(unsetParam.getName(), message));
            context.getPathParamValues().put(unsetParam.getName(), message);
        } else {
            context.setPathParamValues(new HashMap<>());
        }

        // check if there are still unset parameters
        if (context.getPathParams().size() != context.getPathParamValues().size()) {
            // update first unset parameter
            unsetParam = getFirstUnsetPathParam(context);
            responseMessageSB.append(ENTER_PATH_PARAM_VALUE(unsetParam.getName()));
        } else {
            // all params are set => replace params with their values in path
            String path = context.getRequestPath();
            for (Map.Entry<String, String> entry : context.getPathParamValues().entrySet()) {
                path = path.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            responseMessageSB.append(REQUEST_URL_INFO(path));
            context.setState(BODY_QUESTION);
            return true;
        }

        return false;
    }
}

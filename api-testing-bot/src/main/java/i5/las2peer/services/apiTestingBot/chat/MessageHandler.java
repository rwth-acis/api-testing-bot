package i5.las2peer.services.apiTestingBot.chat;

import i5.las2peer.apiTestModel.BodyAssertion;
import i5.las2peer.apiTestModel.BodyAssertionOperator;
import i5.las2peer.apiTestModel.RequestAssertion;
import i5.las2peer.apiTestModel.StatusCodeAssertion;
import i5.las2peer.services.apiTestingBot.context.BodyAssertionType;
import i5.las2peer.services.apiTestingBot.context.TestModelingContext;
import i5.las2peer.services.apiTestingBot.util.ProjectServiceHelper;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.PathParameter;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static i5.las2peer.services.apiTestingBot.chat.MessageHandlerUtil.*;
import static i5.las2peer.services.apiTestingBot.chat.Messages.*;
import static i5.las2peer.services.apiTestingBot.chat.Messages.ENTER_NUMBER;
import static i5.las2peer.services.apiTestingBot.context.TestModelingState.*;

public class MessageHandler {

    private String caeBackendURL;

    public MessageHandler(String caeBackendURL) {
        this.caeBackendURL = caeBackendURL;
    }

    /**
     * Handles the initial message of the user that starts a test modeling conversation.
     * Loads projects that are linked to the given channel.
     * If there is no project linked to the channel, no test case can be modeled.
     * If there is one project linked to the channel, chooses this project.
     * If there are multiple projects linked to the channel, asks the user which one should be chosen.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param channel           Current channel
     * @return Whether the next state should be handled too.
     */
    public boolean handleInit(StringBuilder responseMessageSB, TestModelingContext context, String channel) {
        responseMessageSB.append(MODEL_TEST_CASE_INTRO);

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
            context.nextState();
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
            context.nextState();
            return false;
        }
    }

    /**
     * If there are multiple projects linked to the current channel, then the project selection is handled first.
     * After the project selection is done:
     * If project contains no microservice, no test case can be modeled.
     * If project contains one microservice, chooses this microservice.
     * If project contains multiple microservices, asks the user which one should be chosen.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleProjectSelection(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        boolean handleNextState = false;
        boolean error = false;

        // check if no project is selected yet (e.g., if there were multiple projects linked to the channel and the
        // user needed to select one of them)
        if (context.getProject() == null) {
            // user should have entered a number
            List<JSONObject> projectsLinkedToChannel = context.getProjectsLinkedToChannel();
            error = handleNumberSelectionQuestion(responseMessageSB, message, projectsLinkedToChannel.size(), (num) -> {
                // select this project
                JSONObject selectedProject = projectsLinkedToChannel.get(num - 1);
                context.setProject(selectedProject);
                responseMessageSB.append(TEST_ADD_TO_PROJECT((String) selectedProject.get("name")));
            });
        }

        // error might have occurred if user needed to enter a number to choose a project but input was invalid
        if (!error) {
            // get components of project
            List<JSONObject> microserviceComponents = context.getMicroserviceComponentsOfProject();
            if (microserviceComponents.size() == 0) {
                responseMessageSB.append("\n" + NO_MICROSERVICE_IN_PROJECT);
                context.setState(FINAL);
            } else if (microserviceComponents.size() == 1) {
                // there's only one microservice => use that
                JSONObject component = microserviceComponents.get(0);
                context.setMicroserviceComponent(component);
                context.nextState();
                handleNextState = true;
                responseMessageSB.append(" " + TEST_ADD_TO_MICROSERVICE((String) component.get("name")));
            } else {
                // there are multiple microservices, need to select one
                responseMessageSB.append(" " + SELECT_MICROSERVICE_FOR_TEST_CASE);
                int i = 1;
                for (JSONObject service : microserviceComponents) {
                    String serviceName = (String) service.get("name");
                    responseMessageSB.append("\n" + i + ". " + serviceName);
                    i++;
                }
                context.nextState();
            }
        }
        return handleNextState;
    }

    /**
     * If there are multiple microservices in the selected project, then the microservice selection is handled first.
     * After a microservice has been selected, the user gets asked to enter a name for the test case.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleMicroserviceSelection(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        boolean error = false;

        // check if no microservice is selected yet (e.g., if there were multiple services contained in the selected
        // project and the user needed to select one of them)
        if (context.getMicroserviceComponent() == null) {
            // user should have entered a number
            List<JSONObject> microserviceComponents = context.getMicroserviceComponentsOfProject();
            error = handleNumberSelectionQuestion(responseMessageSB, message, microserviceComponents.size(), (num) -> {
                // select this microservice
                JSONObject selectedService = microserviceComponents.get(num - 1);
                context.setMicroserviceComponent(selectedService);
                responseMessageSB.append(TEST_ADD_TO_MICROSERVICE((String) selectedService.get("name")));
            });
        }

        // error might have occurred if user needed to enter a number to choose a microservice but input was invalid
        if (!error) {
            responseMessageSB.append(ENTER_TEST_CASE_NAME);
            context.nextState();
        }
        return false;
    }

    /**
     * Stores the previously entered test case name to the context, then lists the methods (from OpenAPI doc) that
     * can be tested and asks the user to choose one of them.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleTestCaseName(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        context.setTestCaseName(message);

        // ask which method should be tested
        responseMessageSB.append(TEST_CASE_NAME_INFO(message) + " " + SELECT_METHOD_TO_TEST);

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

        context.nextState();
        return false;
    }

    /**
     * Handles the selection of the method that should be tested.
     * Then switches to state ENTER_PATH_PARAMS or BODY_QUESTION depending on whether the operation contains path
     * parameters.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleMethodSelection(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        boolean error = false;
        if (context.getRequestMethod() == null) {
            // user should have entered a number
            List<Map.Entry<PathItem.HttpMethod, String>> availableMethods = context.getAvailableMethods();
            error = handleNumberSelectionQuestion(responseMessageSB, message, availableMethods.size(), (num) -> {
                // select this method
                Map.Entry<PathItem.HttpMethod, String> selectedMethod = availableMethods.get(num - 1);
                context.setRequestMethod(selectedMethod.getKey().name());
                context.setRequestPath(selectedMethod.getValue());
                responseMessageSB.append(TEST_METHOD_INFO(selectedMethod.getKey().name(), selectedMethod.getValue()));
            });
        }

        // error might have occurred if user needed to enter a number to choose a method but input was invalid
        if (!error) {
            // check if there are path parameters
            if (context.getPathParams().size() > 0) {
                context.setState(ENTER_PATH_PARAMS);
                responseMessageSB.append(SET_PATH_PARAM_VALUES);
            } else {
                // no path params
                context.setState(BODY_QUESTION);
                context.nextState();
            }
            return true;
        }
        return false;
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
            context.nextState();
            return true;
        }

        return false;
    }

    /**
     * The user gets asked whether the test request should contain a JSON body.
     *
     * @param responseMessageSB StringBuilder
     * @return Whether the next state should be handled too.
     */
    public boolean handleBodyQuestion(StringBuilder responseMessageSB) {
        responseMessageSB.append(INCLUDE_REQUEST_BODY_QUESTION);
        return false;
    }

    /**
     * Handles answer of user to the question, whether the test request should contain a JSON body.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param intent            Intent
     * @return Whether the next state should be handled too.
     */
    public boolean handleBodyQuestionAnswer(StringBuilder responseMessageSB, TestModelingContext context, String intent) {
        return handleYesNoQuestion(responseMessageSB, intent, () -> {
            // user should enter body
            responseMessageSB.append(ENTER_REQUEST_BODY);
            context.setState(ENTER_BODY);
            return false;
        }, () -> {
            // no body should be included
            responseMessageSB.append(OK);
            context.setState(ASSERTIONS_QUESTION);
            return true;
        });
    }

    /**
     * Checks if the request body entered by the user is valid JSON.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleBody(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        // check if entered body is valid JSON
        try {
            JSONValue.parseWithException(message);
            // valid
            context.setRequestBody(message);
            responseMessageSB.append(REQUEST_BODY_SET_INFO);
            context.setState(ASSERTIONS_QUESTION);
            return true;
        } catch (ParseException e) {
            // invalid
            responseMessageSB.append(ERROR_BODY_NO_VALID_JSON);
            return false;
        }
    }

    /**
     * Sends message to ask user whether the test should contain assertions on the response to this request.
     *
     * @param responseMessageSB StringBuilder
     * @return Whether the next state should be handled too.
     */
    public boolean handleAssertionsQuestion(StringBuilder responseMessageSB) {
        responseMessageSB.append(INCLUDE_ASSERTIONS_QUESTION);
        return false;
    }

    /**
     * Handles answer of user to the question, whether the test should contain assertions on the response to this request.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param intent            Intent
     * @return Whether the next state should be handled too.
     */
    public boolean handleAssertionsQuestionAnswer(StringBuilder responseMessageSB, TestModelingContext context, String intent) {
        return handleYesNoQuestion(responseMessageSB, intent, () -> {
            responseMessageSB.append(OK);
            context.setState(ASSERTION_TYPE_QUESTION);
            return true;
        }, () -> {
            responseMessageSB.append(OK);
            context.setState(FINAL);
            return false;
        });
    }

    /**
     * Sends message to ask user which type the assertion should have.
     *
     * @param responseMessageSB StringBuilder
     * @return Whether the next state should be handled too.
     */
    public boolean handleAssertionTypeQuestion(StringBuilder responseMessageSB) {
        appendLinesWithBreaks(responseMessageSB, SELECT_ASSERTION_TYPE, ASSERTION_TYPE_STATUS_CODE, ASSERTION_TYPE_RESPONSE_BODY);
        return false;
    }

    /**
     * Handles answer of user to the question, which type the assertion should have.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleAssertionTypeQuestionAnswer(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        try {
            int num = Integer.parseInt(message);
            if (num == 1) {
                context.setState(ENTER_STATUS_CODE);
                return true;
            } else if (num == 2) {
                context.setState(BODY_ASSERTION_TYPE_QUESTION);
                return true;
            } else {
                responseMessageSB.append(ENTER_NUMBER_BETWEEN(1, 2));
                return false;
            }
        } catch (NumberFormatException e) {
            responseMessageSB.append(ENTER_NUMBER);
            return false;
        }
    }

    /**
     * Sends message to ask user to enter the expected status code.
     *
     * @param responseMessageSB StringBuilder
     * @return Whether the next state should be handled too.
     */
    public boolean handleStatusCodeQuestion(StringBuilder responseMessageSB) {
        responseMessageSB.append(EXPECTED_STATUS_CODE_QUESTION);
        return false;
    }

    /**
     * Handles the user's expected status code input.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleStatusCodeInput(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        // status code should have been entered
        try {
            int statusCode = Integer.parseInt(message);
            context.getAssertions().add(new StatusCodeAssertion(StatusCodeAssertion.COMPARISON_OPERATOR_EQUALS, statusCode));
            responseMessageSB.append(ADDED_ASSERTION_TO_TEST);
            context.setState(ASSERTIONS_OVERVIEW);
            return true;
        } catch (NumberFormatException e) {
            responseMessageSB.append(ENTER_NUMBER);
            return false;
        }
    }

    /**
     * Lists the assertions that were modeled.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @return Whether the next state should be handled too.
     */
    public boolean handleAssertionsOverview(StringBuilder responseMessageSB, TestModelingContext context) {
        // show overview of current assertions
        responseMessageSB.append("\n");
        responseMessageSB.append(ASSERTION_OVERVIEW);
        for (RequestAssertion assertion : context.getAssertions()) {
            if (assertion instanceof StatusCodeAssertion) {
                responseMessageSB.append("\n- Status code equals " + ((StatusCodeAssertion) assertion).getStatusCodeValue());
            } else {
                responseMessageSB.append("\n- " + ((BodyAssertion) assertion).getOperator().toString());
            }
        }
        responseMessageSB.append("\n");
        context.setState(ADD_ANOTHER_ASSERTION_QUESTION);
        return true;
    }

    /**
     * Sends message to ask user if another assertion should be added.
     *
     * @param responseMessageSB StringBuilder
     * @return Whether the next state should be handled too.
     */
    public boolean handleAddAssertionQuestion(StringBuilder responseMessageSB) {
        responseMessageSB.append(ADD_ANOTHER_ASSERTION);
        return false;
    }

    public boolean handleAddAssertionQuestionAnswer(StringBuilder responseMessageSB, TestModelingContext context, String intent) {
        return handleAssertionsQuestionAnswer(responseMessageSB, context, intent);
    }

    /**
     * Sends message to ask user which type the body assertion should have.
     *
     * @param responseMessageSB StringBuilder
     * @return Whether the next state should be handled too.
     */
    public boolean handleBodyAssertionTypeQuestion(StringBuilder responseMessageSB) {
        appendLinesWithBreaks(responseMessageSB, SELECT_BODY_ASSERTION_TYPE, BODY_ASSERTION_TYPE_1,
                BODY_ASSERTION_TYPE_2, BODY_ASSERTION_TYPE_3, BODY_ASSERTION_TYPE_4, BODY_ASSERTION_TYPE_5,
                BODY_ASSERTION_TYPE_6);
        return false;
    }

    /**
     * Handles answer of user to the question which type the body assertion should have.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handleBodyAssertionTypeInput(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        // user should have entered a number
        try {
            int num = Integer.parseInt(message);
            int min = 1;
            int max = 6;
            if (min <= num && num <= max) {
                context.setWipBodyAssertionType(numberToBodyAssertionType(num));
                context.setState(ENTER_BODY_ASSERTION_PART);
                return true;
            } else {
                responseMessageSB.append(ENTER_NUMBER_BETWEEN(min, max));
                return false;
            }
        } catch (NumberFormatException e) {
            responseMessageSB.append(ENTER_NUMBER);
            return false;
        }
    }

    /**
     * Depending on the WIP assertion type, sends message that expected type or field name should be entered.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @return Whether the next state should be handled too.
     */
    public boolean handleBodyAssertionPartQuestion(StringBuilder responseMessageSB, TestModelingContext context) {
        switch (context.getWipBodyAssertionType()) {
            case TYPE_CHECK:
            case ANY_LIST_ENTRY_TYPE_CHECK:
            case ALL_LIST_ENTRIES_TYPE_CHECK:
                responseMessageSB.append(ENTER_EXPECTED_TYPE);
                break;
            case FIELD_CHECK:
            case ANY_LIST_ENTRY_FIELD_CHECK:
            case ALL_LIST_ENTRIES_FIELD_CHECK:
                responseMessageSB.append(ENTER_FIELD_NAME);
                break;
        }
        return false;
    }

    /**
     * Handles expected type or field name input from user.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param message           Message sent by the user.
     * @param intent            Intent
     * @return Whether the next state should be handled too.
     */
    public boolean handleBodyAssertionPartInput(StringBuilder responseMessageSB, TestModelingContext context, String message, String intent) {
        BodyAssertionType assertionType = context.getWipBodyAssertionType();
        switch (assertionType) {
            case TYPE_CHECK:
            case ANY_LIST_ENTRY_TYPE_CHECK:
            case ALL_LIST_ENTRIES_TYPE_CHECK:
                if (!List.of("type_jsonobject", "type_jsonarray", "type_string", "type_number", "type_boolean").contains(intent)) {
                    responseMessageSB.append(ERROR_COULD_NOT_UNDERSTAND_TYPE);
                    return false;
                } else {
                    // type was recognized
                    BodyAssertionOperator typeCheckOperator = new BodyAssertionOperator(0, intentToInputType(intent));
                    BodyAssertionOperator operator = createBodyAssertionOperatorTypeCheck(assertionType, typeCheckOperator);

                    if (context.getWipBodyAssertion() == null) {
                        context.getAssertions().add(new BodyAssertion(operator));
                    } else {
                        BodyAssertionOperator lastOperator = context.getWipBodyAssertionLastOperator();
                        lastOperator.setFollowedByOperator(operator);
                        context.saveWipBodyAssertion();
                    }

                    // type check is always the ending operator of the assertion
                    context.setState(ASSERTIONS_OVERVIEW);
                    return true;
                }
            case FIELD_CHECK:
            case ANY_LIST_ENTRY_FIELD_CHECK:
            case ALL_LIST_ENTRIES_FIELD_CHECK:
                BodyAssertionOperator hasFieldOperator = new BodyAssertionOperator(1, 1, message, null);
                BodyAssertionOperator operator = createBodyAssertionOperatorFieldCheck(assertionType, hasFieldOperator);

                if (context.getWipBodyAssertion() == null) {
                    context.setWipBodyAssertion(new BodyAssertion(operator));
                } else {
                    BodyAssertionOperator lastOperator = context.getWipBodyAssertionLastOperator();
                    lastOperator.setFollowedByOperator(operator);
                }
                context.setState(END_OF_BODY_ASSERTION_QUESTION);
                return true;
        }
        return false;
    }

    /**
     * Sends a preview of the current assertion and asks whether it should be edited further.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @return Whether the next state should be handled too.
     */
    public boolean handleEndOfBodyAssertionQuestion(StringBuilder responseMessageSB, TestModelingContext context) {
        responseMessageSB.append(ASSERTION_PREVIEW + " " + context.getWipBodyAssertion().getOperator().toString());
        responseMessageSB.append("\n");

        BodyAssertionOperator lastOperator = context.getWipBodyAssertionLastOperator();
        responseMessageSB.append(FURTHER_ASSERT_ON_FIELD_QUESTION(lastOperator.getInputValue()));
        return false;
    }

    /**
     * Handles answer to question whether the current assertion should be edited further.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @param intent            Intent
     * @return Whether the next state should be handled too.
     */
    public boolean handleEndOfBodyAssertionQuestionAnswer(StringBuilder responseMessageSB, TestModelingContext context,
                                                          String intent) {
        return handleYesNoQuestion(responseMessageSB, intent, () -> {
            responseMessageSB.append(OK);
            context.setState(BODY_ASSERTION_TYPE_QUESTION);
            return true;
        }, () -> {
            responseMessageSB.append(OK);
            // save wip body assertion
            context.saveWipBodyAssertion();
            context.setState(ASSERTIONS_OVERVIEW);
            return true;
        });
    }
}

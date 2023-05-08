package i5.las2peer.services.apiTestingBot.chat;

import i5.las2peer.apiTestModel.*;
import i5.las2peer.services.apiTestingBot.codex.CodeToTestModel;
import i5.las2peer.services.apiTestingBot.codex.CodexAPI;
import i5.las2peer.services.apiTestingBot.codex.CodexTestGen;
import i5.las2peer.services.apiTestingBot.context.BodyAssertionType;
import i5.las2peer.services.apiTestingBot.context.TestModelingContext;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.List;

import static i5.las2peer.services.apiTestingBot.chat.MessageHandlerUtil.*;
import static i5.las2peer.services.apiTestingBot.chat.Messages.*;
import static i5.las2peer.services.apiTestingBot.chat.Messages.ENTER_NUMBER;
import static i5.las2peer.services.apiTestingBot.context.TestModelingState.*;

public abstract class MessageHandler {

    /**
     * Reacts to the initial message of the user that starts a test modeling conversation.
     *
     * @param responseMessageSB StringBuilder
     * @param context           Current test modeling context
     * @return Whether the next state should be handled too.
     */
    public boolean handleInit(StringBuilder responseMessageSB, TestModelingContext context) {
        responseMessageSB.append(MODEL_TEST_CASE_INTRO);
        context.setState(API_TEST_FAMILIARITY_QUESTION);
        return true;
    }

    public boolean handleAPITestFamiliarityQuestion(StringBuilder responseMessageSB) {
        if(!responseMessageSB.isEmpty()) responseMessageSB.append(" ");
        responseMessageSB.append(API_TEST_FAMILIARITY_QUESTION_TEXT);
        return false;
    }

    public abstract boolean handleAPITestFamiliarityQuestionAnswer(StringBuilder responseMessageSB, TestModelingContext context, String intent);

    public boolean handleTestCaseDescriptionQuestion(StringBuilder responseMessageSB) {
        if(!responseMessageSB.isEmpty()) responseMessageSB.append(" ");
        responseMessageSB.append(ENTER_TEST_CASE_DESCRIPTION_TEXT);
        return false;
    }

    public boolean handleTestCaseDescription(StringBuilder responseMessageSB, TestModelingContext context, String message,
                                             String codexAPIToken, String openAIModel) {
        TestRequest generatedRequest = null;
        try {
            generatedRequest = new CodexTestGen(codexAPIToken, openAIModel).descriptionToTestModel(message);
        } catch (CodexAPI.CodexAPIException | IOException e) {
            e.printStackTrace();
            return false;
        } catch (CodeToTestModel.CodeToTestModelException e) {
            e.printStackTrace();
            responseMessageSB.append(ERROR_TEST_CASE_GENERATION);
            return false;
        }
        context.setRequestMethod(generatedRequest.getType());
        context.setRequestPath(generatedRequest.getUrl());
        context.setPathParamValues(generatedRequest.getPathParams());
        context.setTestCaseName("TestCaseName");
        context.setRequestBody(generatedRequest.getBody());
        context.getAssertions().addAll(generatedRequest.getAssertions());
        responseMessageSB.append(getTestDescription(generatedRequest));
        context.setState(FINAL);
        return false;
    }

    public abstract String getTestDescription(TestRequest request);

    /**
     * Returns the request url of the given test request where the path parameters are replaced with their values.
     *
     * @param request TestRequest
     * @return Request url of the given test request where the path parameters are replaced with their values.
     */
    protected static String getRequestUrlWithPathParamValues(TestRequest request) {
        String url = request.getUrl();
        JSONObject pathParams = request.getPathParams();
        for(Object key : pathParams.keySet()) {
            String paramValue = String.valueOf(pathParams.get(key));
            if(paramValue.isEmpty()) paramValue = "<Enter " + key + ">";
            url = url.replace("{" + key + "}", paramValue);
        }
        return url;
    }

    /**
     * Ask user to enter a name for the test case.
     *
     * @param responseMessageSB StringBuilder
     * @return Whether the next state should be handled too.
     */
    public boolean handleTestCaseNameQuestion(StringBuilder responseMessageSB) {
        if(!responseMessageSB.isEmpty()) responseMessageSB.append(" ");
        responseMessageSB.append(ENTER_TEST_CASE_NAME);
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
    public boolean handleTestCaseName(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        context.setTestCaseName(message);
        responseMessageSB.append(TEST_CASE_NAME_INFO(message));
        return true;
    }

    /**
     * The user gets asked whether the test request should contain a JSON body.
     *
     * @param responseMessageSB StringBuilder
     * @return Whether the next state should be handled too.
     */
    public boolean handleBodyQuestion(StringBuilder responseMessageSB) {
        if(!responseMessageSB.isEmpty()) responseMessageSB.append(" ");
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
        if(!responseMessageSB.isEmpty()) responseMessageSB.append(" ");
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
        if(!responseMessageSB.isEmpty()) responseMessageSB.append(" ");
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
        responseMessageSB.append("\n\n");
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
        if(!responseMessageSB.isEmpty()) responseMessageSB.append(" ");
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

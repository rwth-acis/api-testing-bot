package i5.las2peer.services.apiTestingBot.chat;

import i5.las2peer.apiTestModel.BodyAssertion;
import i5.las2peer.apiTestModel.RequestAssertion;
import i5.las2peer.apiTestModel.StatusCodeAssertion;
import i5.las2peer.apiTestModel.TestRequest;
import i5.las2peer.services.apiTestingBot.context.TestModelingContext;

import static i5.las2peer.services.apiTestingBot.chat.Messages.*;
import static i5.las2peer.services.apiTestingBot.context.TestModelingState.*;

/**
 * Handles messages if the bot is used in GitHub.
 */
public class GHMessageHandler extends MessageHandler {

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
        context.setState(NAME_TEST_CASE);
        return true;
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
        context.setState(GH_METHOD_QUESTION);
        return super.handleTestCaseName(responseMessageSB, context, message);
    }

    /**
     * Asks the user to enter the request method.
     *
     * @param responseMessageSB StringBuilder
     * @param context Current test modeling context
     * @return Whether the next state should be handled too.
     */
    public boolean handleMethodQuestion(StringBuilder responseMessageSB, TestModelingContext context) {
        if(!responseMessageSB.isEmpty()) responseMessageSB.append(" ");
        responseMessageSB.append(GH_ENTER_REQUEST_METHOD);
        context.setState(GH_ENTER_METHOD);
        return false;
    }

    /**
     * Handles the request method entered by the user.
     *
     * @param responseMessageSB StringBuilder
     * @param context Current test modeling context
     * @param intent Intent
     * @return Whether the next state should be handled too.
     */
    public boolean handleMethod(StringBuilder responseMessageSB, TestModelingContext context, String intent) {
        if(!Intent.REQUEST_METHOD_INTENTS.contains(intent)) {
            responseMessageSB.append(ERROR_COULD_NOT_UNDERSTAND);
            return false;
        }

        // set request method in context
        context.setRequestMethod(Intent.toRequestMethod(intent));
        context.setState(GH_PATH_QUESTION);
        return true;
    }

    /**
     * Asks the user to enter the request path.
     *
     * @param responseMessageSB StringBuilder
     * @param context Current test modeling context
     * @return Whether the next state should be handled too.
     */
    public boolean handlePathQuestion(StringBuilder responseMessageSB, TestModelingContext context) {
        responseMessageSB.append(GH_ENTER_REQUEST_PATH);
        context.setState(GH_ENTER_PATH);
        return false;
    }

    /**
     * Handles the request path entered by the user.
     *
     * @param responseMessageSB StringBuilder
     * @param context Current test modeling context
     * @param message Message sent by the user.
     * @return Whether the next state should be handled too.
     */
    public boolean handlePath(StringBuilder responseMessageSB, TestModelingContext context, String message) {
        context.setRequestPath(message);
        responseMessageSB.append(GH_REQUEST_INFO(context.getRequestMethod(), context.getRequestPath()));
        context.setState(BODY_QUESTION);
        return true;
    }
    public static String getGitHubTestDescription(TestRequest request) {
        String description = "";
        String url = getRequestUrlWithPathParamValues(request);
        description += "**Method & Path:** `" + request.getType() + "` `" + url + "`\n";

        // request auth info
        String auth = request.getAgent() == -1 ? "None" : "User Agent";
        description += "**Authorization:** " +  auth + "\n";

        // request body (if exists)
        if(request.getBody() != null && !request.getBody().isEmpty()) {
            description += "**Body:**\n" + "```json" + request.getBody() + "```\n";
        }

        // list assertions
        description += "**Assertions:**\n";
        for(RequestAssertion assertion : request.getAssertions()) {
            description += "- ";
            if(assertion instanceof StatusCodeAssertion) {
                description += "Expected status code: " + ((StatusCodeAssertion) assertion).getStatusCodeValue();
            } else {
                description += ((BodyAssertion) assertion).getOperator().toString();
            }
            description += "\n";
        }
        return description;
    }
}

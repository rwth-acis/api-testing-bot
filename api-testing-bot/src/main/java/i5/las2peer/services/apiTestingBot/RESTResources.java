package i5.las2peer.services.apiTestingBot;

import i5.las2peer.api.Context;
import i5.las2peer.apiTestModel.*;
import i5.las2peer.services.apiTestingBot.chat.GHMessageHandler;
import i5.las2peer.services.apiTestingBot.chat.Intent;
import i5.las2peer.services.apiTestingBot.chat.MessageHandler;
import i5.las2peer.services.apiTestingBot.chat.RCMessageHandler;
import i5.las2peer.services.apiTestingBot.context.MessengerType;
import i5.las2peer.services.apiTestingBot.context.TestModelingContext;
import i5.las2peer.services.apiTestingBot.context.TestModelingState;
import i5.las2peer.services.apiTestingBot.util.PRTestGenHelper;
import io.swagger.annotations.Api;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.io.Serializable;
import java.net.HttpURLConnection;

import static i5.las2peer.services.apiTestingBot.context.MessengerType.*;
import static i5.las2peer.services.apiTestingBot.context.TestModelingState.*;

@Api
@Path("/")
public class RESTResources {

    @GET
    @Path("/")
    public Response get() {
        return Response.status(200).entity("API-Testing-Bot service is running!").build();
    }

    @POST
    @Path("/test/model")
    public Response modelTest(String body) {
        JSONObject bodyJSON = (JSONObject) JSONValue.parse(body);
        String intent = (String) bodyJSON.get("intent");
        String message = (String) bodyJSON.get("msg");
        String messenger = (String) bodyJSON.get("messenger");
        MessengerType messengerType = MessengerType.fromString(messenger);
        String channel = (String) bodyJSON.get("channel");

        // get current modeling context for this channel
        TestModelingContext context;
        if(!APITestingBot.channelModelingContexts.containsKey(channel)) APITestingBot.channelModelingContexts.put(channel, new TestModelingContext());
        context = APITestingBot.channelModelingContexts.get(channel);

        // get the initial state of the context (at the beginning of this execution)
        TestModelingState initialState = context.getState();

        boolean handleNextState = false;

        StringBuilder responseMessageSB = new StringBuilder();
        APITestingBot service = (APITestingBot) Context.get().getService();

        // setup message handler
        MessageHandler messageHandler;
        if(messengerType == ROCKET_CHAT) messageHandler = new RCMessageHandler(service.getCaeBackendURL());
        else if(messengerType == GITHUB_ISSUES || messengerType == GITHUB_PR) messageHandler = new GHMessageHandler();
        else return Response.status(Response.Status.BAD_REQUEST).entity("Unsupported messenger type.").build();

        do {
            if (initialState == INIT && intent.equals(Intent.MODEL_TEST)) {
                handleNextState = messageHandler.handleInit(responseMessageSB, context);
            }

            if (handleNextState && context.getState() == RC_SELECT_PROJECT) {
                handleNextState = ((RCMessageHandler) messageHandler).handleProjectSelectionQuestion(responseMessageSB, context, channel);
            }

            if (initialState == RC_SELECT_PROJECT) {
                handleNextState = ((RCMessageHandler) messageHandler).handleProjectSelection(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == RC_SELECT_MICROSERVICE) {
                handleNextState = ((RCMessageHandler) messageHandler).handleMicroserviceSelectionQuestion(responseMessageSB, context);
            }

            if (initialState == RC_SELECT_MICROSERVICE) {
                handleNextState = ((RCMessageHandler) messageHandler).handleMicroserviceSelection(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == NAME_TEST_CASE) {
                handleNextState = messageHandler.handleTestCaseNameQuestion(responseMessageSB);
            }

            if (initialState == NAME_TEST_CASE) {
                handleNextState = messageHandler.handleTestCaseName(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == RC_SELECT_METHOD) {
                handleNextState = ((RCMessageHandler) messageHandler).handleMethodSelectionQuestion(responseMessageSB, context);
            }

            if (initialState == RC_SELECT_METHOD) {
                handleNextState = ((RCMessageHandler) messageHandler).handleMethodSelection(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == RC_ENTER_PATH_PARAMS) {
                handleNextState = ((RCMessageHandler) messageHandler).handlePathParamsQuestion(responseMessageSB, context);
            }

            if ((handleNextState && context.getState() == RC_ENTER_PATH_PARAMS) || initialState == RC_ENTER_PATH_PARAMS) {
                handleNextState = ((RCMessageHandler) messageHandler).handlePathParams(responseMessageSB, context, message);
            }

            if(handleNextState && context.getState() == GH_METHOD_QUESTION) {
                handleNextState = ((GHMessageHandler) messageHandler).handleMethodQuestion(responseMessageSB, context);
            }

            if(initialState == GH_ENTER_METHOD) {
                handleNextState = ((GHMessageHandler) messageHandler).handleMethod(responseMessageSB, context, intent);
            }

            if(handleNextState && context.getState() == GH_PATH_QUESTION) {
                handleNextState = ((GHMessageHandler) messageHandler).handlePathQuestion(responseMessageSB, context);
            }

            if(initialState == GH_ENTER_PATH) {
                handleNextState = ((GHMessageHandler) messageHandler).handlePath(responseMessageSB, context, message);
            }

            if ((handleNextState && context.getState() == BODY_QUESTION)) {
                handleNextState = messageHandler.handleBodyQuestion(responseMessageSB);
            }

            if (!handleNextState && initialState == BODY_QUESTION) {
                handleNextState = messageHandler.handleBodyQuestionAnswer(responseMessageSB, context, intent);
            }

            if (initialState == ENTER_BODY) {
                handleNextState = messageHandler.handleBody(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == ASSERTIONS_QUESTION) {
                handleNextState = messageHandler.handleAssertionsQuestion(responseMessageSB);
            }

            if (initialState == ASSERTIONS_QUESTION) {
                handleNextState = messageHandler.handleAssertionsQuestionAnswer(responseMessageSB, context, intent);
            }

            if (handleNextState && context.getState() == ASSERTION_TYPE_QUESTION) {
                handleNextState = messageHandler.handleAssertionTypeQuestion(responseMessageSB);
            }

            if (initialState == ASSERTION_TYPE_QUESTION) {
                handleNextState = messageHandler.handleAssertionTypeQuestionAnswer(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == ENTER_STATUS_CODE) {
                handleNextState = messageHandler.handleStatusCodeQuestion(responseMessageSB);
            }

            if (initialState == ENTER_STATUS_CODE) {
                handleNextState = messageHandler.handleStatusCodeInput(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == ASSERTIONS_OVERVIEW) {
                handleNextState = messageHandler.handleAssertionsOverview(responseMessageSB, context);
            }

            if (handleNextState && context.getState() == ADD_ANOTHER_ASSERTION_QUESTION) {
                handleNextState = messageHandler.handleAddAssertionQuestion(responseMessageSB);
            }

            if (initialState == ADD_ANOTHER_ASSERTION_QUESTION) {
                handleNextState = messageHandler.handleAddAssertionQuestionAnswer(responseMessageSB, context, intent);
            }

            if(handleNextState && context.getState() == BODY_ASSERTION_TYPE_QUESTION) {
                handleNextState = messageHandler.handleBodyAssertionTypeQuestion(responseMessageSB);
            }

            if(initialState == BODY_ASSERTION_TYPE_QUESTION) {
                handleNextState = messageHandler.handleBodyAssertionTypeInput(responseMessageSB, context, message);
            }

            if(handleNextState && context.getState() == ENTER_BODY_ASSERTION_PART) {
                handleNextState = messageHandler.handleBodyAssertionPartQuestion(responseMessageSB, context);
            }

            if(initialState == ENTER_BODY_ASSERTION_PART) {
                handleNextState = messageHandler.handleBodyAssertionPartInput(responseMessageSB, context, message, intent);
            }

            if(handleNextState && context.getState() == END_OF_BODY_ASSERTION_QUESTION) {
                handleNextState = messageHandler.handleEndOfBodyAssertionQuestion(responseMessageSB, context);
            }

            if(initialState == END_OF_BODY_ASSERTION_QUESTION) {
                handleNextState = messageHandler.handleEndOfBodyAssertionQuestionAnswer(responseMessageSB, context, intent);
            }

            // set initial state to final (otherwise problems occur in the next loop iterations)
            initialState = FINAL;

        } while(handleNextState);

        System.out.println("New state is: " + APITestingBot.channelModelingContexts.get(channel).getState().name());


        if(context.getState() == FINAL) {
            if(messengerType == ROCKET_CHAT && context.getMicroserviceComponent() != null) {
                // store test case (as a suggestion)
                int versionedModelId = ((Long) context.getMicroserviceComponent().get("versionedModelId")).intValue();
                try {
                    Context.get().invoke("i5.las2peer.services.modelPersistenceService.ModelPersistenceService",
                            "addTestSuggestion", new Serializable[]{versionedModelId, context.toTestModel(),
                                    "Modeled with API testing bot."});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if(messengerType == GITHUB_ISSUES || messengerType == GITHUB_PR) {
                try {
                    // generate code for the test case
                    String code = (String) Context.get().invoke("i5.las2peer.services.codeGenerationService.CodeGenerationService",
                            "generateTestMethod", new Serializable[]{ context.toTestModel().getTestCases().get(0) });

                    // post generated code as a comment
                    responseMessageSB.append("Here is the generated test method code:");
                    responseMessageSB.append("\n");
                    responseMessageSB.append("```java");
                    responseMessageSB.append(code);
                    responseMessageSB.append("```");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            APITestingBot.channelModelingContexts.remove(channel);
        }

        String responseMessage = responseMessageSB.toString();

        if(responseMessage.isEmpty()) responseMessage = "Error!";

        JSONObject res = new JSONObject();
        res.put("text", responseMessage);
        res.put("closeContext", context.getState() == FINAL);

        return Response.status(200).entity(res.toJSONString()).build();
    }

    /**
     * Gets called by SBF if user comments "@testingbot code".
     *
     * @param body
     * @return Response message containing the generated code.
     */
    @POST
    @Path("/test/code")
    public Response generateTestCode(String body) {
        String responseMessage = "";

        JSONObject bodyJSON = (JSONObject) JSONValue.parse(body);
        String message = (String) bodyJSON.get("msg");
        String messenger = (String) bodyJSON.get("messenger");
        MessengerType messengerType = MessengerType.fromString(messenger);
        String channel = (String) bodyJSON.get("channel");

        if(message.equalsIgnoreCase("@testingbot code") && messengerType == GITHUB_PR &&
                PRTestGenHelper.generatedTestCases.containsKey(channel)) {
            TestCase generatedTestCase = PRTestGenHelper.generatedTestCases.get(channel);

            // generate code for the test case
            try {
                String code = (String) Context.get().invoke("i5.las2peer.services.codeGenerationService.CodeGenerationService",
                        "generateTestMethod", new Serializable[]{ generatedTestCase });

                // post generated code as a comment
                responseMessage += "Here is the generated test method code:";
                responseMessage += "\n";
                responseMessage += "```java\n";
                responseMessage += code;
                responseMessage += "```";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        JSONObject res = new JSONObject();
        res.put("text", responseMessage);

        return Response.status(200).entity(res.toJSONString()).build();
    }

    /**
     * Endpoint that receives the webhook events from the GitHub app.
     * The events are redirected to the SBF (e.g., to react to issue or PR comments).
     * If the event is a pull request workflow run event, it is also checked if the API testing bot can generate
     * a test case for an operation that has been added or changed within the pull request.
     *
     * @param body Event payload
     * @param eventName Name of GitHub event
     * @param gitHubAppId Id of GitHub app
     * @return 200
     */
    @POST
    @Path("/github/webhook/{gitHubAppId}")
    public Response receiveWebhookEvent(String body, @HeaderParam("X-GitHub-Event") String eventName,
                                        @PathParam("gitHubAppId") int gitHubAppId) {
        APITestingBot service = (APITestingBot) Context.get().getService();
        if(service.getGitHubAppId() != gitHubAppId) return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).build();

        // redirect event to SBF
        this.redirectWebhookEventToSBF(gitHubAppId, eventName, body);

        JSONObject jsonBody = (JSONObject) JSONValue.parse(body);
        if(PRTestGenHelper.isRelevantWorkflowEvent(eventName, jsonBody)) {
            PRTestGenHelper.handleWorkflowEvent(jsonBody, service.getBotManagerURL(), service.getGitHubAppId(),
                    service.getGitHubAppPrivateKey());
        }

        return Response.status(HttpURLConnection.HTTP_OK).build();
    }

    private void redirectWebhookEventToSBF(int gitHubAppId, String eventName, String body) {
        APITestingBot service = (APITestingBot) Context.get().getService();
        String sbfWebhookUrl = service.getBotManagerURL() + "/github/webhook/" + gitHubAppId;
        Unirest.post(sbfWebhookUrl).body(body).header("X-GitHub-Event", eventName).asEmpty();
    }

}

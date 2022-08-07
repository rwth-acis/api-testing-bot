package i5.las2peer.services.apiTestingBot;

import i5.las2peer.api.Context;
import i5.las2peer.services.apiTestingBot.chat.Intent;
import i5.las2peer.services.apiTestingBot.chat.MessageHandler;
import i5.las2peer.services.apiTestingBot.context.TestModelingContext;
import i5.las2peer.services.apiTestingBot.context.TestModelingState;
import io.swagger.annotations.Api;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import java.io.Serializable;

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
        MessageHandler messageHandler = new MessageHandler(service.getCaeBackendURL());

        do {
            if (initialState == INIT && intent.equals(Intent.MODEL_TEST)) {
                handleNextState = messageHandler.handleInit(responseMessageSB, context);
            }

            if (handleNextState && context.getState() == SELECT_PROJECT) {
                handleNextState = messageHandler.handleProjectSelectionQuestion(responseMessageSB, context, channel);
            }

            if (initialState == SELECT_PROJECT) {
                handleNextState = messageHandler.handleProjectSelection(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == SELECT_MICROSERVICE) {
                handleNextState = messageHandler.handleMicroserviceSelectionQuestion(responseMessageSB, context);
            }

            if (initialState == SELECT_MICROSERVICE) {
                handleNextState = messageHandler.handleMicroserviceSelection(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == NAME_TEST_CASE) {
                handleNextState = messageHandler.handleTestCaseNameQuestion(responseMessageSB);
            }

            if (initialState == NAME_TEST_CASE) {
                handleNextState = messageHandler.handleTestCaseName(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == SELECT_METHOD) {
                handleNextState = messageHandler.handleMethodSelectionQuestion(responseMessageSB, context);
            }

            if (initialState == SELECT_METHOD) {
                handleNextState = messageHandler.handleMethodSelection(responseMessageSB, context, message);
            }

            if (handleNextState && context.getState() == ENTER_PATH_PARAMS) {
                handleNextState = messageHandler.handlePathParamsQuestion(responseMessageSB, context);
            }

            if ((handleNextState && context.getState() == ENTER_PATH_PARAMS) || initialState == ENTER_PATH_PARAMS) {
                handleNextState = messageHandler.handlePathParams(responseMessageSB, context, message);
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
        String responseMessage = responseMessageSB.toString();

        if(responseMessage.isEmpty()) responseMessage = "Error!";

        System.out.println("New state is: " + APITestingBot.channelModelingContexts.get(channel).getState().name());

        JSONObject res = new JSONObject();
        res.put("text", responseMessage);
        res.put("closeContext", context.getState() == FINAL);

        if(context.getState() == FINAL) {
            if(context.getMicroserviceComponent() != null) {
                // store test case (as a suggestion)
                int versionedModelId = ((Long) context.getMicroserviceComponent().get("versionedModelId")).intValue();
                try {
                    Context.get().invoke("i5.las2peer.services.modelPersistenceService.ModelPersistenceService",
                            "addTestSuggestion", new Serializable[]{versionedModelId, context.toTestModel(),
                                    "Modeled with API testing bot."});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            APITestingBot.channelModelingContexts.remove(channel);
        }

        return Response.status(200).entity(res.toJSONString()).build();
    }


}

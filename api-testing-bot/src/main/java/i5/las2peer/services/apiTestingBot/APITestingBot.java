package i5.las2peer.services.apiTestingBot;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.apiTestingBot.context.TestModelingContext;
import i5.las2peer.services.apiTestingBot.util.OpenAPIUtils;
import io.swagger.annotations.Api;
import org.json.simple.JSONObject;

import java.util.HashMap;

@Api
@ServicePath("/apitestingbot")
@ManualDeployment
public class APITestingBot extends RESTService {

    public static HashMap<String, TestModelingContext> channelModelingContexts = new HashMap<>();

    private String botManagerURL;
    private String caeBackendURL;

    /**
     * Id of GitHub app that the bot uses.
     */
    private int gitHubAppId;

    /**
     * Private key of GitHub app that the bot uses.
     */
    private String gitHubAppPrivateKey;

    public APITestingBot() {
        setFieldValues();
    }

    @Override
    protected void initResources() {
        getResourceConfig().register(RESTResources.class);
    }

    /**
     * Sends a chat message describing the changes between the two given OpenAPI documents.
     * @param openAPIDocOld Old OpenAPI document.
     * @param openAPIDocUpdated Updated OpenAPI document.
     * @param sbfBotName Name of the bot that should send the message.
     * @param messenger Messenger that should be used by the bot.
     * @param channel Channel to which the message should be posted.
     */
    public void sendAPIDocChangesMessage(String openAPIDocOld, String openAPIDocUpdated, String sbfBotName,
                                         String messenger, String channel) {
        // only send a message if document has changed
        if (OpenAPIUtils.docUnchanged(openAPIDocOld, openAPIDocUpdated)) return;

        String message = OpenAPIUtils.getDiffDescriptionMessage(openAPIDocOld, openAPIDocUpdated, messenger);

        // create monitoring message that triggers a webhook call to the SBF
        // this will trigger a chat message
        String webhookUrl = botManagerURL + "/bots/" + sbfBotName + "/webhook";
        JSONObject webhook = createWebhook(webhookUrl, createWebhookPayload(message, messenger, channel));
        JSONObject monitoringMessage = new JSONObject();
        monitoringMessage.put("webhook", webhook);

        Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_1, monitoringMessage.toJSONString());
    }

    /**
     * Creates a JSONObject that can be used as the content of a monitoring message to trigger a webhook call.
     *
     * @param url            Webhook URL
     * @param webhookPayload Payload of the webhook call
     * @return JSONObject that can be used as the content of a monitoring message to trigger a webhook call.
     */
    private JSONObject createWebhook(String url, JSONObject webhookPayload) {
        JSONObject webhook = new JSONObject();
        webhook.put("url", url);
        webhook.put("payload", webhookPayload);
        return webhook;
    }

    /**
     * Creates the payload for a webhook call to the SBF that will trigger a chat message.
     *
     * @param message   Message that the bot should send.
     * @param messenger Messenger that should be used by the bot.
     * @param channel   Channel to which the message should be posted.
     * @return JSONObject representing the payload for a webhook call to the SBF that will trigger a chat message.
     */
    private JSONObject createWebhookPayload(String message, String messenger, String channel) {
        JSONObject webhookPayload = new JSONObject();
        webhookPayload.put("event", "chat_message");
        webhookPayload.put("message", message);
        webhookPayload.put("messenger", messenger);
        webhookPayload.put("channel", channel);
        return webhookPayload;
    }

    public String getCaeBackendURL() {
        return caeBackendURL;
    }

    public String getBotManagerURL() {
        return botManagerURL;
    }

    public int getGitHubAppId() {
        return gitHubAppId;
    }

    public String getGitHubAppPrivateKey() {
        return gitHubAppPrivateKey;
    }
}

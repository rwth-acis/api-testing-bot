package i5.las2peer.services.apiTestingBot;

import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;

/**
 * Util class for creating messages that describe OpenAPI document changes.
 */
public class OpenAPIUtils {

    /**
     * Calculates the differences between the given OpenAPI documents and creates a chat message that describes them.
     * @param openAPIDocOld Old OpenAPI document.
     * @param openAPIDocUpdated Updated OpenAPI document.
     * @param messenger Messenger for which the message should be generated.
     * @return Chat message that describes the differences between the given OpenAPI documents.
     */
    public static String getDiffDescriptionMessage(String openAPIDocOld, String openAPIDocUpdated, String messenger) {
        ChangedOpenApi diff = OpenAPIUtils.getDocDifferences(openAPIDocOld, openAPIDocUpdated);

        if(messenger.equals("RocketChat")) {
            return new RocketChatRender().render(diff);
        }
        return "Unsupported message format!";
    }

    /**
     * Checks if the given OpenAPI document is unchanged.
     * @param openAPIDocOld Old OpenAPI document.
     * @param openAPIDocUpdated Updated OpenAPI document.
     * @return Whether the given OpenAPI document is unchanged.
     */
    public static boolean docUnchanged(String openAPIDocOld, String openAPIDocUpdated) {
        return OpenAPIUtils.getDocDifferences(openAPIDocOld, openAPIDocUpdated).isUnchanged();
    }

    /**
     * Calculates the differences between the two given OpenAPI documents.
     * @param openAPIDocOld Old OpenAPI document.
     * @param openAPIDocUpdated Updated OpenAPI document.
     * @return Differences between the two given OpenAPI documents.
     */
    private static ChangedOpenApi getDocDifferences(String openAPIDocOld, String openAPIDocUpdated) {
        return OpenApiCompare.fromContents(openAPIDocOld, openAPIDocUpdated);
    }

}

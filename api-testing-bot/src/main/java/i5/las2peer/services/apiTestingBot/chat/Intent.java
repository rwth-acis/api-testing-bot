package i5.las2peer.services.apiTestingBot.chat;

import java.util.List;

/**
 * Rasa intents related to the API testing bot.
 */
public class Intent {
    public static final String MODEL_TEST = "modeltest";

    public static final String YES = "yes";
    public static final String NO = "no";

    public static final String GET = "request_method_get";
    public static final String POST = "request_method_post";
    public static final String PUT = "request_method_put";
    public static final String DELETE = "request_method_delete";

    public static final List<String> REQUEST_METHOD_INTENTS = List.of(GET, POST, PUT, DELETE);

    public static String toRequestMethod(String intent) {
        switch(intent) {
            case Intent.GET:
                return "GET";
            case Intent.POST:
                return "POST";
            case Intent.PUT:
                return "PUT";
            case Intent.DELETE:
                return "DELETE";
            default:
                return "GET";
        }
    }
}

package i5.las2peer.services.apiTestingBot.codex;

import kong.unirest.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class CodexAPI {

    public class CodexAPIException extends Exception {
        public CodexAPIException(String message) {
            super(message);
        }
    }

    private static final String COMPLETIONS_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private String MODEL_NAME = "gpt-3.5-turbo";
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_TOKENS = 100;

    private String openAISystemPrompt =  "You are an programming assistant that completes code. You will not repeat the provided code, you will continue with the completion where the user stopped.";

    private String apiToken;

    public CodexAPI(String apiToken, String model, String systemPrompt) {
        this.apiToken = apiToken;
        this.MODEL_NAME = model;
        this.openAISystemPrompt = systemPrompt;
    }

    public JSONArray insert(String input, String stop) throws CodexAPIException {
        JSONObject body = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role","system");
        systemMessage.put("content",openAISystemPrompt);
        messages.add(systemMessage);
        JSONObject userMessage = new JSONObject();

        userMessage.put("role","user");
        userMessage.put("content",input);
        messages.add(userMessage);

        body.put("model", MODEL_NAME);
        body.put("messages", messages);
        //body.put("suffix", suffix);
        body.put("stop",stop);
        body.put("temperature", TEMPERATURE);
        body.put("max_tokens", MAX_TOKENS);
        body.put("n", 1);
        //body.put("stop", stop);

        HttpResponse<String> res = Unirest.post(COMPLETIONS_ENDPOINT)
                .basicAuth("", apiToken)
                .body(body.toJSONString())
                .contentType(ContentType.APPLICATION_JSON.toString())
                .asString();

        if(res.isSuccess()) {
            JSONObject jsonRes = (JSONObject) JSONValue.parse(res.getBody());
            System.out.println("--------------\n" + "Result: " + jsonRes.toJSONString() + "\n--------------\n");
            JSONArray choices = (JSONArray) jsonRes.get("choices");
            return choices;
        } else {
            throw new CodexAPIException("An error occurred while using the Codex API. Status code: " + res.getStatus()
                    + ", message: " + res.getBody());
        }
    }
}

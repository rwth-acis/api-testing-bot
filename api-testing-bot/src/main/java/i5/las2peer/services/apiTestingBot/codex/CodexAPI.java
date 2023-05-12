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

    private static final String COMPLETIONS_ENDPOINT = "https://api.openai.com/v1/edits";
    private String MODEL_NAME = "code-davinci-edit-001";
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_TOKENS = 100;

    private String apiToken;

    public CodexAPI(String apiToken, String model) {
        this.apiToken = apiToken;
        this.MODEL_NAME = model;
    }

    public JSONArray insert(String input, String instruction) throws CodexAPIException {
        JSONObject body = new JSONObject();
        body.put("model", MODEL_NAME);
        body.put("input", input);
        //body.put("suffix", suffix);
        body.put("instruction",instruction);
        body.put("temperature", TEMPERATURE);
        //body.put("max_tokens", MAX_TOKENS);
        body.put("n", 1);
        //body.put("stop", stop);

        HttpResponse<String> res = Unirest.post(COMPLETIONS_ENDPOINT)
                .basicAuth("", apiToken)
                .body(body.toJSONString())
                .contentType(ContentType.APPLICATION_JSON.toString())
                .asString();

        if(res.isSuccess()) {
            JSONObject jsonRes = (JSONObject) JSONValue.parse(res.getBody());
            JSONArray choices = (JSONArray) jsonRes.get("choices");
            return choices;
        } else {
            throw new CodexAPIException("An error occurred while using the Codex API. Status code: " + res.getStatus()
                    + ", message: " + res.getBody());
        }
    }
}

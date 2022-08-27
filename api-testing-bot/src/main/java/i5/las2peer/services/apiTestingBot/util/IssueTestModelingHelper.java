package i5.las2peer.services.apiTestingBot.util;

import i5.las2peer.apiTestModel.StatusCodeAssertion;
import i5.las2peer.services.apiTestingBot.APITestingBot;
import i5.las2peer.services.apiTestingBot.context.TestModelingContext;
import org.json.simple.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueTestModelingHelper {

    private static final Pattern issueFormPattern = Pattern
            .compile("### Request method\n\n(?<method>.+)\n\n### Request Path\n\n(?<path>.+)\n\n### Request Body\n\n(?<body>.+)\n\n### Expected Status Code\n\n(?<statuscode>.+)\n\n### Response Body Assertions\n\n(?<bodyassertions>.+)", Pattern.DOTALL);

    public static boolean isRelevantIssueEvent(String eventName, JSONObject eventPayload) {
        if (eventName.equals("issues")) {
            String action = (String) eventPayload.get("action");
            if (action.equals("opened")) {
                JSONObject issue = (JSONObject) eventPayload.get("issue");
                String body = (String) issue.get("body");
                return issueFormPattern.matcher(body).matches();
            }
        }
        return false;
    }

    public static boolean handleIssueEvent(JSONObject eventPayload) {
        JSONObject issue = (JSONObject) eventPayload.get("issue");
        String body = (String) issue.get("body");
        Matcher matcher = issueFormPattern.matcher(body);
        matcher.find();
        String requestMethod = matcher.group("method");
        String requestPath = matcher.group("path");
        String requestBody = matcher.group("body");
        if(requestBody.equals("_No response_")) requestBody = "";
        else {
            requestBody = requestBody.split("```json\n")[1].split("```")[0];
        }
        String expectedStatusCode = matcher.group("statuscode");
        expectedStatusCode = expectedStatusCode.substring(0, 3);
        boolean includeBodyAssertions = matcher.group("bodyassertions").contains("X");

        TestModelingContext context = new TestModelingContext();
        context.setTestCaseName("Case");
        context.setRequestMethod(requestMethod);
        context.setRequestPath(requestPath);
        context.setPathParamValues(new JSONObject());
        context.setRequestBody(requestBody);
        context.getAssertions().add(new StatusCodeAssertion(0, Integer.parseInt(expectedStatusCode)));
        APITestingBot.channelModelingContexts.put(getChannelName(eventPayload), context);
        return includeBodyAssertions;
    }

    public static String getChannelName(JSONObject eventPayload) {
        // get repo information
        JSONObject repo = (JSONObject) eventPayload.get("repository");
        String fullName = (String) repo.get("full_name");

        // get issue number
        JSONObject issue = (JSONObject) eventPayload.get("issue");
        int issueNumber = ((Long) issue.get("number")).intValue();
        return fullName + "#" + issueNumber;
    }
}

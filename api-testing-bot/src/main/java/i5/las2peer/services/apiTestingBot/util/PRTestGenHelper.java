package i5.las2peer.services.apiTestingBot.util;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.apiTestModel.*;
import i5.las2peer.services.apiTestingBot.APITestingBot;
import i5.las2peer.services.apiTestingBot.chat.GHMessageHandler;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.kohsuke.github.GHArtifact;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHWorkflowRun;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PRTestGenHelper {

    /**
     * The spec-based test case generation in GitHub pull requests expects that there exists a GitHub actions workflow
     * that generates an artifact containing the OpenAPI documentation.
     *
     * This variable contains the name of the artifact.
     */
    private static final String OPENAPI_DOC_ARTIFACT_NAME = "swagger.json";

    /**
     * The spec-based test case generation in GitHub pull requests expects that there exists a GitHub actions workflow
     * that generates an artifact containing the OpenAPI documentation.
     *
     * This variable contains the filename of the OpenAPI document.
     */
    private static final String OPENAPI_DOC_FILE_NAME = "swagger.json";

    /**
     * Stores the latest test case that has been generated for a GitHub pull request.
     * The key is [OWNER]/[REPO NAME]#[PR NUMBER].
     * This map is needed, because the users can ask the bot to generate the test case code.
     * Then, the bot needs to remember which test case got suggested previously.
     */
    public static HashMap<String, TestCase> generatedTestCases = new HashMap<>();

    private static String BOT_COMMAND_OVERVIEW = "---\n\n" +
                                                 "<details>\n" +
                                                 "<summary>Testing Bot commands</summary>\n" +
                                                 "<br />\n\n" +
                                                 "You can use the following commands:\n" +
                                                 "- `@testingbot code` will generate the code for the test case\n" +
                                                 "</details>";

    /**
     * Handles a relevant workflow event, i.e., an event that tells that a workflow run of a pull request finished
     * successfully. Checks if OpenAPI docs are available for both the base branch and the latest pull request state.
     * This allows to compute the changes of the OpenAPI documentation (that were introduced by the pull request)
     * and to try generating a test case for the changed operations. If a test case could be generated, the bot posts a
     * comment to the pull request, where the test case is presented.
     *
     * @param eventPayload Payload of GitHub event
     * @param botManagerUrl URL of SBFManager
     * @param gitHubAppId Id of GitHub app that the bot uses
     * @param gitHubAppPrivateKey Private key of GitHub app that the bot uses
     */
    public static void handleWorkflowEvent(JSONObject eventPayload, String botManagerUrl, int gitHubAppId, String gitHubAppPrivateKey) {
        JSONObject repository = (JSONObject) eventPayload.get("repository");
        String repoFullName = (String) repository.get("full_name");
        JSONObject workflowRun = (JSONObject) eventPayload.get("workflow_run");
        long workflowRunId = (Long) workflowRun.get("id");

        // check if workflow run of PR contains OpenAPI doc as an artifact
        if(!containsSwaggerArtifact(repoFullName, workflowRunId, gitHubAppId, gitHubAppPrivateKey)) return;

        // get pull request
        JSONObject pullRequest = (JSONObject) ((JSONArray) workflowRun.get("pull_requests")).get(0);

        // check if the latest workflow run on the base branch of the pull request contains OpenAPI doc as an artifact
        long baseWorkflowRunId = getPRBaseWorkflowRunId(repoFullName, pullRequest, gitHubAppId, gitHubAppPrivateKey);
        if(!containsSwaggerArtifact(repoFullName, baseWorkflowRunId, gitHubAppId, gitHubAppPrivateKey)) return;

        // OpenAPI doc is given for both base branch and current PR state => load both files
        String swaggerJson = getSwaggerJsonContent(getSwaggerJsonArtifact(repoFullName, workflowRunId, gitHubAppId, gitHubAppPrivateKey));
        String swaggerJsonBase = getSwaggerJsonContent(getSwaggerJsonArtifact(repoFullName, baseWorkflowRunId, gitHubAppId, gitHubAppPrivateKey));
        if(swaggerJson == null || swaggerJsonBase == null) return;

        String testGenServiceResult = callTestGenService(swaggerJson, swaggerJsonBase);
        if(testGenServiceResult == null) return;

        JSONObject resultJSON = (JSONObject) JSONValue.parse(testGenServiceResult);
        if(!resultJSON.containsKey("testCase")) return;

        JSONObject testCaseJSON = (JSONObject) resultJSON.get("testCase");
        String testCaseDescription = (String) resultJSON.get("description");
        TestCase testCase = new TestCase(testCaseJSON);

        String channel = repoFullName + "#" + pullRequest.get("number");
        postPRComment(channel, getTestCasePresentationComment(testCase, testCaseDescription), botManagerUrl);

        generatedTestCases.put(channel, testCase);
    }

    /**
     * Workflow run events are only relevant if:
     * - run is completed
     * - run event is a pull request
     * - run was successful
     *
     * @param eventName Name of GitHub event
     * @param eventPayload Payload of GitHub event
     * @return Whether the given event is a relevant workflow event.
     */
    public static boolean isRelevantWorkflowEvent(String eventName, JSONObject eventPayload) {
        if(eventName.equals("workflow_run")) {
            String action = (String) eventPayload.get("action");
            if (action.equals("completed")) {
                // check if workflow run belongs to a pull request and was successful
                JSONObject workflowRun = (JSONObject) eventPayload.get("workflow_run");
                String conclusion = (String) workflowRun.get("conclusion");
                String event = (String) workflowRun.get("event");
                if (event.equals("pull_request") && conclusion.equals("success")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Calls the method "openAPIDiffToTest" of the APITestGenService.
     *
     * @param swaggerJson Latest OpenAPI doc of PR
     * @param swaggerJsonBase OpenAPI doc of PR base branch
     * @return Test case and description, if a test case could be generated.
     */
    private static String callTestGenService(String swaggerJson, String swaggerJsonBase) {
        try {
            return (String) Context.get().invoke("i5.las2peer.services.apiTestGenService.APITestGenService",
                    "openAPIDiffToTest", new Serializable[]{ swaggerJsonBase, swaggerJson });
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Posts a comment to a GitHub pull request.
     *
     * @param channel [OWNER]/[REPO NAME]#[PR NUMBER]
     * @param comment Comment to post
     * @param botManagerUrl URL of SBFManager
     */
    private static void postPRComment(String channel, String comment, String botManagerUrl) {
        String webhookUrl = botManagerUrl + "/bots/" + "CAEBot" + "/webhook";
        String messenger = "GitHub Pull Requests";
        JSONObject webhook = APITestingBot.createWebhook(webhookUrl, APITestingBot.createWebhookPayload(comment, messenger, channel));
        JSONObject monitoringMessage = new JSONObject();
        monitoringMessage.put("webhook", webhook);

        Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_1, monitoringMessage.toJSONString());
    }

    /**
     * Creates a pull request comment text presenting the given test case.
     *
     * @param testCase Test case
     * @param testCaseDescription Explanation why test case has been generated
     * @return Pull request comment text presenting the given test case.
     */
    private static String getTestCasePresentationComment(TestCase testCase, String testCaseDescription) {
        // explain why test case has been generated and give test case name
        String message = "I have generated the following test case:\n";
        message += testCaseDescription;
        message += "\n\n";
        message += "" + testCase.getName() + "\n---\n";

        // show request method and path
        TestRequest request = testCase.getRequests().get(0);
        message += GHMessageHandler.getGitHubTestDescription(request);

        // append overview of available bot commands
        message += BOT_COMMAND_OVERVIEW;

        return message;
    }

    /**
     * Checks if the workflow run with the given id contains an OpenAPI doc artifact.
     *
     * @param repoFullName Full name of repository
     * @param workflowRunId Id of GitHub actions workflow run
     * @param gitHubAppId Id of GitHub app that the bot uses
     * @param gitHubAppPrivateKey Private key of GitHub app that the bot uses
     * @return Whether the workflow run with the given id contains an OpenAPI doc artifact.
     */
    private static boolean containsSwaggerArtifact(String repoFullName, long workflowRunId, int gitHubAppId, String gitHubAppPrivateKey) {
        return getSwaggerJsonArtifact(repoFullName, workflowRunId, gitHubAppId, gitHubAppPrivateKey) != null;
    }

    /**
     * Returns the id of the workflow run that belongs to the base branch of the given pull request.
     *
     * @param repoFullName Full name of repository
     * @param pullRequest Pull request info
     * @param gitHubAppId Id of GitHub app that the bot uses
     * @param gitHubAppPrivateKey Private key of GitHub app that the bot uses
     * @return Id of the workflow run that belongs to the base branch of the given pull request.
     */
    private static long getPRBaseWorkflowRunId(String repoFullName, JSONObject pullRequest, int gitHubAppId, String gitHubAppPrivateKey) {
        JSONObject base = (JSONObject) pullRequest.get("base");
        String baseBranchName = (String) base.get("ref");
        String baseSHA = (String) base.get("sha");

        try {
            GitHubAppHelper helper = new GitHubAppHelper(gitHubAppId, gitHubAppPrivateKey);
            GHRepository gitHubRepo = helper.getGitHubInstance(repoFullName).getRepository(repoFullName);
            List<GHWorkflowRun> runs = gitHubRepo.queryWorkflowRuns().branch(baseBranchName).list().toList();
            for(GHWorkflowRun run : runs) {
                if(run.getHeadSha().equals(baseSHA)) {
                    return run.getId();
                }
            }
        } catch (GitHubAppHelper.GitHubAppHelperException | IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Searches for the artifact of the given workflow run that contains the OpenAPI doc.
     *
     * @param repoFullName Full name of repository
     * @param workflowRunId Id of GitHub actions workflow run
     * @param gitHubAppId Id of GitHub app that the bot uses
     * @param gitHubAppPrivateKey Private key of GitHub app that the bot uses
     * @return Artifact of the given workflow run that contains the OpenAPI doc.
     */
    private static GHArtifact getSwaggerJsonArtifact(String repoFullName, long workflowRunId, int gitHubAppId, String gitHubAppPrivateKey) {
        try {
            GitHubAppHelper helper = new GitHubAppHelper(gitHubAppId, gitHubAppPrivateKey);
            GHRepository gitHubRepo = helper.getGitHubInstance(repoFullName).getRepository(repoFullName);
            List<GHArtifact> artifacts = gitHubRepo.getWorkflowRun(workflowRunId).listArtifacts().toList();
            for(GHArtifact artifact : artifacts) {
                if(artifact.getName().equals(OPENAPI_DOC_ARTIFACT_NAME)) {
                    return artifact;
                }
            }
        } catch (GitHubAppHelper.GitHubAppHelperException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Downloads the given artifact from GitHub, extracts it, and searches for the OpenAPI doc file.
     *
     * @param artifact Artifact from GitHub workflow run
     * @return Content of OpenAPI doc file
     */
    private static String getSwaggerJsonContent(GHArtifact artifact) {
        try {
            return artifact.download(is -> {
                ZipInputStream zipInputStream = new ZipInputStream(is);
                // search for file
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                while (nextEntry != null) {
                    if (nextEntry.getName().equals(OPENAPI_DOC_FILE_NAME))
                        break;
                    nextEntry = zipInputStream.getNextEntry();
                }
                // return file content as String
                return IOUtils.toString(zipInputStream, StandardCharsets.UTF_8);
            });
        } catch (IOException e) {
            return null;
        }
    }

}

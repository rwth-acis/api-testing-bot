package i5.las2peer.services.apiTestingBot.codex;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import i5.las2peer.apiTestModel.BodyAssertion;
import i5.las2peer.apiTestModel.BodyAssertionOperator;
import i5.las2peer.apiTestModel.RequestAssertion;
import i5.las2peer.apiTestModel.TestRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CodexTestGen {

    public static final String BASE_PROMPT_CLASS_NAME = "Test";
    public static final String BASE_PROMPT_METHOD_NAME = "test";

    private String codexAPIToken;

    public CodexTestGen(String codexAPIToken) {
        this.codexAPIToken = codexAPIToken;
    }

    public TestRequest descriptionToTestModel(String testCaseDescription) throws CodexAPI.CodexAPIException, IOException, CodeToTestModel.CodeToTestModelException {
        String testCode = descriptionToCode(testCaseDescription);
        return new CodeToTestModel().convert(testCode);
    }

    private String descriptionToCode(String testCaseDescription) throws CodexAPI.CodexAPIException, IOException, CodeToTestModel.CodeToTestModelException {
        InputStream in = getClass().getResourceAsStream("/basePrompt.java");
        String code = new String(in.readAllBytes(), StandardCharsets.UTF_8);

        // put test case description into method comment
        code = code.replace("[INPUT]", testCaseDescription);

        // insert rest of c.sendRequest(...)
        code = insert(code, ",");
        code = code.replace("[insert2]", "[insert]");
        code = insert(code, ";");

        // insert status code assertion
        code = addInsertTag(code);
        code = code.replace("[insert]", "assertThat(statusCode, [insert];");
        code = insert(code, ";");

        // insert other assertions
        int iterations = 5;
        for(int i = 0; i < iterations; i++) {
            String updatedContent = addInsertTag(new String(code));
            updatedContent = updatedContent.replace("[insert]", "assertThat(response, [insert];");
            updatedContent = insert(updatedContent, ";");

            // get body assertions from current request model
            List<BodyAssertion> bodyAssertions = new ArrayList<>();
            try {
                bodyAssertions = getBodyAssertionsFromCode(updatedContent);
            } catch (CodeToTestModel.CodeToTestModelException e) {
                break;
            }
            if(bodyAssertions.isEmpty()) continue;

            // get newest assertion
            BodyAssertion latest = bodyAssertions.get(bodyAssertions.size()-1);
            if(containsIrrelevantHasFieldAssertion(latest, testCaseDescription)) {
                // stop code generation here
                break;
            }

            if(isDuplicate(latest, bodyAssertions.subList(0, bodyAssertions.size()-1))) {
                // stop code generation here
                break;
            }

            code = updatedContent;
        }

        return code;
    }

    private static boolean containsIrrelevantHasFieldAssertion(BodyAssertion bodyAssertion, String testCaseDescription) {
        BodyAssertionOperator operator = bodyAssertion.getOperator();
        while(operator != null) {
            if(operator.getOperatorId() == 1) {
                // this is a "has field" operator
                // check if the field name was mentioned in the test case description given by the user
                String fieldName = operator.getInputValue();
                if(!(testCaseDescription.contains(" " + fieldName + " ")
                        || testCaseDescription.contains(" " + fieldName)
                        || testCaseDescription.contains("\"" + fieldName)
                        || testCaseDescription.contains("'" + fieldName))) {
                    // might be irrelevant
                    return true;
                }
            }
            operator = operator.getFollowingOperator();
        }
        return false;
    }

    private static boolean isDuplicate(BodyAssertion latest, List<BodyAssertion> previousAssertions) {
        for(BodyAssertion a : previousAssertions) {
            if(latest.getOperator().toString().equals(a.getOperator().toString())) return true;
        }
        return false;
    }

    private static List<BodyAssertion> getBodyAssertionsFromCode(String code) throws CodeToTestModel.CodeToTestModelException {
        List<BodyAssertion> bodyAssertions = new ArrayList<>();

        List<RequestAssertion> assertions = new CodeToTestModel().convert(code).getAssertions();
        for(RequestAssertion assertion : assertions) {
            if(assertion instanceof BodyAssertion) {
                BodyAssertion b = (BodyAssertion) assertion;
                bodyAssertions.add(b);
            }
        }

        return bodyAssertions;
    }

    /**
     * Appends a new [insert] tag at the end of the try-block of the test method.
     *
     * @param code Java code
     * @return Given code, with a new [insert] tag at the end of the try-block of the test method.
     */
    private static String addInsertTag(String code) {
        // parse java code
        CompilationUnit unit = new JavaParser().parse(code).getResult().get();

        // get test method
        MethodDeclaration methodDeclaration = unit.getClassByName(BASE_PROMPT_CLASS_NAME).get()
                .getMethodsByName(BASE_PROMPT_METHOD_NAME).get(0);

        // add new [insert] tag at the end of the try-block
        for(Statement statement : methodDeclaration.getBody().get().getStatements()) {
            if(statement instanceof TryStmt) {
                TryStmt tryStmt = (TryStmt) statement;
                // we need to append the [insert] tag as a comment first
                tryStmt.getTryBlock().addOrphanComment(new LineComment("[insert]"));
                break;
            }
        }
        code = unit.toString();
        code = code.replace("// [insert]", "[insert]\n");
        return code;
    }

    /**
     * Uses OpenAI's Codex model to complete the given code (at the [insert] tag).
     *
     * @param code Code containing [insert] at the place where new code should be generated and inserted.
     * @param stop Character at which the code generation should stop.
     * @return Given code, where [insert] got replaced with code generated by the Codex model.
     * @throws CodexAPI.CodexAPIException If the API request was not successful.
     */
    private String insert(String code, String stop) throws CodexAPI.CodexAPIException {
        // split code into two parts - before and after [insert]
        String prompt = code.split("\\[insert]")[0];
        String suffix = code.split("\\[insert]")[1];

        // call OpenAI API
        JSONArray choices = new CodexAPI(codexAPIToken).insert(prompt, suffix, stop);

        // get first choice
        JSONObject choice = (JSONObject) choices.get(0);
        String text = (String) choice.get("text");

        // replace [insert] with generated code
        code = code.replace("[insert]", text);

        return code;
    }

}

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.List;
import java.util.Set;

public class Test {

    public static FeatureMatcher<JSONObject, Object> hasField(String featureName, Matcher<? super Object> subMatcher) {
        return new FeatureMatcher<>(subMatcher, "should have field\"" + featureName + "\"", featureName) {
            @Override
            protected Object featureValueOf(JSONObject jsonObject) {
                return jsonObject.get(featureName);
            }
        };
    }

    public static FeatureMatcher<Object, List<Object>> asJSONObjectList(Matcher<? super List<Object>> subMatcher) {
        return new FeatureMatcher<>(subMatcher, "as a JSONObjectList", "") {
            @Override
            protected List<Object> featureValueOf(Object o) {
                JSONArray arr = (JSONArray) o;
                return List.of(arr.toArray());
            }
        };
    }

    public static TypeSafeDiagnosingMatcher<JSONObject> hasField(String fieldName) {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(JSONObject jsonObject, Description mismatchDescription) {
                if (!jsonObject.containsKey(fieldName))
                    mismatchDescription.appendText("but does not have field " + fieldName);
                return jsonObject.containsKey(fieldName);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("should have field " + fieldName);
            }
        };
    }

    public static FeatureMatcher<Object, JSONObject> asJSONObject(Matcher<? super JSONObject> subMatcher) {
        return new FeatureMatcher<>(subMatcher, "as a JSONObject", "") {
            @Override
            protected JSONObject featureValueOf(Object o) {
                return (JSONObject) o;
            }
        };
    }

    public ClientResponse sendRequest(String method, String uri, String content, String contentType, String accept, Map<String, String> headers, Object... pathParameters) throws Exception {
        return super.sendRequest(method, basePath + uri, content, contentType, accept, headers);
    }

    /**
     * JUnit test: Send post request to /example and check that status code is 201 and response has field "exampleText".
     */
    @Test
    public void exampleTest() {
        MiniClientCoverage c = new MiniClientCoverage(mainPath);
        c.setConnectorEndpoint(connector.getHttpEndpoint());

        try {
            ClientResponse result = c.sendRequest("POST", mainPath + "/example", "application/json", "*/*", null);
            int statusCode = result.getHttpCode();
            Object response = JSONValue.parse(result.getResponse().trim());

            assertThat(statusCode, is(201));
            assertThat(response, asJSONObject(hasField("exampleText", isA(String.class))));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e);
        }
    }

    /**
     * JUnit test: Send post request to /example/3 with body {"num": 5, "value": "Hi"} and check that status code is 201 and response has type json object.
     */
    @Test
    public void exampleTest() {
        MiniClientCoverage c = new MiniClientCoverage(mainPath);
        c.setConnectorEndpoint(connector.getHttpEndpoint());

        try {
            ClientResponse result = c.sendRequest("POST", mainPath + "/example/{id}", """{"num": 5, "value": "Hi"}""", "application/json", "*/*", 3);
            int statusCode = result.getHttpCode();
            Object response = JSONValue.parse(result.getResponse().trim());

            assertThat(statusCode, is(201));
            assertThat(response, isA(JSONObject.class));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e);
        }
    }

    /* JUnit test: [INPUT] */
    @Test
    public void test() {
        MiniClientCoverage c = new MiniClientCoverage(mainPath);
        c.setConnectorEndpoint(connector.getHttpEndpoint());

        try {
            ClientResponse result = c.sendRequest([insert], mainPath + [insert2];
            int statusCode = result.getHttpCode();
            Object response = JSONValue.parse(result.getResponse().trim());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e);
        }
    }
}
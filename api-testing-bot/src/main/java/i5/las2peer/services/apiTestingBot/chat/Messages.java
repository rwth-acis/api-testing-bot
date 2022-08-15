package i5.las2peer.services.apiTestingBot.chat;

public class Messages {

    public static String MODEL_TEST_CASE_INTRO = "Ok, let's model a test case.";
    public static String SELECT_PROJECT_FOR_TEST_CASE = "Which project should the test case be added to? Please enter a number:";
    public static String SELECT_MICROSERVICE_FOR_TEST_CASE = "Which microservice should the test case be added to? Please enter a number:";
    public static String ENTER_TEST_CASE_NAME = "Please enter a name for the test case:";
    public static String SELECT_METHOD_TO_TEST = "We continue with the first request. Please select a method to test:";
    public static String SET_PATH_PARAM_VALUES = "The path contains path parameters whose values must be set.";
    public static String INCLUDE_REQUEST_BODY_QUESTION = "Do you want to include a JSON body to the request?";
    public static String ENTER_REQUEST_BODY = "Ok, please enter the request body:";
    public static String REQUEST_BODY_SET_INFO = "Request body was set.";
    public static String INCLUDE_ASSERTIONS_QUESTION = "Do you want to include assertions on the response to this request?";
    public static String SELECT_ASSERTION_TYPE = "Which type of assertion should be added? Please enter a number:";
    public static String ASSERTION_TYPE_STATUS_CODE = "1. Assertion on the response status code (e.g., check that status code 200 is returned)";
    public static String ASSERTION_TYPE_RESPONSE_BODY = "2. Assertion on the response body (e.g., check that response contains field \"id\")";
    public static String EXPECTED_STATUS_CODE_QUESTION = "Which response status code do you expect?";
    public static String ADDED_ASSERTION_TO_TEST = "Added assertion to the test.";
    public static String ASSERTION_OVERVIEW = "Here is an overview of the current assertions:";
    public static String ADD_ANOTHER_ASSERTION = "Do you want to add another assertion?";
    public static String SELECT_BODY_ASSERTION_TYPE = "What is the assertion supposed to check?";
    public static String BODY_ASSERTION_TYPE_1 = "1. Whether the body (or field value) has a specific type (e.g., if it is a JSONObject, a Number, ...)";
    public static String BODY_ASSERTION_TYPE_2 = "2. Whether it contains a field (e.g., if the JSONObject contains a field \"id\")";
    public static String BODY_ASSERTION_TYPE_3 = "3. Whether it is a list and contains an entry of a specific type";
    public static String BODY_ASSERTION_TYPE_4 = "4. Whether it is a list and contains an entry that contains a specific field";
    public static String BODY_ASSERTION_TYPE_5 = "5. Whether it is a list and all entries have a specific type";
    public static String BODY_ASSERTION_TYPE_6 = "6. Whether it is a list and all entries contain a specific field";
    public static String ENTER_EXPECTED_TYPE = "What type is expected? (e.g., JSONObject, JSONArray, String, Number, Boolean)";
    public static String ENTER_FIELD_NAME = "Please enter the field name/key:";
    public static String ASSERTION_PREVIEW = "The assertion now looks as follows:";
    public static String OK = "Ok.";

    public static String ENTER_NUMBER = ":warning:Please enter a number! :warning:";
    public static String NO_PROJECT_LINKED_TO_CHANNEL = ":x:Error, no project is linked to this channel. :x:";
    public static String NO_MICROSERVICE_IN_PROJECT = ":x:Error, no microservice is part of the selected project. :x:";
    public static String ERROR_LOADING_AVAILABLE_METHODS = ":x:Error, unable to load available methods of microservice. :x:";
    public static String ERROR_COULD_NOT_UNDERSTAND = "I could not understand that. Please try again.";
    public static String ERROR_COULD_NOT_UNDERSTAND_TYPE = "I could not understand that. Please enter a valid type.";
    public static String ERROR_BODY_NO_VALID_JSON = "Entered body is no valid JSON! Please try again.";

    public static String TEST_ADD_TO_PROJECT(String projectName) {
        return "The test will be added to the project \"" + projectName + "\".";
    }

    public static String TEST_ADD_TO_MICROSERVICE(String serviceName) {
        return "The test will be added to the microservice \"" + serviceName + "\".";
    }

    public static String TEST_CASE_NAME_INFO(String name) {
        return "Test case will be named " + name + ".";
    }

    public static String ENTER_NUMBER_BETWEEN(int min, int max) {
        return ":warning:Please enter a number between " + min + " and " + max + "!:warning:";
    }

    public static String TEST_METHOD_INFO(String method, String path) {
        return "The test will send a `" + method + "` request to `" + path + "`.";
    }

    public static String PATH_PARAM_SET_INFO(String paramName, String paramValue) {
        return "Setting `" + paramName + "` to \"" + paramValue + "\".";
    }

    public static String ENTER_PATH_PARAM_VALUE(String paramName) {
        return "Please enter a value for the path parameter `" + paramName + "`:";
    }

    public static String REQUEST_URL_INFO(String url) {
        return "The request URL now is `" + url + "`.";
    }

    public static String FURTHER_ASSERT_ON_FIELD_QUESTION(String fieldName) {
        return "Do you want to further edit this assertion and assert something on the field \""
                + fieldName + "\"? (e.g., check that the field has a specific type)";
    }


    public static String GH_ENTER_REQUEST_METHOD = "Please enter the request method (e.g., GET, POST,...):";
    public static String GH_ENTER_REQUEST_PATH = "Please enter the request path (e.g., /mensa/Aachen/dishes):";

    public static String GH_REQUEST_INFO(String method, String path) {
        return "The test request will be `" + method + "` `" + path + "`.";
    }
}

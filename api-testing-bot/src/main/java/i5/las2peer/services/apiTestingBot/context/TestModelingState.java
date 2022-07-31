package i5.las2peer.services.apiTestingBot.context;

public enum TestModelingState {
    /**
     * Initial state.
     */
    INIT,

    /**
     * This state is reached, if the user has entered a number to select a project that is linked to the current channel.
     * This state is also reached, if the channel is only linked to one project and this project has been selected
     * automatically.
     * After a project has been selected, the microservices of the selected project are listed.
     */
    SELECT_PROJECT,

    /**
     * This state is reached, if the user has entered a number to select a microservice for which a test should be modeled.
     * This state is also reached, if the selected project only contains one microservice and this one has been selected
     * automatically.
     * After a microservice has been selected, the user gets asked to enter a name for the test case.
     */
    SELECT_MICROSERVICE,

    /**
     * In this state, the previously entered test case name gets stored in the context.
     * Then, the user is asked to select a method (from OpenAPI doc) that should be tested.
     */
    NAME_TEST_CASE,

    /**
     * In this state is reached, if the user has entered a number to select the method that should be tested.
     * After a method has been selected, state is switched to ENTER_PATH_PARAMS if operation contains path parameters,
     * to state BODY_QUESTION otherwise.
     */
    SELECT_METHOD,

    /**
     * State used to enter all path parameters.
     */
    ENTER_PATH_PARAMS,

    /**
     * In this state, the user gets asked whether the test request should contain a JSON body.
     * Also handles answer of user to this question.
     * If the request should contain a body, the user is asked to enter it (state ENTER_BODY).
     * Otherwise, state is switched to ASSERTIONS_QUESTION.
     */
    BODY_QUESTION,

    /**
     * In this state, the user enters the request body.
     */
    ENTER_BODY,

    /**
     * In this state, the user gets asked whether the test should contain assertions on the response to this request.
     * Also handles the answer of user to this question.
     */
    ASSERTIONS_QUESTION,

    /**
     * In this state, the user gets asked which type the assertion should have.
     * Also handles the answer of user to this question.
     */
    ASSERTION_TYPE_QUESTION,

    /**
     * In this state, the user gets asked to enter a status code.
     * Also handles the answer of user to this question.
     */
    ENTER_STATUS_CODE,

    /**
     * In this state, the assertions that were modeled are listed.
     */
    ASSERTIONS_OVERVIEW,

    /**
     * In this state, the user gets asked if another assertion should be added.
     * Also handles the answer of user to this question.
     */
    ADD_ANOTHER_ASSERTION_QUESTION,

    /**
     * In this state, the user gets asked which type the body assertion should have.
     * Also handles the answer of user to this question.
     */
    BODY_ASSERTION_TYPE_QUESTION,

    /**
     * In this state, depending on the WIP assertion type, the user gets asked to enter the expected type or a field name.
     * Also handles the answer of user.
     */
    ENTER_BODY_ASSERTION_PART,

    /**
     * In this state, the user gets asked whether the current assertion should be edited further.
     * Also handles the answer of user.
     */
    END_OF_BODY_ASSERTION_QUESTION,

    FINAL
}
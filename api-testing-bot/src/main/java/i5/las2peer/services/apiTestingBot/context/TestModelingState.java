package i5.las2peer.services.apiTestingBot.context;

public enum TestModelingState {
    /**
     * Initial state.
     */
    INIT,

    API_TEST_FAMILIARITY_QUESTION,

    ENTER_TEST_CASE_DESCRIPTION,

    /**
     * This state handles the selection of a CAE project (which must be linked to the current channel).
     * In this state, the available projects are listed.
     * If there is no project linked to the channel, modeling will be stopped.
     * If there is only one project linked to the channel, it is chosen automatically.
     * If there are multiple projects linked to the channel, the users can choose one of them.
     */
    RC_SELECT_PROJECT,

    /**
     * This state handles the selection of a microservice.
     * In this state, the available microservices are listed.
     * If there is no microservice in the project, modeling will be stopped.
     * If there is only one microservice in the project, it is chosen automatically.
     * If there are multiple microservices in the project, the users can choose one of them.
     */
    RC_SELECT_MICROSERVICE,

    /**
     * In this state the users are asked to enter a name for the test case.
     * This state also handles the entered test case name and stores it in the context.
     */
    NAME_TEST_CASE,

    /**
     * In this state, the user is asked to select a method (from OpenAPI doc) that should be tested.
     * Also handles answer of user to this question.
     */
    RC_SELECT_METHOD,

    /**
     * State used to enter all path parameters.
     */
    RC_ENTER_PATH_PARAMS,

    /**
     * In this state, the user gets asked to enter the request method.
     */
    GH_METHOD_QUESTION,

    /**
     * In this state, the user enters the request method.
     */
    GH_ENTER_METHOD,

    /**
     * In this state, the user gets asked to enter the request path.
     */
    GH_PATH_QUESTION,

    /**
     * In this state, the user enters the request path.
     */
    GH_ENTER_PATH,

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
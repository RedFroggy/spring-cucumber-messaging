package fr.redfroggy.bdd.messaging.glue;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.redfroggy.bdd.messaging.scope.ScenarioScope;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.MessageChannel;

import java.io.IOException;
import java.util.List;

public class MessagingBddStepDefinition extends AbstractBddStepDefinition {

    MessagingBddStepDefinition(MessageCollector collector, List<MessageChannel> channels) {
        super(collector, channels);
    }

    /**
     * Set the request body
     * A json string structure is accepted
     * The body will be parsed to be sure the json is valid
     */
    @Given("^I set queue message body to (.*)$")
    public void setBodyTo(String body) {
        this.setBody(body);
    }

    @And("^I set queue message body path (.*) to (.*)$")
    public void setBodyWithJsonPath(String jsonPath, String value) {
        this.setBodyPathWithValue(jsonPath, value);
    }

    /**
     * Set the request body by reading a file
     * A json string structure is accepted
     * The body will be parsed to be sure the json is valid
     */
    @Given("^I set queue message body with file (.*)$")
    public void setBodyWithFile(String filePath) throws IOException {
        super.setBodyWithFile(filePath);
    }

    /**
     * Add a new header
     */
    @Given("^I set (.*) queue message header to (.*)$")
    public void header(String headerName, String headerValue) {
        this.setHeader(headerName, headerValue);
    }

    @When("^I PUSH to queue (.*)$")
    public void get(String channelName) {
        this.pushToQueue(channelName);
    }

    @When("^I POLL first message from queue (.*)$")
    public void pollFromQueue(String channelName) throws InterruptedException {
        this.readMessageFromQueue(channelName, MessageChannelAction.POLL);
    }

    @When("^I PEEK first message from queue (.*)$")
    public void peekFromQueue(String channelName) throws InterruptedException {
        this.readMessageFromQueue(channelName, MessageChannelAction.PEEK);
    }

    @Then("^Queue should have (.*) messages left$")
    public void pollFromQueue(int expectedSize) {
        checkChannelHasSize(expectedSize);
    }

    /**
     * Test that a given header exists
     */
    @Then("^queue message header (.*) should exist$")
    public void headerExists(String headerName) {
        this.checkHeaderExists(headerName, false);
    }

    /**
     * Test that a given header does not exists
     */
    @Then("^queue message header (.*) should not exist$")
    public void headerNotExists(String headerName) {
        this.checkHeaderExists(headerName, true);
    }

    /**
     * Test if a given header value is matching the expected value
     *
     */
    @Then("^queue message header (.*) should be (.*)$")
    public void headerEqual(String headerName, String headerValue) {
        this.checkHeaderHasValue(headerName, headerValue, false);
    }

    /**
     * Test if a given header value is not matching the expected value
     */
    @Then("^queue message header (.*) should not be (.*)$")
    public void headerNotEqual(String headerName, String headerValue) {
        this.checkHeaderHasValue(headerName, headerValue, true);
    }

    /**
     * Test if the response body is a valid json. The string response is parsed as a
     * JSON object ot check the integrity
     */
    @Then("^queue message body should be valid json$")
    public void bodyIsValid() throws IOException {
        this.checkJsonBody();
    }

    /**
     * Test if the response body contains a given value
     */
    @Then("^queue message body should contain (.*)$")
    public void bodyContains(String bodyValue) {
        this.checkBodyContains(bodyValue);
    }

    /**
     * Test the given json path query exists in the response body
     */
    @Then("^queue message body path (.*) should exist$")
    public void bodyPathExists(String jsonPath) throws JsonProcessingException {
        this.checkJsonPathExists(jsonPath);
    }

    /**
     * Test the given json path query doesn't exist in the response body
     */
    @Then("^queue message body path (.*) should not exist$")
    public void bodyPathDoesntExist(String jsonPath) throws JsonProcessingException {
        this.checkJsonPathDoesntExist(jsonPath);
    }

    /**
     * Test the given json path exists in the response body and match the given value
     */
    @Then("^queue message body path (.*) should be (.*)$")
    public void bodyPathEqual(String jsonPath, String value) throws JsonProcessingException {
        this.checkJsonPath(jsonPath, value, false);
    }

    /**
     * Test the given json path exists and does not match the given value
     */
    @Then("^queue message body path (.*) should not be (.*)$")
    public void bodyPathNotEqual(String jsonPath, String value) throws JsonProcessingException {
        this.checkJsonPath(jsonPath, value, true);
    }

    /**
     * Test if the json path exists in the response body and is array typed
     */
    @Then("^queue message body is typed as array for path (.*)$")
    public void bodyPathIsArray(String jsonPath) throws JsonProcessingException {
        this.checkJsonPathIsArray(jsonPath, -1);
    }

    /**
     * Test if the json path exists in the response body, is array typed and as the
     * expected length
     */
    @Then("^queue message body is typed as array using path (.*) with length (\\d+)$")
    public void bodyPathIsArrayWithLength(String jsonPath, int length) throws JsonProcessingException {
        this.checkJsonPathIsArray(jsonPath, length);
    }

    /**
     * Store a given response header to the scenario scope The purpose is to reuse
     * its value in another scenario The most common use case is the authentication
     * process
     *
     * @see ScenarioScope
     */
    @Then("^I store the value of queue message header (.*) as (.*) in scenario scope$")
    public void storeResponseHeader(String headerName, String headerAlias) {
        this.storeHeader(headerName, headerAlias);
    }

    /**
     * Store a given json path value to the scenario scope The purpose is to reuse
     * its value in another scenario The most common use case is the authentication
     * process
     *
     * @see ScenarioScope
     */
    @Then("^I store the value of queue message path (.*) as (.*) in scenario scope$")
    public void storeResponseJsonPath(String jsonPath, String jsonPathAlias) throws JsonProcessingException {
        this.storeJsonPath(jsonPath, jsonPathAlias);
    }

    /**
     * Test a scenario scope variable value match the expected one
     * @see ScenarioScope
     */
    @Then("^queue value of scenario variable (.*) should be (.*)$")
    public void scenarioVariableIsValid(String property, String value) {
        this.checkScenarioVariable(property, value);
    }
}

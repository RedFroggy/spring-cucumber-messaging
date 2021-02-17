package fr.redfroggy.bdd.messaging.glue;

import fr.redfroggy.bdd.messaging.scope.ScenarioScope;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.MessageChannel;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
    public void setBodyTo(String body) throws IOException {
        this.setBody(body);
    }

    /**
     * Add a new header
     */
    @Given("^I set (.*) header to (.*)$")
    public void header(String headerName, String headerValue) {
        this.setHeader(headerName, headerValue);
    }

    /**
     * Add multiple headers
     */
    @Given("^I set headers to:$")
    public void headers(Map<String, String> parameters) {
        this.addHeaders(parameters);
    }

    @When("^I PUSH to queue (.*)$")
    public void get(String channelName) {
        boolean pushed = this.pushToQueue(channelName);
        if (!pushed) {
            fail("Could not push to queue: " + channelName);
        }
    }

    @When("^I (.*) first message from queue (.*)$")
    public void pollFromQueue(String messageAction, String channelName) throws InterruptedException {
        MessageChannelAction channelAction = MessageChannelAction.valueOf(messageAction.toUpperCase());
        this.readMessageFromQueue(channelName, channelAction);
    }

    @Then("Queue should have (.*) messages$")
    public void pollFromQueue(int expectedSize) {
        checkChannelHasSize(expectedSize);
    }

    /**
     * Test that a given header exists
     */
    @Then("^message header (.*) should exist$")
    public void headerExists(String headerName) {
        this.checkHeaderExists(headerName, false);
    }

    /**
     * Test that a given header does not exists
     */
    @Then("^message header (.*) should not exist$")
    public void headerNotExists(String headerName) {
        this.checkHeaderExists(headerName, true);
    }

    /**
     * Test if a given header value is matching the expected value
     *
     */
    @Then("^message header (.*) should be (.*)$")
    public void headerEqual(String headerName, String headerValue) {
        this.checkHeaderHasValue(headerName, headerValue, false);
    }

    /**
     * Test if a given header value is not matching the expected value
     */
    @Then("^message header (.*) should not be (.*)$")
    public void headerNotEqual(String headerName, String headerValue) {
        this.checkHeaderHasValue(headerName, headerValue, true);
    }

    /**
     * Test if the response body is a valid json. The string response is parsed as a
     * JSON object ot check the integrity
     */
    @Then("^message body should be valid json$")
    public void bodyIsValid() throws IOException {
        this.checkJsonBody();
    }

    /**
     * Test if the response body contains a given value
     */
    @Then("^message body should contain (.*)$")
    public void bodyContains(String bodyValue) {
        this.checkBodyContains(bodyValue);
    }

    /**
     * Test the given json path query exists in the response body
     */
    @Then("^message body path (.*) should exists$")
    public void bodyPathExists(String jsonPath) {
        this.checkJsonPathExists(jsonPath);
    }

    /**
     * Test the given json path query doesn't exist in the response body
     */
    @Then("^message body path (.*) should not exist$")
    public void bodyPathDoesntExist(String jsonPath) {
        this.checkJsonPathDoesntExist(jsonPath);
    }

    /**
     * Test the given json path exists in the response body and match the given value
     */
    @Then("^message body path (.*) should be (.*)$")
    public void bodyPathEqual(String jsonPath, String value) {
        this.checkJsonPath(jsonPath, value, false);
    }

    @Then("^message body path (.*) should not have content$")
    public void emptyBodyPath(String jsonPath) {
        Object json = getJsonPath(jsonPath);

        if (json != null) {
            if (json instanceof Collection<?>) {
                assertThat(((Collection<?>) json).isEmpty()).isTrue();
            }
        }
    }

    /**
     * Test the given json path exists and does not match the given value
     */
    @Then("^message body path (.*) should not be (.*)$")
    public void bodyPathNotEqual(String jsonPath, String value) {
        this.checkJsonPath(jsonPath, value, true);
    }

    /**
     * Test if the json path exists in the response body and is array typed
     */
    @Then("^message body is typed as array for path (.*)$")
    public void bodyPathIsArray(String jsonPath) {
        this.checkJsonPathIsArray(jsonPath, -1);
    }

    /**
     * Test if the json path exists in the response body, is array typed and as the
     * expected length
     */
    @Then("^message body is typed as array using path (.*) with length (\\d+)$")
    public void bodyPathIsArrayWithLength(String jsonPath, int length) {
        this.checkJsonPathIsArray(jsonPath, length);
    }

    /**
     * Store a given response header to the scenario scope The purpose is to reuse
     * its value in another scenario The most common use case is the authentication
     * process
     *
     * @see ScenarioScope
     */
    @Then("^I store the value of message header (.*) as (.*) in scenario scope$")
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
    @Then("^I store the value of message path (.*) as (.*) in scenario scope$")
    public void storeResponseJsonPath(String jsonPath, String jsonPathAlias) {
        this.storeJsonPath(jsonPath, jsonPathAlias);
    }

    /**
     * Test a scenario scope variable value match the expected one
     * @see ScenarioScope
     */
    @Then("^value of scenario variable (.*) should be (.*)$")
    public void scenarioVariableIsValid(String property, String value) {
        this.checkScenarioVariable(property, value);
    }
}
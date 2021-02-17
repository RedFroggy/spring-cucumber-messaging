package fr.redfroggy.bdd.messaging.glue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;
import fr.redfroggy.bdd.messaging.scope.ScenarioScope;
import org.junit.Assert;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Abstract step definition implementation for a messaging system tests
 * Will perform push and poll to channels
 */
@SuppressWarnings("unchecked")
abstract class AbstractBddStepDefinition {

    // Message body to send to queue
    private String body;

    // Messaging objects
    private Message<?> message;
    BlockingQueue<Message<?>> messages;
    private final MessageHeaders headers;
    private final MessageCollector collector;
    private final List<MessageChannel> channels;

    private final ObjectMapper objectMapper;

    private static final ScenarioScope scenarioScope = new ScenarioScope();

    AbstractBddStepDefinition(MessageCollector collector, List<MessageChannel> channels) {
        this.collector = collector;
        this.channels = channels;

        headers = new MessageHeaders(new HashMap<>());
        objectMapper = new ObjectMapper();
    }

    void setHeader(String name, String value) {
        assertThat(name).isNotNull();
        assertThat(value).isNotNull();
        value = replaceDynamicParameters(value);
        headers.put(name, value);
    }

    void addHeaders(Map<String, String> newHeaders) {
        assertThat(newHeaders).isNotEmpty();
        headers.putAll(newHeaders);
    }

    /**
     * Set the messaging {@link #body}
     */
    void setBody(String body) {
        assertThat(body).isNotEmpty();

        // verify json is valid
        assertThat(JsonPath.parse(body)).isNotNull();

        this.body = replaceDynamicParameters(body);
    }

    boolean pushToQueue(String channelName) {
        MessageChannel channel = getChannelByName(channelName);
        Assert.assertNotNull(channel);

        return channel.send(new GenericMessage<>(body));
    }

    void readMessageFromQueue(String channelName, MessageChannelAction action) throws InterruptedException {
        MessageChannel channel = getChannelByName(channelName);
        Assert.assertNotNull(channel);

        messages = collector.forChannel(channel);
        assertThat(messages).isNotNull();
        assertThat(messages).isNotEmpty();

        switch (action) {
            case POLL:
                message = messages.poll();
                break;
            case TAKE:
                message = messages.take();
                break;
            default:
                message = messages.element();
        }
    }

    void checkChannelHasSize(int expectedSize) {
        assertThat(messages).isNotNull();
        assertThat(messages).hasSize(expectedSize);
    }

    /**
     * Find an header by name
     */
    String checkHeaderExists(String headerName, boolean isNot) {
        assertThat(headerName).isNotEmpty();
        assertThat(message.getHeaders()).isNotNull();
        if (!isNot) {
            assertThat(message.getHeaders().get(headerName)).isNotNull();
            return String.valueOf(message.getHeaders().get(headerName));
        } else {
            assertThat(message.getHeaders().get(headerName)).isNull();
            return null;
        }
    }

    void checkHeaderHasValue(String headerName, String headerValue, boolean isNot) {
        assertThat(headerName).isNotEmpty();

        assertThat(headerValue).isNotEmpty();

        assertThat(message.getHeaders()).isNotNull();

        String header = String.valueOf(message.getHeaders().get(headerName));
        assertThat(header).isNotNull();

        if (!isNot) {
            assertThat(header).contains(headerValue);
        } else {
            assertThat(header).doesNotContain(headerValue);
        }
    }

    /**
     * Test message payload is valid json
     */
    void checkJsonBody() throws IOException {
        String body = String.valueOf(message.getPayload());
        assertThat(body).isNotEmpty();

        // Check body json structure is valid
        objectMapper.readValue(body, Object.class);
    }

    /**
     * Test message body contains value
     */
    void checkBodyContains(String bodyValue) {
        assertThat(bodyValue).isNotEmpty();
        assertThat(String.valueOf(message.getPayload())).contains(bodyValue);
    }

    /**
     * Test json path validity
     *
     * @param jsonPath
     *            json path query
     * @return value found using <code>jsonPath</code>
     */
    Object checkJsonPathExists(String jsonPath) {
        return getJsonPath(jsonPath);
    }

    void checkJsonPathDoesntExist(String jsonPath) {
        ReadContext ctx = readPayload();

        if (ctx != null) {
            assertThat(jsonPath).isNotEmpty();

            assertThatThrownBy(() -> ctx.read(jsonPath))
                    .isExactlyInstanceOf(PathNotFoundException.class);
        }
    }

    /**
     * Test json path value
     *
     * @param jsonPath
     *            json path query
     * @param jsonValueString
     *            expected/unexpected json path value
     * @param isNot
     *            if true, test equality, inequality if false
     */
    void checkJsonPath(String jsonPath, String jsonValueString, boolean isNot) {
        Object pathValue = checkJsonPathExists(jsonPath);
        assertThat(String.valueOf(pathValue)).isNotEmpty();

        if (pathValue instanceof Collection) {
            checkJsonValue((Collection) pathValue, jsonValueString, isNot);
            return;
        }
        Object jsonValue = ReflectionTestUtils.invokeMethod(pathValue, "valueOf", jsonValueString);

        if (!isNot) {
            assertThat(pathValue).isEqualTo(jsonValue);
        } else {
            assertThat(pathValue).isNotEqualTo(jsonValue);
        }
    }

    /**
     * Test json path value
     *
     * @param pathValue
     *            json path array value
     * @param jsonValue
     *            expected/unexpected json path value
     * @param isNot
     *            if true, test equality, inequality if false
     */
    private void checkJsonValue(Collection pathValue, String jsonValue, boolean isNot) {
        assertThat(pathValue).isNotEmpty();
        Object jsonValueToEvaluate = jsonValue;
        if (pathValue.iterator().next() instanceof Boolean) {
            jsonValueToEvaluate = Boolean.valueOf(jsonValue);
        }

        if (!isNot) {
            assertThat(pathValue).contains(jsonValueToEvaluate);
        } else {
            assertThat(pathValue).doesNotContain(jsonValueToEvaluate);
        }
    }

    /**
     * Test json path is array typed and its size is matching the expected length
     *
     * @param jsonPath
     *            json path query
     * @param length
     *            expected length (-1 to not control the size)
     */
    void checkJsonPathIsArray(String jsonPath, int length) {
        Object pathValue = getJsonPath(jsonPath);
        assertThat(pathValue).isInstanceOf(Collection.class);
        if (length != -1) {
            assertThat(((Collection) pathValue)).hasSize(length);
        }
    }

    /**
     * Store a given header in the scenario scope using the given alias
     *
     * @param headerName
     *            header to save
     * @param headerAlias
     *            new header name in the scenario scope
     */
    void storeHeader(String headerName, String headerAlias) {

        assertThat(headerName).isNotEmpty();

        assertThat(headerAlias).isNotEmpty();

        String headerValue = checkHeaderExists(headerName, false);
        assertThat(headerValue).isNotEmpty();

        scenarioScope.getHeaders().put(headerAlias, headerValue);
    }

    /**
     * Store a json path value using the given alias
     *
     * @param jsonPath
     *            json path query
     * @param jsonPathAlias
     *            new json path alias in the scenario scope
     */
    void storeJsonPath(String jsonPath, String jsonPathAlias) {
        assertThat(jsonPath).isNotEmpty();

        assertThat(jsonPathAlias).isNotEmpty();

        Object pathValue = getJsonPath(jsonPath);
        scenarioScope.getJsonPaths().put(jsonPathAlias, pathValue);
    }

    /**
     * Test a scenario variable existence
     *
     * @param property
     *            name of the variable
     * @param value
     *            expected value
     */
    void checkScenarioVariable(String property, String value) {
        if (!CollectionUtils.isEmpty(scenarioScope.getJsonPaths())) {
            Assert.assertEquals(scenarioScope.getJsonPaths().get(property), value);
        }
    }

    /**
     * Parse the message json body
     *
     * @return ReadContext instance
     */
    private ReadContext readPayload() {
        ReadContext ctx = JsonPath.parse(String.valueOf(message.getPayload()));
        assertThat(ctx).isNotNull();

        return ctx;
    }

    /**
     * Get values for a given json path qby querying the message body
     *
     * @param jsonPath json path query
     * @return json path value
     */
    protected Object getJsonPath(String jsonPath) {

        assertThat(jsonPath).isNotEmpty();

        ReadContext ctx = readPayload();

        if (ctx == null) {
            return null;
        }

        Object pathValue = ctx.read(jsonPath);

        assertThat(pathValue).isNotNull();

        return pathValue;
    }

    protected String replaceDynamicParameters(String value) {
        Pattern pattern = Pattern.compile("`\\${1}(.*?)`");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            Object scopeValue = scenarioScope.getJsonPaths().get(matcher.group(1));
            if (scopeValue != null) {
                return replaceDynamicParameters(value.replace("`$"+ matcher.group(1) +"`", scopeValue.toString()));
            }
        }
        return value;
    }

    private MessageChannel getChannelByName(String channelName) {
        assertThat(channelName).isNotNull();
        return this.channels.stream()
                .filter(directChannel -> channelName.equals(directChannel.toString()))
                .findFirst().orElse(null);
    }
}

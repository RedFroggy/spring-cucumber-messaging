package fr.redfroggy.bdd.messaging.glue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import fr.redfroggy.bdd.messaging.scope.ScenarioScope;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final Map<String, Object> headers;
    private final MessageCollector collector;
    private final List<MessageChannel> channels;

    private final Configuration jsonPathConfiguration;
    private final ObjectMapper objectMapper;

    private static final ScenarioScope scenarioScope = new ScenarioScope();

    AbstractBddStepDefinition(MessageCollector collector, List<MessageChannel> channels) {
        this.collector = collector;
        this.channels = channels;

        headers = new HashMap<>();

        JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
        jsonProvider.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        jsonPathConfiguration = Configuration.builder().jsonProvider(jsonProvider).build();

        objectMapper = jsonProvider.getObjectMapper();
    }

    void setHeader(String name, String value) {
        assertThat(name).isNotNull();
        assertThat(value).isNotNull();
        value = replaceDynamicParameters(value, false);
        headers.put(name.trim(), value.trim());
    }

    /**
     * Set the queue message {@link #body}
     */
    void setBody(String body) {
        assertThat(body).isNotEmpty();

        String sanitizedBody =  replaceDynamicParameters(body, true);

        // verify json is valid
        assertThat(JsonPath.parse(sanitizedBody)).isNotNull();

        this.body = sanitizedBody;
    }

    /**
     * Edit the queue message body using a jsonPath and replace by the new value
     */
    void setBodyPathWithValue(String jsonPath, String value) {
        assertThat(jsonPath).isNotEmpty();
        assertThat(body).isNotNull();

        body = JsonPath.parse(body).set(jsonPath, value).jsonString();

    }
  
    void setBodyWithFile(String filePath) throws IOException {
        this.setBody(StreamUtils
                .copyToString(getClass().getClassLoader()
                        .getResourceAsStream(filePath), StandardCharsets.UTF_8));
    }

    void pushToQueue(String channelName) {
        MessageChannel channel = getChannelByName(channelName);
        assertThat(channel).isNotNull();

        channel.send(new GenericMessage<>(body, headers));
    }

    void readMessageFromQueue(String channelName, MessageChannelAction action) {
        MessageChannel channel = getChannelByName(channelName);
        assertThat(channel).isNotNull();

        messages = collector.forChannel(channel);
        assertThat(messages).isNotNull();

        if(!messages.isEmpty()) {
            if (action == MessageChannelAction.POLL) {
                message = messages.poll();
            } else {
                message = messages.element();
            }
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

        Object header = message.getHeaders().get(headerName.trim());
        assertThat(header).isNotNull();

        if (!isNot) {
            assertThat(String.valueOf(header)).contains(headerValue.trim());
        } else {
            assertThat(String.valueOf(header)).doesNotContain(headerValue.trim());
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
    Object checkJsonPathExists(String jsonPath) throws JsonProcessingException {
        return getJsonPath(jsonPath);
    }

    void checkJsonPathDoesntExist(String jsonPath) throws JsonProcessingException {
        ReadContext ctx = readPayload();

        assertThat(jsonPath).isNotEmpty();
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
    void checkJsonPath(String jsonPath, String jsonValueString, boolean isNot) throws JsonProcessingException {
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

        if (!isNot) {
            assertThat(pathValue).isEqualTo(JsonPath.parse(jsonValue).json());
        } else {
            assertThat(pathValue).isNotEqualTo(JsonPath.parse(jsonValue).json());
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
    void checkJsonPathIsArray(String jsonPath, int length) throws JsonProcessingException {
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
    void storeJsonPath(String jsonPath, String jsonPathAlias) throws JsonProcessingException {
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
        Object scopeValue;
        scopeValue = scenarioScope.getJsonPaths().get(property);

        if (scopeValue == null) {
            scopeValue = scenarioScope.getHeaders().get(property);
        }

        assertThat(scopeValue).isNotNull();
        assertThat(scopeValue).isEqualTo(value);
    }

    /**
     * Parse the message json body
     *
     * @return ReadContext instance
     */
    private ReadContext readPayload() throws JsonProcessingException {
        ReadContext ctx;
        Object payload = message.getPayload();
        if (payload instanceof String) {
            ctx = JsonPath.parse(String.valueOf(message.getPayload()), jsonPathConfiguration);
        } else {
            ctx = JsonPath.parse(objectMapper.writeValueAsString(message.getPayload()), jsonPathConfiguration);
        }
        assertThat(ctx).isNotNull();

        return ctx;
    }

    /**
     * Get values for a given json path qby querying the message body
     *
     * @param jsonPath json path query
     * @return json path value
     */
    protected Object getJsonPath(String jsonPath) throws JsonProcessingException {

        assertThat(jsonPath).isNotEmpty();

        ReadContext ctx = readPayload();

        Object pathValue = ctx.read(jsonPath);

        assertThat(pathValue).isNotNull();

        return pathValue;
    }

    protected String replaceDynamicParameters(String value, boolean jsonPath) {
        Pattern pattern = Pattern.compile("`\\${1}(.*?)`");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            Object scopeValue = jsonPath ? scenarioScope.getJsonPaths().get(matcher.group(1))
                    : scenarioScope.getHeaders().get(matcher.group(1));
            assertThat(scopeValue).isNotNull();

            return replaceDynamicParameters(value.replace("`$"+ matcher.group(1) +"`",
                    scopeValue.toString()), jsonPath);
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

package fr.redfroggy.bdd.messaging.glue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.redfroggy.bdd.messaging.user.UserDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

@RunWith(MockitoJUnitRunner.class)
public class AbstractBddStepDefinitionTest {

    private  Message<?> message;
    private DefaultStepDefinition stepDefinition;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {

        message = Mockito.mock(Message.class);
        BlockingQueue<Message<?>> messages = new LinkedBlockingDeque<>();
        messages.add(message);

        MessageChannel channel = Mockito.mock(MessageChannel.class);
        Mockito.when(channel.toString()).thenReturn("channelName");

        MessageCollector collector = Mockito.mock(MessageCollector.class);
        Mockito.when(collector.forChannel(channel)).thenReturn(messages);

        stepDefinition = new DefaultStepDefinition(collector, Collections.singletonList(channel));
    }

    @Test
    public void shouldReadObjectPayload() throws JsonProcessingException {

        stepDefinition.readMessageFromQueue("channelName", MessageChannelAction.POLL);

        UserDTO userDTO = new UserDTO();
        userDTO.setId("1");
        Mockito.when(message.getPayload()).thenReturn(userDTO);

        Assert.assertEquals(stepDefinition.getJsonPath("$.id"), "1");
    }

    @Test
    public void shouldReadJsonPayload() throws JsonProcessingException {

        stepDefinition.readMessageFromQueue("channelName", MessageChannelAction.POLL);

        UserDTO userDTO = new UserDTO();
        userDTO.setId("1");
        Mockito.when(message.getPayload()).thenReturn(objectMapper.writeValueAsString(userDTO));

        Assert.assertEquals(stepDefinition.getJsonPath("$.id"), "1");
    }

    private static class DefaultStepDefinition extends AbstractBddStepDefinition {

        DefaultStepDefinition(MessageCollector collector, List<MessageChannel> channels) {
            super(collector, channels);
        }
    }
}

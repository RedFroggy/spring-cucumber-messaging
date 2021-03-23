package fr.redfroggy.bdd.messaging.user;

import org.junit.Assert;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.support.GenericMessage;

import java.util.Map;

@EnableBinding(UserStream.Process.class)
public class UserStream {

    private final UserStream.Process process;
    private final UserDetailService userDetailService;

    public UserStream(UserStream.Process process, UserDetailService userDetailService) {
        this.process = process;
        this.userDetailService = userDetailService;
    }

    @StreamListener(Process.INPUT_VALID_USER)
    public void validUser(UserDTO user, @Headers Map<String, Object> headers) {
        Assert.assertNotNull(user);
        boolean valid = user.getId() != null && user.getLastName() != null
                && user.getFirstName() != null && user.getAge() > 0;
        user.setStatus(valid ? "VALID" : "INVALID");

        ResponseEntity<UserDetailsDTO> responseUserDetails = userDetailService.getUserDetails(user.getId(), user.getSessionIds());
        Assert.assertNotNull(responseUserDetails);
        if (!responseUserDetails.getStatusCode().is2xxSuccessful()) {
            throw new IllegalArgumentException("Invalid user details");
        }
        user.setDetails(responseUserDetails.getBody());

        // send result to output queue
        this.process.validUser().send(new GenericMessage<>(user, headers));
    }

    public interface Process {
        String INPUT_VALID_USER = "input-valid-user";
        String OUTPUT_VALID_USER = "output-valid-user";

        @Input(INPUT_VALID_USER)
        MessageChannel getUser();

        @Output(OUTPUT_VALID_USER)
        MessageChannel validUser();
    }
}

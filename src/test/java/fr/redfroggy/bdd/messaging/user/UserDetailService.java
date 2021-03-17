package fr.redfroggy.bdd.messaging.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import wiremock.org.apache.commons.lang3.StringUtils;

import java.util.List;

@Service
public class UserDetailService {

    @Value("${marvel.api.host}")
    private String apiHost;

    private final TestRestTemplate restTemplate;

    public UserDetailService(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<UserDetailsDTO> getUserDetails(String id, List<String> sessionIds) {
        String url = !CollectionUtils.isEmpty(sessionIds) ? apiHost + "/public/characters/" + id + "?sessionId="
                + sessionIds.get(0) : apiHost + "/public/characters/" + id;
        return this.restTemplate.getForEntity(url, UserDetailsDTO.class);
    }

}

package fr.redfroggy.bdd.messaging.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class UserDTO extends PartialUserDTO {

    private String id;

    private String firstName;

    private int age;

    private UserDTO relatedTo;

    private String status;

    private List<String> sessionIds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public UserDTO getRelatedTo() {
        return relatedTo;
    }

    public void setRelatedTo(UserDTO relatedTo) {
        this.relatedTo = relatedTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSessionIds() {
        return sessionIds;
    }

    public void setSessionIds(List<String> sessionIds) {
        this.sessionIds = sessionIds;
    }
}

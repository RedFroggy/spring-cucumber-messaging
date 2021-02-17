@user @messaging
Feature: Users messaging tests

  Scenario: Should valid user
    Given I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50"}
    Given I PUSH to queue input-valid-user
    And I POLL first message from queue output-valid-user
    And message body path $.status should be VALID
    And message body path $.firstName should be Bruce
    And message body path $.lastName should be Wayne
    And message body path $.age should be 50

@user @messaging
Feature: Users messaging tests

  Background:
    And I set contentType queue message header to application/json

  Scenario: Should valid user
    Given I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50", "sessionIds": ["43233333", "45654345"]}
    And I set X_TOKEN_ID queue message header to 1234
    When I PUSH to queue input-valid-user
    And I POLL first message from queue output-valid-user
    Then queue message body should be valid json
    And queue message body should contain Wayne
    And queue message body path $.id should exist
    And queue message body path $.relatedTo should not exist
    And queue message body path $.status should not be INVALID
    And queue message body path $.status should be VALID
    And queue message body path $.firstName should be Bruce
    And queue message body path $.lastName should be Wayne
    And queue message body path $.age should be 50
    And queue message body is typed as array for path $.sessionIds
    And queue message body is typed as array using path $.sessionIds with length 2
    And queue message body path $.sessionIds should be ["43233333", "45654345"]
    And queue message header contentType should not be application/xml
    And queue message header X_TOKEN_ID should exist
    And queue message header X_TOKEN_ID should be 1234
    And I store the value of queue message header X_TOKEN_ID as tokenId in scenario scope
    And I store the value of queue message path $.sessionIds.[0] as firstSessionId in scenario scope

  Scenario: Should not be able to read multiple times from queue when polling
    Given I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50"}
    And queue value of scenario variable tokenId should be 1234
    And I set  X_TOKEN_ID queue message header to `$tokenId`
    When I PUSH to queue input-valid-user
    And I POLL first message from queue output-valid-user
    Then queue message body path $.status should be VALID
    And queue message header X_TOKEN_ID should be 1234
    When I POLL first message from queue output-valid-user
    And Queue should have 0 messages left

  Scenario: Should be able to read multiple times from queue when peeking
    Given I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50"}
    When I PUSH to queue input-valid-user
    And I PEEK first message from queue output-valid-user
    Then queue message body path $.status should be VALID
    When I PEEK first message from queue output-valid-user
    And Queue should have 1 messages left
    Then queue message body path $.status should be VALID
    And queue message header X_TOKEN_ID should not exist

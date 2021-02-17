@user @messaging
Feature: Users messaging tests

  Background:
    And I set contentType header to application/json

  Scenario: Should valid user
    Given I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50", "sessionIds": ["43233333", "45654345"]}
    And I set X_TOKEN_ID header to 1234
    When I PUSH to queue input-valid-user
    And I POLL first message from queue output-valid-user
    Then message body should be valid json
    And message body should contain Wayne
    And message body path $.id should exist
    And message body path $.relatedTo should not exist
    And message body path $.status should not be INVALID
    And message body path $.status should be VALID
    And message body path $.firstName should be Bruce
    And message body path $.lastName should be Wayne
    And message body path $.age should be 50
    And message body is typed as array for path $.sessionIds
    And message body is typed as array using path $.sessionIds with length 2
    And message header contentType should not be application/xml
    And message header X_TOKEN_ID should exist
    And message header X_TOKEN_ID should be 1234
    And I store the value of message header X_TOKEN_ID as tokenId in scenario scope
    And I store the value of message path $.sessionIds.[0] as firstSessionId in scenario scope

  Scenario: Should not be able to read multiple times from queue when polling
    Given I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50"}
    And value of scenario variable tokenId should be 1234
    And I set  X_TOKEN_ID header to `$tokenId`
    When I PUSH to queue input-valid-user
    And I POLL first message from queue output-valid-user
    Then message body path $.status should be VALID
    And message header X_TOKEN_ID should be 1234
    When I POLL first message from queue output-valid-user
    And Queue should have 0 messages left

  Scenario: Should be able to read multiple times from queue when peeking
    Given I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50"}
    When I PUSH to queue input-valid-user
    And I PEEK first message from queue output-valid-user
    Then message body path $.status should be VALID
    When I PEEK first message from queue output-valid-user
    And Queue should have 1 messages left
    Then message body path $.status should be VALID
    And message header X_TOKEN_ID should not exist

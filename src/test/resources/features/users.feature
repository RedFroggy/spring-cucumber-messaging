@user @messaging
Feature: Users messaging tests

  Background:
    Given I set contentType queue message header to application/json

  Scenario: Should valid user
    When I mock third party call GET /public/characters/2?sessionId=43233333 with return code 200, content type: application/json and body: {"comicName": "IronMan", "city": "New York", "mainColor": ["red", "yellow"]}
    And I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50", "sessionIds": ["43233333", "45654345"]}
    And I set queue message body path $.age to 51
    And I set X_TOKEN_ID queue message header to 1234
    And I PUSH to queue input-valid-user
    And I POLL first message from queue output-valid-user
    Then queue message body should be valid json
    And queue message body should contain Wayne
    And queue message body path $.id should exist
    And queue message body path $.relatedTo should not exist
    And queue message body path $.status should not be INVALID
    And queue message body path $.status should be VALID
    And queue message body path $.firstName should be Bruce
    And queue message body path $.lastName should be Wayne
    And queue message body path $.age should be 51
    And queue message body is typed as array for path $.sessionIds
    And queue message body is typed as array using path $.sessionIds with length 2
    And queue message body path $.sessionIds should be ["43233333", "45654345"]
    And queue message body path $.sessionIds should not be []
    And queue message header contentType should not be application/xml
    And queue message header X_TOKEN_ID should exist
    And queue message header X_TOKEN_ID should be 1234
    And I store the value of queue message header X_TOKEN_ID as tokenId in scenario scope
    And I store the value of queue message path $.sessionIds.[0] as firstSessionId in scenario scope

  Scenario: Should valid user using json file as body
    Given I mock third party call GET /public/characters/15948349393 with return code 200, content type: application/json and file: fixtures/bruce_wayne_marvel_api.fixture.json
    When I set queue message body with file fixtures/bruce-banner.user.json
    And I PUSH to queue input-valid-user
    And I POLL first message from queue output-valid-user
    Then queue message body should be valid json
    And queue message body path $.firstName should be Bruce
    And queue message body path $.lastName should be Banner

  Scenario: Should not be able to read multiple times from queue when polling
    When I mock third party call GET /public/characters/2 with return code 200, content type: application/json and body: {"comicName": "IronMan", "city": "New York", "mainColor": ["red", "yellow"]}
    And I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50", "sessionIds": [`$firstSessionId`]}
    And queue value of scenario variable tokenId should be 1234
    And I set  X_TOKEN_ID queue message header to `$tokenId`
    And I PUSH to queue input-valid-user
    And I POLL first message from queue output-valid-user
    Then queue message body path $.status should be VALID
    And queue message header X_TOKEN_ID should be 1234
    And queue value of scenario variable firstSessionId should be 43233333
    When I POLL first message from queue output-valid-user
    And queue should have 0 messages left

  Scenario: Should be able to read multiple times from queue when peeking
    When I mock third party call GET /public/characters/2 with return code 200, content type: application/json and body: {"comicName": "IronMan", "city": "New York", "mainColor": ["red", "yellow"]}
    And I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50"}
    And I PUSH to queue input-valid-user
    And I PEEK first message from queue output-valid-user
    Then queue message body path $.status should be VALID
    When I PEEK first message from queue output-valid-user
    And queue should have 1 messages left
    Then queue message body path $.status should be VALID
    And queue message header X_TOKEN_ID should not exist

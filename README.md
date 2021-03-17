# Test your messaging system with Spring, Cucumber and Gherkin !

<div align="center">
  <a name="logo" href="https://www.redfroggy.fr"><img src="assets/logo.png" alt="RedFroggy"></a>
  <h4 align="center">A RedFroggy project</h4>
</div>
<br/>
<div align="center">
  <a href="https://forthebadge.com"><img src="https://forthebadge.com/images/badges/fuck-it-ship-it.svg"/></a>
  <a href="https://forthebadge.com"><img src="https://forthebadge.com/images/badges/built-with-love.svg"/></a>
<a href="https://forthebadge.com"><img src="https://forthebadge.com/images/badges/made-with-java.svg"/></a>
</div>
<div align="center">
   <a href="https://maven-badges.herokuapp.com/maven-central/fr.redfroggy.test.bdd/ucumber-messaging"><img src="https://maven-badges.herokuapp.com/maven-central/fr.redfroggy.test.bdd/cucumber-messaging/badge.svg?style=plastic" /></a>
   <a href="https://travis-ci.com/RedFroggy/spring-cucumber-messaging"><img src="https://travis-ci.com/RedFroggy/spring-cucumber-messaging.svg?branch=master"/></a>
   <a href="https://codecov.io/gh/RedFroggy/spring-cucumber-messaging"><img src="https://codecov.io/gh/RedFroggy/spring-cucumber-messaging/branch/master/graph/badge.svg?token=XM9R6ZV9SJ"/></a>
   <a href="https://github.com/semantic-release/semantic-release"><img src="https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg"/></a>
   <a href="https://opensource.org/licenses/mit-license.php"><img src="https://badges.frapsoft.com/os/mit/mit.svg?v=103"/></a> 
</div>
<br/>
<br/>

Made with Spring, [Cucumber](https://cucumber.io/) and [Gherkin](https://cucumber.io/docs/gherkin/) !

## Stack
- Spring Boot / Spring cloud
- Cucumber / Gherkin
- Jayway JsonPath

## Installation
```xml
<dependency>
  <groupId>fr.redfroggy.test.bdd</groupId>
  <artifactId>cucumber-messaging</artifactId>
</dependency>
```
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/fr.redfroggy.test.bdd/cucumber-messaging/badge.svg)](https://maven-badges.herokuapp.com/maven-central/fr.redfroggy.test.bdd/cucumber-messaging)

Run `npm install` to add commitlint + husky

## Example

```gherkin
@user @messaging
Feature: Users messaging tests

  Background:
    Given I set contentType queue message header to application/json

  Scenario: Should valid user
    When I mock third party call GET /public/characters/2?sessionId=43233333 with return code 200 and body: {"comicName": "IronMan", "city": "New York", "mainColor": ["red", "yellow"]}
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
    When I mock third party call GET /public/characters/2 with return code 200 and body: {"comicName": "IronMan", "city": "New York", "mainColor": ["red", "yellow"]}
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
    When I mock third party call GET /public/characters/2 with return code 200 and body: {"comicName": "IronMan", "city": "New York", "mainColor": ["red", "yellow"]}
    And I set queue message body to {"id": "2","firstName":"Bruce","lastName":"Wayne","age":"50"}
    And I PUSH to queue input-valid-user
    And I PEEK first message from queue output-valid-user
    Then queue message body path $.status should be VALID
    When I PEEK first message from queue output-valid-user
    And queue should have 1 messages left
    Then queue message body path $.status should be VALID
    And queue message header X_TOKEN_ID should not exist

```


You can look at the [users.feature](src/test/resources/features/users.feature) file for a more detailed example.

## Share data between steps
- You can use the following step to store data from a json response body to a shared context:
```gherkin
And I store the value of queue message path $.id as idUser in scenario scope
```
- You can use the following step to store data from a response header to a shared context:
```gherkin
And I store the value of queue message header Authorization as authHeader in scenario scope
```
- The result of the JsonPath `$.id` will be stored in the `idUser` variable.
- To reuse this variable in another step, you can do:
```gherkin
When I PUSH to queue valid-user`$idUser`
And I set X-USER-ID queue message header to `$idUser`
```


## How to use it in my existing project ?

You can see a usage example in the [test folder](src/test/java/fr/redfroggy/bdd/messaging).

### Add a CucumberTest  file

```java
@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {"pretty"},
        features = "src/test/resources/features",
        glue = {"fr.redfroggy.bdd.messaging.glue"})
public class CucumberTest {

}
````
- Set the glue property to  `fr.redfroggy.bdd.messaging.glue` and add your package glue.
- Set your `features` folder property
- Add your `.feature` files under your `features` folder
- In your `.feature` files you should have access to all the steps defined in the [MessagingBddStepDefinition](src/main/java/fr/redfroggy/bdd/messaging/glue/MessagingBddStepDefinition.java) file.


### Add default step definition file
It is mandatory to have a class annotated with `@CucumberContextConfiguration` to be able to run the tests.
This class must be in the same `glue` package that you've specified in the `CucumberTest` class.

```java
@CucumberContextConfiguration
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DefaultStepDefinition {

}
````

## Mock third party call
If you need to mock a third party API, you can use the following steps:

```gherkin
I mock third party call (.*) (.*) with return code (.*) and body: (.*)
  # Example: I mock third party call GET /public/characters/1?format=json with return code 200 and body: {"comicName": "IronMan", "city": "New York", "mainColor": ["red", "yellow"]}
I mock third party call (.*) (.*) with return code (.*), content type: (.*) and file: (.*)
  # Example: I mock third party call GET /public/characters/2 with return code 200, content type: application/json and file: fixtures/bruce_wayne_marvel_api.fixture.json
```

## Run local unit tests

````bash
$ mvn test
````

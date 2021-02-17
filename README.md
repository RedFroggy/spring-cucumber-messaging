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
  <a href="https://travis-ci.org/RedFroggy/spring-cucumber-messaging"><img src="https://travis-ci.org/RedFroggy/spring-cucumber-messaging.svg?branch=master"/></a>
   <a href="https://codecov.io/gh/RedFroggy/spring-cucumber-messaging"><img src="https://codecov.io/gh/RedFroggy/spring-cucumber-messaging/branch/master/graph/badge.svg?token=XM9R6ZV9SJ"/></a>
  <a href="https://github.com/semantic-release/semantic-release"><img src="https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg"/></a>
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
    <version>1.0.0</version>
</dependency>
```
Run `npm install` to add commitlint + husky

## Demo & Example

![Spring Cucumber Gherkin Demo](assets/demo.gif)

You can look at the [users.feature](src/test/resources/features/users.feature) file for a more detailed example.

## Share data between steps
- You can use the following step to store data from a json response body to a shared context:
```gherkin
And I store the value of body path $.id as idUser in scenario scope
```
- You can use the following step to store data from a response header to a shared context:
```gherkin
And I store the value of response header Authorization as authHeader in scenario scope
```
- The result of the JsonPath `$.id` will be stored in the `idUser` variable.
- To reuse this variable in another step, you can do:
```gherkin
When I DELETE /users/`$idUser`
And I set Authorization header to `$authHeader`
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
- Set the glue property to  `fr.redfroggy.bdd.glue` and add your package glue.
- Set your `features` folder property
- Add your `.feature` files under your `features` folder
- In your `.feature` files you should have access to all the steps defined in the [DefaultRestApiBddStepDefinition](src/main/java/fr/redfroggy/bdd/glue/DefaultRestApiBddStepDefinition.java) file.


### Add default step definition file
It is mandatory to have a class annotated with `@CucumberContextConfiguration` to be able to run the tests.
This class must be in the same `glue` package that you've specified in the `CucumberTest` class.

```java
@CucumberContextConfiguration
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DefaultStepDefinition {

}
````

## Run local unit tests

````bash
$ mvn test
````

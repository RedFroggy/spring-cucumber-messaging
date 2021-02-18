package fr.redfroggy.bdd.messaging.glue;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * This file is mandatory for cucumber tests because it needs a Cucumber context to be able to work.
 * In this file you can add your own steps implementation.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class DefaultMessagingStepDefinitionTest {
}

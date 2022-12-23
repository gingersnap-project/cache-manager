package io.gingersnapproject.rest;

import static io.restassured.RestAssured.given;

import io.gingersnapproject.mysql.MySQLResources;
import io.gingersnapproject.profile.GingersnapIntegrationTest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
@GingersnapIntegrationTest
@QuarkusTestResource(MySQLResources.class)
public class RuleResourcesTest {

   @Test
   public void testRetrieveRuleKeys() {
      given()
            .when().get("/rules/" + MySQLResources.RULE)
            .then()
            .statusCode(200)
            .body("", Matchers.emptyCollectionOf(String.class));
   }
}

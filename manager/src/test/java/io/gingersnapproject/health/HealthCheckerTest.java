package io.gingersnapproject.health;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.gingersnapproject.mysql.MySQLResources;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(MySQLResources.class)
public class HealthCheckerTest {
   @Test
   public void testHealthEndpointDefined() {
      given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200);
   }
}

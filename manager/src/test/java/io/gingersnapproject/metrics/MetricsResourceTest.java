package io.gingersnapproject.metrics;

import io.gingersnapproject.metrics.micrometer.TimerMetric;
import io.gingersnapproject.mysql.MySQLResources;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
@QuarkusTestResource(MySQLResources.class)
public class MetricsResourceTest {

    private static final String GET_PATH = "/rules/{rule}/{key}";
    private static final String METRIC_FORMAT = "%s_seconds_count{gingersnap=\"cache_manager\",} %s";

    @Test
    public void testMetricsEndpoint() {
        EnumMap<TimerMetric, String> expectedCount = new EnumMap<>(TimerMetric.class);
        Arrays.stream(TimerMetric.values()).forEach(metric -> expectedCount.put(metric, "0.0"));

        // cache remote hit!
        given()
                .when().get(GET_PATH, "us-east", "1")
                .then().body(containsString("Jon Doe"));
        expectedCount.put(TimerMetric.CACHE_REMOTE_HIT, "1.0");
        assertMetricsValue(expectedCount);

        // cache local hit
        given()
                .when().get(GET_PATH, "us-east", "1")
                .then().body(containsString("Jon Doe"));
        expectedCount.put(TimerMetric.CACHE_LOCAL_HIT, "1.0");
        assertMetricsValue(expectedCount);

        // cache remote miss
        given()
                .when().get(GET_PATH, "us-east", "100000")
                .then().statusCode(HttpStatus.SC_NO_CONTENT);
        expectedCount.put(TimerMetric.CACHE_REMOTE_MISS, "1.0");
        assertMetricsValue(expectedCount);

        // cache local miss
        given()
                .when().get(GET_PATH, "us-east", "100000")
                .then().statusCode(HttpStatus.SC_NO_CONTENT);
        expectedCount.put(TimerMetric.CACHE_LOCAL_MISS, "1.0");
        assertMetricsValue(expectedCount);

        // cache error
        given()
                .when().get(GET_PATH, "non-existing-rule", "100000")
                .then().statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        expectedCount.put(TimerMetric.CACHE_ERROR, "1.0");
        assertMetricsValue(expectedCount);
    }

    private void assertMetricsValue(EnumMap<TimerMetric, String> expectedCount) {
        var matcherList = expectedCount.entrySet().stream()
                .map(MetricsResourceTest::convertToContainsString)
                .toList();
        given()
                .get("/q/metrics")
                .then()
                .body(matcherList.get(0), matcherList.subList(1, matcherList.size()).toArray(Matcher[]::new));
    }

    private static Matcher<String> convertToContainsString(Map.Entry<TimerMetric, String> entry) {
        return containsString(format(METRIC_FORMAT, entry.getKey().metricName(), entry.getValue()));
    }

}

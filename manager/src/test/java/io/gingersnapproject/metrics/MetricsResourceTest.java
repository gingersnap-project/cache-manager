package io.gingersnapproject.metrics;

import io.gingersnapproject.metrics.micrometer.PerRuleGaugeMetric;
import io.gingersnapproject.metrics.micrometer.TimerMetric;
import io.gingersnapproject.mysql.MySQLResources;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
@QuarkusTestResource(MySQLResources.class)
public class MetricsResourceTest {

   private static final String GET_PATH = "/rules/{rule}/{key}";
   private static final String TIMER_METRIC_FORMAT = "%s_seconds_count{gingersnap=\"cache_manager\",} %s";
   private static final String GAUGE_METRIC_FORMAT = "%s{gingersnap=\"cache_manager\",} %s";

   @Test
   public void testMetricsEndpoint() {
      EnumMap<TimerMetric, String> expectedCount = new EnumMap<>(TimerMetric.class);
      Arrays.stream(TimerMetric.values()).forEach(metric -> expectedCount.put(metric, "0.0"));
      EnumMap<PerRuleGaugeMetric, String> expectedGauge = new EnumMap<>(PerRuleGaugeMetric.class);
      Arrays.stream(PerRuleGaugeMetric.values()).forEach(metric -> expectedGauge.put(metric, "0.0"));

      // cache remote hit!
      given().when().get(GET_PATH, MySQLResources.RULE, "1").then().body(containsString("Jon Doe"));
      expectedCount.put(TimerMetric.CACHE_REMOTE_HIT, "1.0");
      expectedGauge.put(PerRuleGaugeMetric.CACHE_SIZE, "1.0");
      assertTimerMetricsValue(expectedCount);
      assertGaugeMetricsValue(expectedGauge, MySQLResources.RULE);

      // cache local hit
      given().when().get(GET_PATH, MySQLResources.RULE, "1").then().body(containsString("Jon Doe"));
      expectedCount.put(TimerMetric.CACHE_LOCAL_HIT, "1.0");
      assertTimerMetricsValue(expectedCount);
      assertGaugeMetricsValue(expectedGauge, MySQLResources.RULE);

      // cache remote miss
      given().when().get(GET_PATH, MySQLResources.RULE, "100000").then().statusCode(HttpStatus.SC_NO_CONTENT);
      expectedCount.put(TimerMetric.CACHE_REMOTE_MISS, "1.0");
      expectedGauge.put(PerRuleGaugeMetric.CACHE_SIZE, "2.0");
      assertTimerMetricsValue(expectedCount);
      assertGaugeMetricsValue(expectedGauge, MySQLResources.RULE);

      // cache local miss
      given().when().get(GET_PATH, MySQLResources.RULE, "100000").then().statusCode(HttpStatus.SC_NO_CONTENT);
      expectedCount.put(TimerMetric.CACHE_LOCAL_MISS, "1.0");
      assertTimerMetricsValue(expectedCount);
      assertGaugeMetricsValue(expectedGauge, MySQLResources.RULE);

      // rule is never created, metrics remains the same
      given().when().get(GET_PATH, "non-existing-rule", "100000").then().statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      assertTimerMetricsValue(expectedCount);
      assertGaugeMetricsValue(expectedGauge, MySQLResources.RULE);
   }

   private void assertTimerMetricsValue(EnumMap<TimerMetric, String> expectedCount) {
      var matcherList = expectedCount.entrySet().stream().map(MetricsResourceTest::convertToContainsString).toList();
      assertMetricsMatchers(matcherList);
   }

   private void assertGaugeMetricsValue(EnumMap<PerRuleGaugeMetric, String> expectedCount, String rule) {
      var matcherList = expectedCount.entrySet().stream().map(entry -> convertToContainsString(entry, rule)).toList();
      assertMetricsMatchers(matcherList);
   }

   private static void assertMetricsMatchers(List<Matcher<String>> matchers) {
      var response = given().get("/q/metrics").then();
      if (matchers.size() == 1) {
         response.body(matchers.get(0));
      } else {
         response.body(matchers.get(0), matchers.subList(1, matchers.size()).toArray(Matcher[]::new));
      }
   }

   private static Matcher<String> convertToContainsString(Map.Entry<TimerMetric, String> entry) {
      return containsString(format(TIMER_METRIC_FORMAT, entry.getKey().metricName(MySQLResources.RULE).replace('-', '_'), entry.getValue()));
   }

   private static Matcher<String> convertToContainsString(Map.Entry<PerRuleGaugeMetric, String> entry, String rule) {
      return containsString(format(GAUGE_METRIC_FORMAT, entry.getKey().metricName(rule).replace('-', '_'), entry.getValue()));
   }
}

package io.gingersnapproject.metrics;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import io.gingersnapproject.metrics.micrometer.PerRuleGaugeMetric;
import io.gingersnapproject.metrics.micrometer.PerRuleTimerMetric;
import io.gingersnapproject.mysql.MySQLResources;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.prometheus.PrometheusNamingConvention;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static io.gingersnapproject.metrics.micrometer.CacheManagerMicrometerMetrics.COMPONENT_KEY;
import static io.gingersnapproject.metrics.micrometer.CacheManagerMicrometerMetrics.COMPONENT_NAME;
import static io.gingersnapproject.metrics.micrometer.CacheManagerMicrometerMetrics.RULE_KEY;
import static io.gingersnapproject.database.DatabaseResourcesLifecyleManager.RULE_NAME;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
@QuarkusTestResource(DatabaseResourcesLifecyleManager.class)
public class MetricsResourceTest {

   private static final String GET_PATH = "/rules/{rule}/{key}";
   // prometheus naming for testing!
   private static final NamingConvention NAMING_CONVENTION = new PrometheusNamingConvention();

   @Test
   public void testMetricsEndpoint() {
      EnumMap<PerRuleTimerMetric, String> expectedCount = new EnumMap<>(PerRuleTimerMetric.class);
      Arrays.stream(PerRuleTimerMetric.values()).forEach(metric -> expectedCount.put(metric, "0.0"));
      EnumMap<PerRuleGaugeMetric, String> expectedGauge = new EnumMap<>(PerRuleGaugeMetric.class);
      Arrays.stream(PerRuleGaugeMetric.values()).forEach(metric -> expectedGauge.put(metric, "0.0"));

      // cache remote hit!
      given().when().get(GET_PATH, RULE_NAME, "1").then().body(containsString("Jon Doe"));
      expectedCount.put(PerRuleTimerMetric.CACHE_REMOTE_HIT, "1.0");
      expectedGauge.put(PerRuleGaugeMetric.CACHE_SIZE, "1.0");
      assertTimerMetricsValue(expectedCount);
      assertGaugeMetricsValue(expectedGauge);

      // cache local hit
      given().when().get(GET_PATH, RULE_NAME, "1").then().body(containsString("Jon Doe"));
      expectedCount.put(PerRuleTimerMetric.CACHE_LOCAL_HIT, "1.0");
      assertTimerMetricsValue(expectedCount);
      assertGaugeMetricsValue(expectedGauge);

      // cache remote miss
      given().when().get(GET_PATH, RULE_NAME, "100000").then().statusCode(HttpStatus.SC_NO_CONTENT);
      expectedCount.put(PerRuleTimerMetric.CACHE_REMOTE_MISS, "1.0");
      expectedGauge.put(PerRuleGaugeMetric.CACHE_SIZE, "2.0");
      assertTimerMetricsValue(expectedCount);
      assertGaugeMetricsValue(expectedGauge);

      // cache local miss
      given().when().get(GET_PATH, RULE_NAME, "100000").then().statusCode(HttpStatus.SC_NO_CONTENT);
      expectedCount.put(PerRuleTimerMetric.CACHE_LOCAL_MISS, "1.0");
      assertTimerMetricsValue(expectedCount);
      assertGaugeMetricsValue(expectedGauge);

      // rule is never created, metrics remains the same
      given().when().get(GET_PATH, "non-existing-rule", "100000").then().statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      assertTimerMetricsValue(expectedCount);
      assertGaugeMetricsValue(expectedGauge);
   }

   private void assertTimerMetricsValue(EnumMap<PerRuleTimerMetric, String> expectedCount) {
      var matcherList = expectedCount.entrySet().stream().map(MetricsResourceTest::convertTimerToContainsString).toList();
      assertMetricsMatchers(matcherList);
   }

   private void assertGaugeMetricsValue(EnumMap<PerRuleGaugeMetric, String> expectedCount) {
      var matcherList = expectedCount.entrySet().stream().map(MetricsResourceTest::convertGaugeToContainsString).toList();
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

   private static Matcher<String> convertTimerToContainsString(Map.Entry<PerRuleTimerMetric, String> entry) {
      return containsString(timerMetricName(entry.getKey(), entry.getValue()));
   }

   private static Matcher<String> convertGaugeToContainsString(Map.Entry<PerRuleGaugeMetric, String> entry) {
      return containsString(gaugeMetricName(entry.getKey(), entry.getValue()));
   }

   private static String timerMetricName(PerRuleTimerMetric metric, String value) {
      return metricName(metric.metricName(), "_count", value, Meter.Type.TIMER);
   }

   private static String gaugeMetricName(PerRuleGaugeMetric metric, String value) {
     return metricName(metric.metricName(), null, value, Meter.Type.GAUGE);
   }

   private static String metricName(String metricName, String suffix, String value, Meter.Type type) {
      String name = NAMING_CONVENTION.name(metricName, type);
      if (suffix != null) {
         name += suffix;
      }
      // tags
      name += "{%s=\"%s\",%s=\"%s\",}".formatted(
            NAMING_CONVENTION.tagKey(COMPONENT_KEY),
            NAMING_CONVENTION.tagValue(COMPONENT_NAME),
            NAMING_CONVENTION.tagKey(RULE_KEY),
            NAMING_CONVENTION.tagValue(RULE_NAME)
      );
      // value
      return name + " " + value;
   }
}

package io.gingersnapproject.profile;

import io.quarkus.test.junit.QuarkusTestProfile;

public class IntegrationNativeProfile implements QuarkusTestProfile {
   public static final String INTEGRATION_NATIVE = "integration-native";

   @Override
   public String getConfigProfile() {
      return INTEGRATION_NATIVE;
   }
}

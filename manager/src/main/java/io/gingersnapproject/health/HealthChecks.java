package io.gingersnapproject.health;

import javax.inject.Singleton;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;

import io.gingersnapproject.HotRodServer;
import io.quarkus.arc.Arc;

public class HealthChecks {

   private static final String SERVER_CHECK_NAME = "HotRod Server";

   @Liveness
   @Singleton
   public static class LivenessCheck implements HealthCheck {
      @Override
      public HealthCheckResponse call() {
         HotRodServer server = Arc.container().instance(HotRodServer.class).get();
         return HealthCheckResponse.named(SERVER_CHECK_NAME)
               .status(server.isLive())
               .build();
      }
   }

   @Readiness
   @Singleton
   public static class ReadinessCheck implements HealthCheck {
      @Override
      public HealthCheckResponse call() {
         HotRodServer server = Arc.container().instance(HotRodServer.class).get();
         return HealthCheckResponse.named(SERVER_CHECK_NAME)
               .status(server.isReady())
               .build();
      }
   }

   @Startup
   @Singleton
   public static class StartupCheck implements HealthCheck {
      @Override
      public HealthCheckResponse call() {
         HotRodServer server = Arc.container().instance(HotRodServer.class).get();
         return HealthCheckResponse.named(SERVER_CHECK_NAME)
               .status(server.hasStarted())
               .build();
      }
   }
}

package io.gingersnapproject.configuration;

import io.smallrye.config.WithDefault;

public interface HotRod {
   @WithDefault("0.0.0.0")
   String host();

   @WithDefault("11222")
   int port();
}

package io.gingersnapproject.configuration;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "gingersnap")
public interface Configuration {
   HotRod hotrod();

   @WithName("rule")
   Map<String, Rule> rules();
}

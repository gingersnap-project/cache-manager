package io.gingersnapproject.configuration;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "gingersnap")
public interface Configuration {
   HotRod hotrod();
}

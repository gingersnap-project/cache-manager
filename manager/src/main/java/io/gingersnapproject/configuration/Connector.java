package io.gingersnapproject.configuration;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface Connector {

   String schema();

   String table();
}

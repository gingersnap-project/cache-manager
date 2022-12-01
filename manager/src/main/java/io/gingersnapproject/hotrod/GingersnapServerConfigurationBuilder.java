package io.gingersnapproject.hotrod;

import static org.infinispan.server.core.configuration.ProtocolServerConfiguration.NAME;

import org.infinispan.server.core.configuration.ProtocolServerConfigurationBuilder;

public class GingersnapServerConfigurationBuilder extends ProtocolServerConfigurationBuilder<GingersnapServerConfiguration, GingersnapServerConfigurationBuilder> {
   private static final String DEFAULT_NAME = "gingersnap";

   public GingersnapServerConfigurationBuilder(int port) {
      super(port);
   }

   @Override
   public GingersnapServerConfiguration create() {
      if (!attributes.attribute(NAME).isModified()) {
         String socketBinding = socketBinding();
         name(DEFAULT_NAME + (socketBinding == null ? "" : "-" + socketBinding));
      }
      return new GingersnapServerConfiguration(attributes.protect(), ssl.create(), ipFilter.create());
   }

   @Override
   public GingersnapServerConfiguration build() {
      return create();
   }

   @Override
   public GingersnapServerConfigurationBuilder self() {
      return this;
   }
}

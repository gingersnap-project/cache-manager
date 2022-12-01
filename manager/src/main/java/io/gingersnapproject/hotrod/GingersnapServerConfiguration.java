package io.gingersnapproject.hotrod;

import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.server.core.configuration.IpFilterConfiguration;
import org.infinispan.server.core.configuration.ProtocolServerConfiguration;
import org.infinispan.server.core.configuration.SslConfiguration;

public class GingersnapServerConfiguration extends ProtocolServerConfiguration<GingersnapServerConfiguration> {
   protected GingersnapServerConfiguration(AttributeSet attributes, SslConfiguration ssl, IpFilterConfiguration ipFilter) {
      super("GINGERSNAP", attributes, ssl, ipFilter);
   }
}

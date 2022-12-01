package io.gingersnapproject.hotrod;

import java.util.Map;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.server.hotrod.HotRodHeader;
import org.infinispan.server.hotrod.HotRodOperation;
import org.infinispan.server.hotrod.VersionedEncoder;

public class GingersnapHeader extends HotRodHeader {
   public GingersnapHeader(HotRodHeader header) {
      super(header);
   }

   public GingersnapHeader(HotRodOperation op, byte version, long messageId, String cacheName, int flag, short clientIntel, int topologyId, MediaType keyType, MediaType valueType, Map<String, byte[]> otherParams) {
      super(op, version, messageId, cacheName, flag, clientIntel, topologyId, keyType, valueType, otherParams);
   }

   @Override
   public VersionedEncoder encoder() {
      return GingersnapEncoder1.INSTANCE;
   }
}

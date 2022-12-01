package io.gingersnapproject.hotrod;

/**
 * Defines constants defined by Hot Rod specifications.
 * @since 0.1
 */
public interface HotRodConstants {
   byte VERSION_30 = HotRodVersion.HOTROD_30.getVersion();
   byte VERSION_31 = HotRodVersion.HOTROD_31.getVersion();
   byte VERSION_40 = HotRodVersion.HOTROD_40.getVersion();

   byte MAGIC_REQ = (byte) 0xA0;
   short MAGIC_RES = 0xA1;

   //requests
   byte PUT_REQUEST = 0x01;
   byte GET_REQUEST = 0x03;
   byte PUT_IF_ABSENT_REQUEST = 0x05;
   byte REMOVE_REQUEST = 11;
   byte STATS_REQUEST = 0x15;
   byte PING_REQUEST = 0x17;
   byte QUERY_REQUEST = 0x1F;
   byte SIZE_REQUEST = 0x29;
   byte EXEC_REQUEST = 0x2B;
   byte PUT_ALL_REQUEST = 0x2D;
   byte GET_ALL_REQUEST = 0x2F;
   byte ITERATION_START_REQUEST = 0x31;
   byte ITERATION_NEXT_REQUEST = 0x33;
   byte ITERATION_END_REQUEST = 0x35;

   byte GET_MULTIMAP_REQUEST = 0x67;
   byte PUT_MULTIMAP_REQUEST = 0x6B;
}

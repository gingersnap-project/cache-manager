package io.gingersnapproject.hotrod;

/**
 * Enumeration defining all of the possible hotrod operations
 *
 * @author wburns
 * @since 0.1
 */
public enum HotRodOperation {
   // Puts
   PUT(0x01, 0x02),
   PUT_IF_ABSENT(0x05, 0x06),
   // Gets
   GET(0x03, 0x04),
   // Removes
   REMOVE(0x0B, 0x0C),

   // Operation(s) that end after Header is read
   PING(0x17, 0x18),
   STATS(0x15, 0x16),
   SIZE(0x29, 0x2A),

   // Operation(s) that end after Custom Header is read
   EXEC(0x2B, 0x2C),

   // Operations that end after a Custom Key is read
   QUERY(0x1F, 0x20),
   ITERATION_START(0x31, 0x32),
   ITERATION_NEXT(0x33, 0x34),
   ITERATION_END(0x35, 0x36),

   // Operations that end after a Custom Value is read
   PUT_ALL(0x2D, 0x2E),
   GET_ALL(0x2F, 0x30),

   // Multimap operations
   GET_MULTIMAP(0x67, 0x68),
   PUT_MULTIMAP(0x6B, 0x6C),

   // Responses
   ERROR(0x50);

   private final int requestOpCode;
   private final int responseOpCode;
   private static final HotRodOperation[] REQUEST_OPCODES;
   private static final HotRodOperation[] RESPONSE_OPCODES;
   public static final int REQUEST_COUNT;
   static final HotRodOperation[] VALUES = values();

   HotRodOperation(int requestOpCode, int responseOpCode) {
      this.requestOpCode = requestOpCode;
      this.responseOpCode = responseOpCode;
   }

   HotRodOperation(int responseOpCode) {
      this(0, responseOpCode);
   }

   public int getRequestOpCode() {
      return requestOpCode;
   }

   public int getResponseOpCode() {
      return responseOpCode;
   }

   static {
      REQUEST_OPCODES = new HotRodOperation[256];
      RESPONSE_OPCODES = new HotRodOperation[256];
      int requestCount = 0;
      for(HotRodOperation op : VALUES) {
         if (op.requestOpCode > 0) {
            REQUEST_OPCODES[op.requestOpCode] = op;
            ++requestCount;
         }
         if (op.responseOpCode > 0)
            RESPONSE_OPCODES[op.responseOpCode] = op;
      }
      REQUEST_COUNT = requestCount;
   }

   public static HotRodOperation fromRequestOpCode(byte op) {
      return REQUEST_OPCODES[op & 0xff];
   }

   public static HotRodOperation fromResponseOpCode(byte op) {
      return RESPONSE_OPCODES[op & 0xff];
   }
}

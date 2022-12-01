package io.gingersnapproject.hotrod;

import static org.infinispan.server.hotrod.transport.ExtendedByteBuf.writeUnsignedLong;

import org.infinispan.server.hotrod.Constants;
import org.infinispan.server.hotrod.Encoder4x;
import org.infinispan.server.hotrod.HotRodHeader;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.HotRodVersion;
import org.infinispan.server.hotrod.OperationStatus;
import org.infinispan.server.hotrod.transport.ExtendedByteBuf;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class GingersnapEncoder1 extends Encoder4x {
   public static final GingersnapEncoder1 INSTANCE = new GingersnapEncoder1();

   private GingersnapEncoder1() {

   }
   @Override
   protected ByteBuf writeHeader(HotRodHeader header, HotRodServer server, Channel channel, OperationStatus status) {
      return writeHeader(header, channel, status, false);
   }

   @Override
   public ByteBuf pingResponse(HotRodHeader header, HotRodServer server, Channel channel, OperationStatus status) {
      ByteBuf buf = writeHeader(header, channel, status, true);
      buf.writeByte(HotRodVersion.LATEST.getVersion());
      ExtendedByteBuf.writeUnsignedInt(HotRodOperation.REQUEST_COUNT, buf);
      for (HotRodOperation op : HotRodOperation.VALUES) {
         // We only include request ops
         if (op.getRequestOpCode() > 0) {
            buf.writeShort(op.getRequestOpCode());
         }
      }
      return buf;
   }

   protected ByteBuf writeHeader(HotRodHeader header, Channel channel, OperationStatus status, boolean writeMediaType) {
      ByteBuf buf = channel.alloc().ioBuffer();

      buf.writeByte(Constants.MAGIC_RES);
      writeUnsignedLong(header.getMessageId(), buf);
      buf.writeByte(header.getOp().getResponseOpCode());

      buf.writeByte(status.getCode());

      // Always ignore topology
      buf.writeByte(0);

      if (writeMediaType) {
         buf.writeByte(0);
         buf.writeByte(0);
      }

      return buf;
   }
}

package com.io7m.callisto.tests.prototype0;

import com.google.protobuf.ByteString;
import com.io7m.callisto.prototype0.messages.PrototypeMessages;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class CoMessagesExample
{
  private CoMessagesExample()
  {

  }

  public static void main(
    final String[] args)
    throws IOException
  {
    try (final OutputStream os =
           Files.newOutputStream(Paths.get("/tmp/data.bin"))) {

      final byte[] buf = new byte[3 * 4];
      final ByteBuffer vec = ByteBuffer.wrap(buf);
      vec.putFloat(0, 23.0f);
      vec.putFloat(4, 0.2f);
      vec.putFloat(8, 1.63f);

      final PrototypeMessages.CoClientPacket pb =
        PrototypeMessages.CoClientPacket.newBuilder()
          .setClientId(0xa0a0a0a0)
          .setAck(0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L)
          .setSequence(569)
          .addMessages(
            PrototypeMessages.CoMessageData.newBuilder()
              .setSequence(0)
              .setTypeGroup(0x494F374D)
              .setTypeName(23)
              .setData(ByteString.copyFrom(buf)))
          .addMessages(
            PrototypeMessages.CoMessageData.newBuilder()
              .setSequence(1)
              .setTypeGroup(0x494F374D)
              .setTypeName(23)
              .setData(ByteString.copyFrom(buf)))
          .addMessages(
            PrototypeMessages.CoMessageData.newBuilder()
              .setSequence(2)
              .setTypeGroup(0x494F374D)
              .setTypeName(23)
              .setData(ByteString.copyFrom(buf)))
          .build();

      os.write(pb.toByteArray());
      os.flush();
    }
  }
}

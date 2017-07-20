package com.io7m.callisto.prototype0.server;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.idpool.CoIDPoolUnpredictable;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSourceType;
import com.io7m.jnull.NullCheck;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoClientData;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoClientHello;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoClientPacket;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoServerHello;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoServerHelloError;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoServerHelloOK;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoServerPacket;

public final class CoServerClientHandler implements Closeable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoServerClientHandler.class);

  private final CoNetworkPacketSourceType source;
  private final Object2IntOpenHashMap<String> client_ids_by_name;
  private final Int2ReferenceRBTreeMap<Client> client_by_id;
  private final CoIDPoolUnpredictable ids;
  private final ByteBuffer buffer;
  private final CoEventServiceType events;
  private int time;

  private static final class Client
  {
    private final String name;
    private final int id;

    public Client(
      final int in_id,
      final String in_name)
    {
      this.id = in_id;
      this.name = NullCheck.notNull(in_name, "Name");
    }
  }

  public CoServerClientHandler(
    final CoEventServiceType in_events,
    final CoNetworkPacketSourceType in_source)
  {
    this.events = NullCheck.notNull(in_events, "Events");
    this.source = NullCheck.notNull(in_source, "Source");

    this.client_ids_by_name = new Object2IntOpenHashMap<>();
    this.client_by_id = new Int2ReferenceRBTreeMap<>();
    this.ids = new CoIDPoolUnpredictable();
    this.buffer = ByteBuffer.allocateDirect(1200);
  }

  private void onReceivedPacket(
    final SocketAddress address,
    final ByteBuffer data)
  {
    NullCheck.notNull(address, "Address");
    NullCheck.notNull(data, "Data");

    LOG.trace("{}: received {} octets", address, Integer.valueOf(data.limit()));

    try {
      final CoClientPacket pack = CoClientPacket.parseFrom(data);
      switch (pack.getValueCase()) {
        case HELLO:
          this.onReceivedHello(address, pack.getHello());
          break;
        case DATA:
          this.onReceivedData(address, pack.getData());
          break;
        case VALUE_NOT_SET: {
          LOG.error("received invalid message: {}: {}", address, pack);
        }
      }
    } catch (final InvalidProtocolBufferException e) {
      LOG.error("received invalid message: {}: ", address, e);
    } catch (final IOException e) {
      LOG.error("i/o error: {}: ", address, e);
    }
  }

  private void onReceivedData(
    final SocketAddress address,
    final CoClientData data)
  {
    LOG.debug("data: {}: {}", address, data);
  }

  private void onReceivedHello(
    final SocketAddress address,
    final CoClientHello hello)
    throws IOException
  {
    LOG.debug("hello: {}: {}", address, hello);

    final String name =
      NullCheck.notNull(hello.getName(), "Name");

    if (this.client_ids_by_name.containsKey(name)) {
      this.sendMessage(address, helloError("Username is already taken."));
      return;
    }

    final int id = this.ids.fresh();
    final Client c = new Client(id, name);
    this.client_by_id.put(id, c);
    this.client_ids_by_name.put(name, id);

    LOG.info(
      "{}: client {} joined (id {})",
      address,
      name,
      Integer.toUnsignedString(id, 16));
    this.sendMessage(address, helloOK(id));
    this.events.post(CoServerNetworkEventConnected.of(id, name, address));
  }

  private static CoServerPacket helloOK(
    final int id)
  {
    final CoServerHelloOK ho =
      CoServerHelloOK.newBuilder()
        .setClientId(id)
        .build();

    final CoServerHello h =
      CoServerHello.newBuilder()
        .setOk(ho)
        .build();

    return CoServerPacket.newBuilder().setHello(h).build();
  }

  private static CoServerPacket helloError(
    final String message)
  {
    final CoServerHelloError he =
      CoServerHelloError
        .newBuilder()
        .setMessage(message)
        .build();

    final CoServerHello h =
      CoServerHello.newBuilder()
        .setError(he)
        .build();

    return CoServerPacket.newBuilder().setHello(h).build();
  }

  private void sendMessage(
    final SocketAddress address,
    final MessageLite m)
    throws IOException
  {
    this.checkMessageSize(m);

    final int size = m.getSerializedSize();
    this.buffer.position(0);
    this.buffer.limit(size);
    try (final ByteBufferOutputStream out =
           new ByteBufferOutputStream(this.buffer)) {
      m.writeTo(out);
    }

    this.buffer.position(0);
    this.buffer.limit(size);
    this.source.send(address, this.buffer);
  }

  private void checkMessageSize(
    final MessageLite hello)
  {
    final int size = hello.getSerializedSize();
    if (size > this.buffer.capacity()) {
      throw new IllegalArgumentException(
        new StringBuilder(128)
          .append("Message too large.")
          .append(System.lineSeparator())
          .append("  Maximum size: ")
          .append(this.buffer.capacity())
          .append(System.lineSeparator())
          .append("  Received size: ")
          .append(size)
          .append(System.lineSeparator())
          .toString());
    }
  }

  private static final class ByteBufferOutputStream extends OutputStream
  {
    private final ByteBuffer buffer;

    ByteBufferOutputStream(
      final ByteBuffer in_buffer)
    {
      this.buffer = NullCheck.notNull(in_buffer, "Buffer");
    }

    @Override
    public void write(
      final int b)
      throws IOException
    {
      this.buffer.put((byte) b);
    }
  }

  public void onTick()
    throws IOException
  {
    ++this.time;
    this.source.poll(this::onReceivedPacket);
  }

  @Override
  public void close()
    throws IOException
  {
    this.source.close();
  }
}

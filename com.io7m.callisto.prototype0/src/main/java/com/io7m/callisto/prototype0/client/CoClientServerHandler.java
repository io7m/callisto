package com.io7m.callisto.prototype0.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSinkType;
import com.io7m.jfsm.core.FSMEnumMutable;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoClientHello;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoClientPacket;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoServerData;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoServerHello;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoServerHelloError;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoServerHelloOK;
import static com.io7m.callisto.prototype0.messages.PrototypeMessages.CoServerPacket;

public final class CoClientServerHandler implements Closeable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoClientServerHandler.class);

  private final CoNetworkPacketSinkType sink;
  private final ByteBuffer buffer;
  private final int sequence;
  private final FSMEnumMutable<State> state;
  private final CoEventServiceType events;
  private int hello_attempts;
  private int time;
  private int client_id;
  private String user;
  private String pass;

  private enum State
  {
    STATE_INITIAL,
    STATE_WAITING_FOR_HELLO,
    STATE_CONNECTED,
    STATE_DISCONNECTED
  }

  public CoClientServerHandler(
    final CoEventServiceType in_events,
    final CoNetworkPacketSinkType in_sink)
  {
    this.events = NullCheck.notNull(in_events, "Events");
    this.sink = NullCheck.notNull(in_sink, "Sink");
    this.buffer = ByteBuffer.allocateDirect(1200);
    this.sequence = 0;
    this.client_id = 0;
    this.time = 0;
    this.hello_attempts = 0;

    this.state =
      FSMEnumMutable.builder(State.STATE_INITIAL)
        .addTransition(State.STATE_INITIAL, State.STATE_WAITING_FOR_HELLO)
        .addTransition(State.STATE_WAITING_FOR_HELLO, State.STATE_CONNECTED)
        .addTransition(State.STATE_WAITING_FOR_HELLO, State.STATE_DISCONNECTED)
        .addTransition(State.STATE_CONNECTED, State.STATE_DISCONNECTED)
        .build();
  }

  public void onStart(
    final String user,
    final String pass)
    throws IOException
  {
    NullCheck.notNull(user, "User");
    NullCheck.notNull(pass, "Pass");

    switch (this.state.current()) {
      case STATE_INITIAL: {
        this.user = user;
        this.pass = pass;

        this.sendHello(user, pass);
        this.state.transition(State.STATE_WAITING_FOR_HELLO);
        break;
      }
      case STATE_WAITING_FOR_HELLO:
      case STATE_CONNECTED:
      case STATE_DISCONNECTED: {
        this.state.transition(State.STATE_WAITING_FOR_HELLO);
      }
    }
  }

  public void onTick()
    throws IOException
  {
    ++this.time;

    switch (this.state.current()) {
      case STATE_DISCONNECTED:
        return;

      case STATE_WAITING_FOR_HELLO: {
        if (this.hello_attempts == 10) {
          this.events.post(CoClientNetworkEventConnectionTimedOut.of(
            String.format("Could not establish a connection to the server after %d attempts",
                          Integer.valueOf(this.hello_attempts))));
          this.state.transition(State.STATE_DISCONNECTED);
        }

        if (this.time % 120 == 0) {
          this.sendHello(this.user, this.pass);
        }
        break;
      }
      case STATE_INITIAL:
      case STATE_CONNECTED: {
        break;
      }
    }

    this.sink.poll(this::onReceivedMessage);
  }

  private void onReceivedMessage(
    final SocketAddress address,
    final ByteBuffer data)
  {
    LOG.debug("{}: received {} octets", address, Integer.valueOf(data.limit()));

    switch (this.state.current()) {
      case STATE_INITIAL:
      case STATE_WAITING_FOR_HELLO:
      case STATE_CONNECTED: {
        try {
          final CoServerPacket pack = CoServerPacket.parseFrom(data);
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
        break;
      }
      case STATE_DISCONNECTED: {
        throw new UnreachableCodeException();
      }
    }
  }

  private void onReceivedData(
    final SocketAddress address,
    final CoServerData data)
  {
    switch (this.state.current()) {
      case STATE_CONNECTED: {
        LOG.debug("received data packet: {}", data);
        break;
      }
      case STATE_INITIAL:
      case STATE_WAITING_FOR_HELLO:
      case STATE_DISCONNECTED: {
        LOG.error("ignored unexpected ServerData packet");
        break;
      }
    }
  }

  private void onReceivedHello(
    final SocketAddress address,
    final CoServerHello hello)
  {
    switch (this.state.current()) {
      case STATE_WAITING_FOR_HELLO: {
        switch (hello.getValueCase()) {
          case OK: {
            this.onReceivedHelloOK(address, hello.getOk());
            break;
          }
          case ERROR: {
            this.onReceivedHelloError(address, hello.getError());
            break;
          }
          case VALUE_NOT_SET:
            LOG.error("received invalid message: {}: {}", address, hello);
        }
        break;
      }
      case STATE_INITIAL:
      case STATE_CONNECTED:
      case STATE_DISCONNECTED: {
        LOG.error("ignored unexpected Hello packet");
        break;
      }
    }
  }

  private void onReceivedHelloError(
    final SocketAddress address,
    final CoServerHelloError error)
  {
    LOG.error("server rejected connection: {}", error.getMessage());
    this.state.transition(State.STATE_DISCONNECTED);
    this.events.post(CoClientNetworkEventConnectionRefused.of(error.getMessage()));
  }

  private void onReceivedHelloOK(
    final SocketAddress address,
    final CoServerHelloOK ok)
  {
    final int received_id = ok.getClientId();
    LOG.info(
      "connected with client id {}",
      Integer.toUnsignedString(received_id, 16));
    this.client_id = received_id;
    this.state.transition(State.STATE_CONNECTED);
    this.events.post(CoClientNetworkEventConnected.of(received_id));
  }

  private void sendHello(
    final String user,
    final String pass)
    throws IOException
  {
    ++this.hello_attempts;
    this.sendMessage(
      CoClientPacket.newBuilder()
        .setHello(CoClientHello.newBuilder()
                    .setName(user)
                    .setPassword(pass)
                    .build())
        .build());
  }

  private void sendMessage(
    final MessageLite m)
    throws IOException
  {
    this.checkMessageSize(m);

    try (final ByteBufferOutputStream out =
           new ByteBufferOutputStream(this.buffer)) {
      m.writeTo(out);
    }

    this.buffer.flip();
    this.sink.send(this.buffer);
  }

  @Override
  public void close()
    throws IOException
  {
    this.sink.close();
  }

  private static final class ByteBufferOutputStream extends OutputStream
  {
    private final ByteBuffer buffer;

    ByteBufferOutputStream(
      final ByteBuffer in_buffer)
    {
      this.buffer = NullCheck.notNull(in_buffer, "Buffer");
      this.buffer.rewind();
    }

    @Override
    public void write(
      final int b)
      throws IOException
    {
      this.buffer.put((byte) b);
    }
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
}

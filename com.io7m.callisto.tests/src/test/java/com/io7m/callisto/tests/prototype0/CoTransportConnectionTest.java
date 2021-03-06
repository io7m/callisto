/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.callisto.tests.prototype0;

import com.google.protobuf.ByteString;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSocketType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderLocal;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPool;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantReference;
import com.io7m.callisto.prototype0.stringconstants.messages.CoStringConstant;
import com.io7m.callisto.prototype0.transport.CoTransportConnection;
import com.io7m.callisto.prototype0.transport.CoTransportConnectionConfiguration;
import com.io7m.callisto.prototype0.transport.CoTransportConnectionListenerType;
import com.io7m.callisto.prototype0.transport.CoTransportConnectionType;
import com.io7m.callisto.prototype0.transport.CoTransportConnectionUsableType;
import com.io7m.callisto.prototype0.transport.messages.CoDataUnreliable;
import com.io7m.callisto.prototype0.transport.messages.CoMessage;
import com.io7m.callisto.prototype0.transport.messages.CoPacket;
import com.io7m.callisto.prototype0.transport.messages.CoPacketID;
import com.io7m.jnull.NullCheck;
import com.io7m.jranges.RangeCheckException;
import mockit.Delegate;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.io7m.callisto.prototype0.transport.CoTransportConnectionUsableType.Reliability;

public final class CoTransportConnectionTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoTransportConnectionTest.class);

  @Rule
  public final ExpectedException expected = ExpectedException.none();

  @Test
  public void testTransportReceiveUnreliableReordered(
    final @Mocked CoTransportConnectionListenerType listener)
  {
    final Setup setup = new Setup(listener);

    final CoTransportConnectionType connection =
      CoTransportConnection.create(
        Clock.systemUTC(),
        setup.logging_listener,
        setup.strings,
        setup.peer,
        CoTransportConnectionConfiguration.of(30, 30 * 30),
        setup.remote,
        0x4543b73e);

    final byte[] data = new byte[1];

    IntStream.of(4, 3, 2, 1, 0).forEach(id -> {
      final CoMessage message =
        CoMessage.newBuilder()
          .setMessageType(CoStringConstant.newBuilder().setValue(1))
          .setMessageId(id)
          .setMessageData(ByteString.copyFrom(data))
          .build();

      final CoPacketID packet_id =
        CoPacketID.newBuilder()
          .setChannel(0)
          .setConnectionId(0x1)
          .setSequence(id)
          .build();

      final CoPacket packet =
        CoPacket.newBuilder()
          .setDataUnreliable(
            CoDataUnreliable.newBuilder()
              .setId(packet_id)
              .addMessages(message)
              .build())
          .build();

      connection.receive(packet);
    });

    new StrictExpectations()
    {{
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 4, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 3, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 2, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 1, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0, this.with(new AnyInteger()).intValue());

      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(0)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(1)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(2)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(3)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(4)));
    }};

    connection.tick();
  }

  @Test
  public void testTransportReceiveUnreliableDiscontinuous(
    final @Mocked CoTransportConnectionListenerType listener)
  {
    final Setup setup = new Setup(listener);

    final CoTransportConnectionType connection =
      CoTransportConnection.create(
        Clock.systemUTC(),
        setup.logging_listener,
        setup.strings,
        setup.peer,
        CoTransportConnectionConfiguration.of(30, 30 * 30),
        setup.remote,
        0x4543b73e);

    final byte[] data = new byte[1];

    IntStream.of(0, 2, 4, 6).forEach(id -> {
      final CoMessage message =
        CoMessage.newBuilder()
          .setMessageType(CoStringConstant.newBuilder().setValue(1))
          .setMessageId(id)
          .setMessageData(ByteString.copyFrom(data))
          .build();

      final CoPacketID packet_id =
        CoPacketID.newBuilder()
          .setChannel(0)
          .setConnectionId(0x1)
          .setSequence(id)
          .build();

      final CoPacket packet =
        CoPacket.newBuilder()
          .setDataUnreliable(
            CoDataUnreliable.newBuilder()
              .setId(packet_id)
              .addMessages(message)
              .build())
          .build();

      connection.receive(packet);
    });

    new StrictExpectations()
    {{
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 2, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 4, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 6, this.with(new AnyInteger()).intValue());

      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(0)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(2)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(4)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(6)));
    }};

    connection.tick();
  }

  @Test
  public void testTransportReceiveUnreliableDiscontinuousUpperRange(
    final @Mocked CoTransportConnectionListenerType listener)
  {
    final Setup setup = new Setup(listener);

    final CoTransportConnectionType connection =
      CoTransportConnection.create(
        Clock.systemUTC(),
        setup.logging_listener,
        setup.strings,
        setup.peer,
        CoTransportConnectionConfiguration.of(30, 30 * 30),
        setup.remote,
        0x4543b73e);

    final byte[] data = new byte[1];

    IntStream.of(
      0xfffffb,
      0xfffffc,
      0xfffffd,
      0xfffffe,
      0xffffff,
      0,
      1,
      2,
      3,
      4).forEach(id -> {
      final CoMessage message =
        CoMessage.newBuilder()
          .setMessageType(CoStringConstant.newBuilder().setValue(1))
          .setMessageId(id)
          .setMessageData(ByteString.copyFrom(data))
          .build();

      final CoPacketID packet_id =
        CoPacketID.newBuilder()
          .setChannel(0)
          .setConnectionId(0x1)
          .setSequence(id)
          .build();

      final CoPacket packet =
        CoPacket.newBuilder()
          .setDataUnreliable(
            CoDataUnreliable.newBuilder()
              .setId(packet_id)
              .addMessages(message)
              .build())
          .build();

      connection.receive(packet);
    });

    new StrictExpectations()
    {{
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0xfffffb, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0xfffffc, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0xfffffd, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0xfffffe, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0xffffff, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 1, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 2, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 3, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 4, this.with(new AnyInteger()).intValue());

      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(0xfffffb)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(0xfffffc)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(0xfffffd)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(0xfffffe)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(0xffffff)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(0)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(1)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(2)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(3)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(4)));
    }};

    connection.tick();
  }

  @Test
  public void testTransportReceiveUnreliableDuplicates(
    final @Mocked CoTransportConnectionListenerType listener)
  {
    final Setup setup = new Setup(listener);

    final CoTransportConnectionType connection =
      CoTransportConnection.create(
        Clock.systemUTC(),
        setup.logging_listener,
        setup.strings,
        setup.peer,
        CoTransportConnectionConfiguration.of(30, 30 * 30),
        setup.remote,
        0x4543b73e);

    final byte[] data = new byte[4];
    final ByteBuffer wrap = ByteBuffer.wrap(data);

    final AtomicInteger x = new AtomicInteger(0);
    IntStream.of(0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4).forEach(id -> {
      wrap.putInt(0, x.getAndIncrement());

      final CoMessage message =
        CoMessage.newBuilder()
          .setMessageType(CoStringConstant.newBuilder().setValue(x.intValue()))
          .setMessageId(id)
          .setMessageData(ByteString.copyFrom(data))
          .build();

      final CoPacketID packet_id =
        CoPacketID.newBuilder()
          .setChannel(0)
          .setConnectionId(0x1)
          .setSequence(id)
          .build();

      final CoPacket packet =
        CoPacket.newBuilder()
          .setDataUnreliable(
            CoDataUnreliable.newBuilder()
              .setId(packet_id)
              .addMessages(message)
              .build())
          .build();

      connection.receive(packet);
    });

    new StrictExpectations()
    {{
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 0, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 1, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 1, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 1, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 2, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 2, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 2, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 3, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 3, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 3, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 4, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 4, this.with(new AnyInteger()).intValue());
      listener.onReceivePacketDeliverUnreliable(
        connection, 0, 4, this.with(new AnyInteger()).intValue());

      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(0)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(1)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(2)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(3)));
      listener.onMessageReceived(
        connection, 0, this.with(new CoMessageIDChecker(4)));
    }};

    connection.tick();
  }

  @Test
  public void testTransportSendUnreliable(
    final @Mocked CoTransportConnectionListenerType listener)
  {
    final Setup setup = new Setup(listener);

    final CoTransportConnectionType connection =
      CoTransportConnection.create(
        Clock.systemUTC(),
        setup.logging_listener,
        setup.strings,
        setup.peer,
        CoTransportConnectionConfiguration.of(30, 30 * 30),
        setup.remote,
        0x4543b73e);

    final byte[] data = new byte[10];
    final ByteBuffer message = ByteBuffer.wrap(data);

    new StrictExpectations()
    {{
      listener.onChannelCreated(connection, 0);

      listener.onEnqueuePacketUnreliable(
        connection,
        0,
        0,
        this.with(new PacketSizeChecker()).intValue());

      listener.onEnqueuePacketUnreliable(
        connection,
        0,
        1,
        this.with(new PacketSizeChecker()).intValue());

      listener.onSendPacketUnreliable(
        connection,
        0,
        0,
        this.with(new PacketSizeChecker()).intValue());

      listener.onSendPacketUnreliable(
        connection,
        0,
        1,
        this.with(new PacketSizeChecker()).intValue());
    }};

    for (int index = 0; index < 100; ++index) {
      final Optional<String> s_opt =
        setup.strings.lookupString(CoStringConstantReference.of(index % 5));

      connection.send(
        Reliability.MESSAGE_UNRELIABLE, 0, s_opt.get(), message);
      message.rewind();
    }

    connection.tick();
  }

  @Test
  public void testTransportSendBadChannel(
    final @Mocked CoTransportConnectionListenerType listener)
  {
    final Setup setup = new Setup(listener);

    final CoTransportConnectionType connection =
      CoTransportConnection.create(
        Clock.systemUTC(),
        setup.logging_listener,
        setup.strings,
        setup.peer,
        CoTransportConnectionConfiguration.of(30, 30 * 30),
        setup.remote,
        0x4543b73e);

    final byte[] data = new byte[10];
    final ByteBuffer message = ByteBuffer.wrap(data);

    this.expected.expect(RangeCheckException.class);
    connection.send(
      Reliability.MESSAGE_RELIABLE,
      256,
      "com.io7m.callist0.example0.type0",
      message);
  }

  private static final class LoggingListener
    implements CoTransportConnectionListenerType
  {
    private final CoTransportConnectionListenerType listener;

    public LoggingListener(
      final CoTransportConnectionListenerType in_listener)
    {
      this.listener = NullCheck.notNull(in_listener, "Listener");
    }

    @Override
    public void onClosed(
      final CoTransportConnectionUsableType connection,
      final String message)
    {
      LOG.debug("onClosed: {}: {}", connection, message);
      this.listener.onClosed(connection, message);
    }

    @Override
    public void onTimedOut(
      final CoTransportConnectionUsableType connection)
    {
      LOG.debug("onConnectionTimedOut: {}", connection);
      this.listener.onTimedOut(connection);
    }

    @Override
    public void onEnqueuePacketReliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onEnqueuePacketReliable: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onEnqueuePacketReliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onEnqueuePacketReliableRequeue(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onEnqueuePacketReliableRequeue: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onEnqueuePacketReliableRequeue(
        connection, channel, sequence, size);
    }

    @Override
    public void onEnqueuePacketUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onEnqueuePacketUnreliable: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onEnqueuePacketUnreliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onEnqueuePacketReliableFragment(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onEnqueuePacketReliableFragment: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onEnqueuePacketReliableFragment(
        connection, channel, sequence, size);
    }

    @Override
    public void onEnqueuePacketAck(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onEnqueuePacketReceipt: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onEnqueuePacketAck(
        connection, channel, sequence, size);
    }

    @Override
    public void onSendPacketReliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onSendPacketReliable: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onSendPacketReliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onSendPacketUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onSendPacketUnreliable: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onSendPacketUnreliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onSendPacketReliableFragment(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onSendPacketReliableFragment: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onSendPacketReliableFragment(
        connection, channel, sequence, size);
    }

    @Override
    public void onSendPacketAck(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onSendPacketReceipt: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onSendPacketAck(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceiveDropPacketUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onDropPacketUnreliable: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onReceiveDropPacketUnreliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onMessageReceived(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final CoMessage message)
    {
      LOG.debug(
        "onMessageReceived: {} {}",
        Integer.valueOf(channel),
        message);
      this.listener.onMessageReceived(connection, channel, message);
    }

    @Override
    public void onReceivePacketDeliverReliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onReceivePacketDeliverReliable: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onReceivePacketDeliverReliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketDeliverUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onReceivePacketDeliverUnreliable: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onReceivePacketDeliverUnreliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketReliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onReceivePacketReliable: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onReceivePacketReliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onReceivePacketUnreliable: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onReceivePacketUnreliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketReliableFragment(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onReceivePacketReliableFragment: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onReceivePacketReliableFragment(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketAck(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onReceivePacketReceipt: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onReceivePacketAck(
        connection, channel, sequence, size);
    }

    @Override
    public void onSavedPacketReliableSave(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onSavedPacketReliableSave: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onSavedPacketReliableSave(
        connection, channel, sequence, size);
    }

    @Override
    public void onSavedPacketReliableExpire(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      LOG.debug(
        "onSavedPacketReliableExpire: {} {} {}",
        Integer.valueOf(channel),
        Integer.valueOf(sequence),
        Integer.valueOf(size));
      this.listener.onSavedPacketReliableExpire(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketPing(
      final CoTransportConnectionUsableType connection)
    {
      LOG.debug("onReceivePacketPing: {}", connection);
      this.listener.onReceivePacketPing(connection);
    }

    @Override
    public void onSendPacketPong(
      final CoTransportConnectionUsableType connection)
    {
      LOG.debug("onSendPacketPong: {}", connection);
      this.listener.onSendPacketPong(connection);
    }

    @Override
    public void onReceivePacketPong(
      final CoTransportConnectionUsableType connection)
    {
      LOG.debug("onReceivePacketPong: {}", connection);
      this.listener.onReceivePacketPong(connection);
    }

    @Override
    public void onSendPacketPing(
      final CoTransportConnectionUsableType connection)
    {
      LOG.debug("onSendPacketPing: {}", connection);
      this.listener.onSendPacketPing(connection);
    }

    @Override
    public void onReceivePacketAckNotAvailable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence)
    {
      LOG.debug(
        "onReceivePacketAckNotAvailable: {}: {} {}",
        connection,
        Integer.valueOf(channel),
        Integer.valueOf(sequence));
      this.listener.onReceivePacketAckNotAvailable(
        connection,
        channel,
        sequence);
    }

    @Override
    public void onReceivePacketBadChannel(
      final CoTransportConnectionUsableType connection,
      final int channel)
    {
      LOG.debug(
        "onReceivePacketBadChannel: {}: {}",
        connection,
        Integer.valueOf(channel));
      this.listener.onReceivePacketBadChannel(connection, channel);
    }

    @Override
    public void onChannelCreated(
      final CoTransportConnectionUsableType connection,
      final int channel)
    {
      LOG.debug(
        "onChannelCreated: {}: {}",
        connection,
        Integer.valueOf(channel));
      this.listener.onChannelCreated(connection, channel);
    }
  }

  private static final class PacketSizeChecker implements Delegate<Integer>
  {
    PacketSizeChecker()
    {

    }

    boolean checkSize(
      final int size)
    {
      return size >= 10 && size < 1200;
    }
  }

  private static class CoMessageIDChecker implements Delegate<CoMessage>
  {
    private final int id;

    CoMessageIDChecker(
      final int in_id)
    {
      this.id = in_id;
    }

    boolean check(
      final CoMessage m)
    {
      return m.getMessageId() == this.id;
    }
  }

  private static final class Setup
  {
    private final CoTransportConnectionListenerType listener;
    private final CoTransportConnectionListenerType logging_listener;
    private final CoStringConstantPoolType strings;
    private final CoNetworkPacketSocketType peer;
    private final SocketAddress remote;

    Setup(
      final CoTransportConnectionListenerType listener)
    {
      this.listener = listener;

      this.logging_listener =
        new LoggingListener(listener);
      this.strings =
        new CoStringConstantPool(() -> LOG.debug("updated string pool"));

      this.strings.newUpdate()
        .set(0, "com.io7m.callist0.example0.type0")
        .set(1, "com.io7m.callist0.example0.type1")
        .set(2, "com.io7m.callist0.example0.type2")
        .set(3, "com.io7m.callist0.example0.type3")
        .set(4, "com.io7m.callist0.example0.type4")
        .execute();

      final CoNetworkProviderLocal provider =
        new CoNetworkProviderLocal();
      final Properties props =
        new Properties();
      this.peer = provider.createSocket(props);
      this.remote = new InetSocketAddress("::1", 9999);
    }
  }

  private static class AnyInteger implements Delegate<Integer>
  {
    boolean check(final Integer x)
    {
      return true;
    }
  }
}

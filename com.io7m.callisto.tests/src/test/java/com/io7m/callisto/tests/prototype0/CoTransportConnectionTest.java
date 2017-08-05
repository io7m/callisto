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

import com.io7m.callisto.prototype0.network.CoNetworkPacketSocketType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderLocal;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPool;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantReference;
import com.io7m.callisto.prototype0.transport.CoTransportConnection;
import com.io7m.callisto.prototype0.transport.CoTransportConnectionType;
import com.io7m.callisto.prototype0.transport.CoTransportConnectionUsableType;
import com.io7m.jnull.NullCheck;
import mockit.Delegate;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Properties;

public final class CoTransportConnectionTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoTransportConnectionTest.class);

  @Test
  public void testTransportSendUnreliable(
    final @Mocked CoTransportConnection.ListenerType listener)
  {
    final CoTransportConnection.ListenerType logging_listener =
      new LoggingListener(listener);

    final CoStringConstantPoolType strings =
      new CoStringConstantPool(() -> LOG.debug("updated string pool"));

    strings.newUpdate()
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
    final CoNetworkPacketSocketType peer =
      provider.createSocket(props);
    final SocketAddress remote =
      new InetSocketAddress("::1", 9999);

    final CoTransportConnectionType connection =
      new CoTransportConnection(
        logging_listener,
        strings,
        peer,
        remote,
        0x4543b73e);

    final byte[] data = new byte[10];
    final ByteBuffer message = ByteBuffer.wrap(data);

    new StrictExpectations()
    {{
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
      connection.send(
        CoTransportConnectionUsableType.Reliability.MESSAGE_UNRELIABLE,
        0,
        strings.lookupString(CoStringConstantReference.of(index % 5)),
        message);
      message.rewind();
    }

    connection.tick();
  }

  private static final class LoggingListener
    implements CoTransportConnection.ListenerType
  {
    private final CoTransportConnection.ListenerType listener;

    public LoggingListener(
      final CoTransportConnection.ListenerType in_listener)
    {
      this.listener = NullCheck.notNull(in_listener, "Listener");
    }

    @Override
    public void onConnectionClosed(
      final CoTransportConnectionUsableType connection)
    {
      LOG.debug("onConnectionClosed: {}", connection);
      this.listener.onConnectionClosed(connection);
    }

    @Override
    public void onConnectionTimedOut(
      final CoTransportConnectionUsableType connection)
    {
      LOG.debug("onConnectionTimedOut: {}", connection);
      this.listener.onConnectionTimedOut(connection);
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
}

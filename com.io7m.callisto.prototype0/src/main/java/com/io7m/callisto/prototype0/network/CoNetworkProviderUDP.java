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

package com.io7m.callisto.prototype0.network;

import com.io7m.jnull.NullCheck;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyIncorrectType;
import com.io7m.jproperties.JPropertyNonexistent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Optional;
import java.util.Properties;

public final class CoNetworkProviderUDP implements CoNetworkProviderType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoNetworkProviderUDP.class);

  public CoNetworkProviderUDP()
  {

  }

  @Override
  public CoNetworkPacketPeerType createPeer(
    final Properties p)
    throws CoNetworkException
  {
    NullCheck.notNull(p, "Properties");

    DatagramChannel channel = null;
    Selector selector = null;

    try {
      channel = DatagramChannel.open();
      selector = Selector.open();

      final Optional<InetSocketAddress> bound;
      if (p.containsKey("local_address")) {
        final BigInteger addr_port =
          JProperties.getBigInteger(p, "local_port");
        final String addr_text =
          JProperties.getString(p, "local_address");

        final int port =
          addr_port.intValueExact();
        final InetAddress addr =
          InetAddress.getByName(addr_text);
        final InetSocketAddress sock_addr =
          new InetSocketAddress(addr, port);

        LOG.debug("bind {}", sock_addr);
        channel.bind(sock_addr);
        bound = Optional.of(sock_addr);
      } else {
        bound = Optional.empty();
      }

      channel.configureBlocking(false);

      final Optional<SocketAddress> remote;
      if (p.containsKey("remote_address")) {
        final BigInteger addr_port =
          JProperties.getBigInteger(p, "remote_port");
        final String addr_text =
          JProperties.getString(p, "remote_address");

        final int port =
          addr_port.intValueExact();
        final InetAddress addr =
          InetAddress.getByName(addr_text);
        final InetSocketAddress sock_addr =
          new InetSocketAddress(addr, port);

        LOG.debug("connect {}", sock_addr);
        channel.connect(sock_addr);
        remote = Optional.of(sock_addr);
      } else {
        remote = Optional.empty();
      }

      channel.register(selector, SelectionKey.OP_READ);
      return new Peer(bound, remote, selector, channel);
    } catch (final JPropertyNonexistent | JPropertyIncorrectType | UnknownHostException | ArithmeticException ex) {
      final CoNetworkConfigurationException thrown =
        new CoNetworkConfigurationException(ex);

      try {
        if (channel != null) {
          channel.close();
        }
      } catch (final IOException e) {
        thrown.addSuppressed(e);
      }

      try {
        if (selector != null) {
          selector.close();
        }
      } catch (final IOException e) {
        thrown.addSuppressed(e);
      }

      throw thrown;
    } catch (final IOException e) {
      throw new CoNetworkIOException(e);
    }
  }

  private static final class Peer implements CoNetworkPacketPeerType
  {
    private final Optional<InetSocketAddress> local_address;
    private final Selector selector;
    private final DatagramChannel channel;
    private final ByteBuffer buffer;
    private final Optional<SocketAddress> remote_address;

    private Peer(
      final Optional<InetSocketAddress> in_bind_addr,
      final Optional<SocketAddress> in_remote_addr,
      final Selector in_selector,
      final DatagramChannel in_channel)
    {
      this.local_address =
        NullCheck.notNull(in_bind_addr, "Bound Address");
      this.remote_address =
        NullCheck.notNull(in_remote_addr, "Remote Address");
      this.selector =
        NullCheck.notNull(in_selector, "Selector");
      this.channel =
        NullCheck.notNull(in_channel, "Channel");
      this.buffer =
        ByteBuffer.allocateDirect(4096);
    }

    @Override
    public String toString()
    {
      return new StringBuilder(128)
        .append("[UDP [")
        .append(this.local_address)
        .append("]]")
        .toString();
    }

    @Override
    public void close()
      throws IOException
    {
      if (this.channel.isOpen()) {
        LOG.debug("close {}", this.local_address);
        this.channel.close();
        this.selector.close();
      }
    }

    @Override
    public void poll(
      final CoNetworkPacketReceiverType receiver)
      throws IOException
    {
      NullCheck.notNull(receiver, "Receiver");

      while (true) {
        final int r = this.selector.selectNow();
        if (r == 0) {
          break;
        }

        this.selector.selectedKeys().clear();
        this.buffer.rewind();
        final SocketAddress address = this.channel.receive(this.buffer);
        this.buffer.flip();
        receiver.receive(address, this.buffer);
      }
    }

    @Override
    public void send(
      final SocketAddress remote_address,
      final ByteBuffer data)
      throws IOException
    {
      NullCheck.notNull(remote_address, "Address");
      NullCheck.notNull(data, "Data");
      this.channel.send(data, remote_address);
    }

    @Override
    public Optional<SocketAddress> remote()
    {
      return this.remote_address;
    }
  }
}

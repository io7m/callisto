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

import com.io7m.callisto.prototype0.idpool.CoIDPool;
import com.io7m.callisto.prototype0.idpool.CoIDPoolType;
import com.io7m.jnull.NullCheck;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyIncorrectType;
import com.io7m.jproperties.JPropertyNonexistent;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CoNetworkProviderLocal implements CoNetworkProviderType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoNetworkProviderLocal.class);

  private final Object2ReferenceOpenHashMap<InetSocketAddress, Node> nodes;
  private final CoIDPoolType ports;

  public CoNetworkProviderLocal()
  {
    this.nodes = new Object2ReferenceOpenHashMap<>();
    this.ports = new CoIDPool();
  }

  @Override
  public CoNetworkPacketSourceType createPacketSource(
    final Properties p)
  {
    NullCheck.notNull(p, "Properties");

    try {
      final BigInteger addr_port =
        JProperties.getBigInteger(p, "local_port");
      final String addr_text =
        JProperties.getString(p, "local_address");

      final int port =
        addr_port.intValueExact();
      final InetAddress addr =
        InetAddress.getByName(addr_text);
      final InetSocketAddress bind_address =
        new InetSocketAddress(addr, port);

      LOG.debug("bind {}", bind_address);

      final Node node = new Node(bind_address, null);
      synchronized (this.nodes) {
        if (this.nodes.containsKey(bind_address)) {
          throw new SocketException("Address already in use: " + bind_address);
        }
        this.nodes.put(bind_address, node);
      }
      return node;
    } catch (final JPropertyNonexistent | JPropertyIncorrectType | UnknownHostException | ArithmeticException | SocketException ex) {
      throw new CoNetworkConfigurationException(ex);
    }
  }

  @Override
  public CoNetworkPacketSinkType createPacketSink(
    final Properties p)
    throws CoNetworkException
  {
    NullCheck.notNull(p, "Properties");

    try {
      final BigInteger addr_port =
        JProperties.getBigInteger(p, "remote_port");
      final String addr_text =
        JProperties.getString(p, "remote_address");

      final InetSocketAddress bind_address =
        new InetSocketAddress("::1", this.ports.fresh());

      final int port =
        addr_port.intValueExact();
      final InetAddress addr =
        InetAddress.getByName(addr_text);
      final InetSocketAddress remote_address =
        new InetSocketAddress(addr, port);

      LOG.debug("bind {}", bind_address);
      LOG.debug("connect {}", remote_address);

      final Node node = new Node(bind_address, remote_address);
      synchronized (this.nodes) {
        if (this.nodes.containsKey(bind_address)) {
          throw new SocketException("Address already in use: " + bind_address);
        }
        this.nodes.put(bind_address, node);
      }
      return node;
    } catch (final JPropertyNonexistent | JPropertyIncorrectType | UnknownHostException | ArithmeticException | SocketException ex) {
      throw new CoNetworkConfigurationException(ex);
    }
  }

  private final class Node implements CoNetworkPacketSourceType,
    CoNetworkPacketSinkType
  {
    private final InetSocketAddress bind;
    private final InetSocketAddress remote;
    private final ConcurrentLinkedQueue<CoNetworkLocalDatagram> incoming;

    private Node(
      final InetSocketAddress bind_addr,
      final InetSocketAddress remote_addr)
    {
      this.bind = bind_addr;
      this.remote = remote_addr;
      this.incoming = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void send(
      final ByteBuffer data)
      throws IOException
    {
      NullCheck.notNull(data, "Data");

      final Node peer;
      synchronized (CoNetworkProviderLocal.this.nodes) {
        peer = CoNetworkProviderLocal.this.nodes.get(this.remote);
      }

      if (peer != null) {
        final ByteBuffer clone = ByteBuffer.allocate(data.limit());
        clone.put(data);
        clone.rewind();
        peer.incoming.add(CoNetworkLocalDatagram.of(this.bind, clone));
      }
    }

    @Override
    public void send(
      final SocketAddress address,
      final ByteBuffer data)
      throws IOException
    {
      NullCheck.notNull(address, "Address");
      NullCheck.notNull(data, "Data");

      final Node peer;
      synchronized (CoNetworkProviderLocal.this.nodes) {
        peer = CoNetworkProviderLocal.this.nodes.get(address);
      }

      if (peer != null) {
        final ByteBuffer clone = ByteBuffer.allocate(data.limit());
        clone.put(data);
        clone.rewind();
        peer.incoming.add(CoNetworkLocalDatagram.of(this.bind, clone));
      }
    }

    @Override
    public void poll(
      final CoNetworkPacketReceiverType receiver)
      throws IOException
    {
      NullCheck.notNull(receiver, "Receiver");

      final Iterator<CoNetworkLocalDatagram> iter = this.incoming.iterator();
      while (iter.hasNext()) {
        final CoNetworkLocalDatagram datagram = iter.next();
        iter.remove();
        receiver.receive(datagram.sender(), datagram.data());
      }
    }

    @Override
    public void close()
      throws IOException
    {
      CoNetworkProviderLocal.this.closeNode(this.bind);
    }
  }

  private void closeNode(
    final InetSocketAddress bind)
  {
    LOG.debug("close {}", bind);

    synchronized (CoNetworkProviderLocal.this.nodes) {
      this.ports.release(bind.getPort());
      this.nodes.remove(bind, this);
    }
  }
}

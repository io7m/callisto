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
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public final class CoNetworkProviderLocal implements CoNetworkProviderType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoNetworkProviderLocal.class);

  private final Object2ReferenceOpenHashMap<InetSocketAddress, Node> nodes;
  private final CoIDPoolType ports;
  private final ExecutorService exec;
  private final SecureRandom random;
  private volatile double loss;
  private volatile long latency_min;
  private volatile long latency_max;

  public CoNetworkProviderLocal()
  {
    this.nodes = new Object2ReferenceOpenHashMap<>();
    this.ports = new CoIDPool();
    this.random = new SecureRandom();
    this.loss = 0.0;
    this.latency_min = 0L;
    this.latency_max = 0L;

    this.exec = Executors.newSingleThreadExecutor(r -> {
      final Thread th = new Thread(r);
      th.setName("com.io7m.callisto.network.local.deliver");
      th.setDaemon(true);
      return th;
    });
  }

  public void setPacketLoss(
    final double in_loss)
  {
    this.loss = Math.min(1.0, Math.max(0.0, in_loss));
  }

  public void setPacketLatency(
    final long in_latency_min,
    final long in_latency_max)
  {
    this.latency_min = Math.max(0L, in_latency_min);
    this.latency_max = Math.min(this.latency_min, in_latency_max);
  }

  @Override
  public CoNetworkPacketSocketType createSocket(
    final Properties p)
    throws CoNetworkException
  {
    NullCheck.notNull(p, "Properties");

    try {
      final Node node;

      synchronized (this.nodes) {

        final InetSocketAddress bind_address;
        if (p.containsKey("local_port")) {
          final BigInteger addr_port =
            JProperties.getBigInteger(p, "local_port");
          final String addr_text =
            JProperties.getString(p, "local_address");

          final int port =
            addr_port.intValueExact();
          final InetAddress addr =
            InetAddress.getByName(addr_text);
          bind_address =
            new InetSocketAddress(addr, port);
        } else {
          bind_address =
            new InetSocketAddress("::1", this.ports.fresh());
        }

        final Optional<InetSocketAddress> remote_address;
        if (p.containsKey("remote_address")) {
          final BigInteger addr_port =
            JProperties.getBigInteger(p, "remote_port");
          final String addr_text =
            JProperties.getString(p, "remote_address");

          final int port =
            addr_port.intValueExact();
          final InetAddress addr =
            InetAddress.getByName(addr_text);
          remote_address = Optional.of(new InetSocketAddress(addr, port));
        } else {
          remote_address = Optional.empty();
        }

        LOG.debug("bind {}", bind_address);
        if (this.nodes.containsKey(bind_address)) {
          throw new SocketException("Address already in use: " + bind_address);
        }

        node = new Node(bind_address, remote_address);
        this.nodes.put(bind_address, node);
      }

      return node;
    } catch (final JPropertyNonexistent | JPropertyIncorrectType | ArithmeticException | SocketException | UnknownHostException ex) {
      throw new CoNetworkConfigurationException(ex);
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

  private final class Node implements CoNetworkPacketSocketType
  {
    private final InetSocketAddress bind;
    private final ConcurrentLinkedQueue<CoNetworkLocalDatagram> incoming;
    private final Optional<InetSocketAddress> remote;

    private Node(
      final InetSocketAddress bind_addr,
      final Optional<InetSocketAddress> remote_addr)
    {
      this.bind = bind_addr;
      this.remote = remote_addr;
      this.incoming = new ConcurrentLinkedQueue<>();
    }

    private void enqueueIncoming(
      final CoNetworkLocalDatagram datagram)
    {
      final CoNetworkProviderLocal c = CoNetworkProviderLocal.this;

      if (c.random.nextDouble() <= c.loss) {
        LOG.trace(
          "[{} -> {}]: losing packet",
          datagram.sender(),
          this.bind);
        return;
      }

      if (c.latency_max == 0L) {
        this.incoming.add(datagram);
        return;
      }

      final long bound = c.latency_max - c.latency_min;
      final long delay = c.latency_min + (long) c.random.nextInt((int) bound);

      LOG.trace(
        "[{} -> {}]: delaying delivery {}ms",
        datagram.sender(),
        this.bind,
        Long.valueOf(delay));

      c.exec.execute(() -> {
        try {
          Thread.sleep(delay);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        this.incoming.add(datagram);
      });
    }

    @Override
    public int maximumTransferUnit()
    {
      return 1200;
    }

    @Override
    public void send(
      final SocketAddress remote_address,
      final ByteBuffer data)
    {
      NullCheck.notNull(remote_address, "Address");
      NullCheck.notNull(data, "Data");

      final Node peer;
      synchronized (CoNetworkProviderLocal.this.nodes) {
        peer = CoNetworkProviderLocal.this.nodes.get(remote_address);
      }

      if (peer != null) {
        final ByteBuffer clone = ByteBuffer.allocate(data.limit());
        clone.put(data);
        clone.rewind();
        peer.enqueueIncoming(CoNetworkLocalDatagram.of(this.bind, clone));
      }
    }

    @Override
    public void poll(
      final CoNetworkPacketReceiverType receiver)
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

    @Override
    public Optional<SocketAddress> remote()
    {
      return this.remote.map(Function.identity());
    }
  }
}

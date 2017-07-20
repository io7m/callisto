package com.io7m.callisto.tests.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class UDPExample
{
  private static final Logger LOG =
    LoggerFactory.getLogger(UDPExample.class);

  private UDPExample()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final InetSocketAddress server_sock_addr =
      new InetSocketAddress(InetAddress.getByName("::1"), 9999);
    final DatagramChannel server_channel =
      DatagramChannel.open();
    final Selector server_selector =
      Selector.open();

    LOG.debug("server channel: {}", server_channel);

    LOG.debug("bind {}", server_sock_addr);

    server_channel.bind(server_sock_addr);
    server_channel.configureBlocking(false);

    final DatagramChannel client_channel =
      DatagramChannel.open();
    final Selector client_selector =
      Selector.open();

    LOG.debug("connect {}", server_sock_addr);

    client_channel.connect(server_sock_addr);
    client_channel.configureBlocking(false);

    LOG.debug("client: {}", client_channel.getLocalAddress());

    final SelectionKey server_key =
      server_channel.register(
        server_selector,
        SelectionKey.OP_READ);

    final SelectionKey client_key =
      client_channel.register(
        client_selector,
        SelectionKey.OP_WRITE);

    while (true) {
      LOG.debug("waiting for client write");

      final int r = client_selector.select(
        TimeUnit.MILLISECONDS.convert(10L, TimeUnit.SECONDS));

      final Set<SelectionKey> keys = client_selector.selectedKeys();
      LOG.debug("selected: {}", Integer.valueOf(keys.size()));

      final Iterator<SelectionKey> iter =
        client_selector.selectedKeys().iterator();
      while (iter.hasNext()) {
        final SelectionKey key = iter.next();
        LOG.debug("key: writable {}", Boolean.valueOf(key.isWritable()));
        iter.remove();
      }

      if (r > 0) {
        break;
      }
    }

    client_channel.write(ByteBuffer.wrap(new byte[]{
      (byte) 0, (byte) 1, (byte) 2, (byte) 3
    }));

    client_channel.write(ByteBuffer.wrap(new byte[]{
      (byte) 4, (byte) 5, (byte) 6, (byte) 7
    }));

    while (true) {
      LOG.debug("waiting for server read");

      final int r = server_selector.select(
        TimeUnit.MILLISECONDS.convert(10L, TimeUnit.SECONDS));

      final Set<SelectionKey> keys = server_selector.selectedKeys();
      LOG.debug("selected: {}", Integer.valueOf(keys.size()));

      final Iterator<SelectionKey> iter =
        server_selector.selectedKeys().iterator();
      while (iter.hasNext()) {
        final SelectionKey key = iter.next();
        LOG.debug("key: readable {}", Boolean.valueOf(key.isReadable()));
        LOG.debug("key: channel {}", key.channel());
        iter.remove();
      }

      if (r > 0) {
        break;
      }
    }

    {
      final ByteBuffer buffer = ByteBuffer.allocate(8);
      final SocketAddress remote = server_channel.receive(buffer);
      LOG.debug(
        "{}: {} {} {} {}",
        remote,
        Byte.valueOf(buffer.get(0)),
        Byte.valueOf(buffer.get(1)),
        Byte.valueOf(buffer.get(2)),
        Byte.valueOf(buffer.get(3)));
    }

    {
      final ByteBuffer buffer = ByteBuffer.allocate(8);
      final SocketAddress remote = server_channel.receive(buffer);
      LOG.debug(
        "{}: {} {} {} {}",
        remote,
        Byte.valueOf(buffer.get(0)),
        Byte.valueOf(buffer.get(1)),
        Byte.valueOf(buffer.get(2)),
        Byte.valueOf(buffer.get(3)));
    }

    client_channel.close();
    client_selector.close();
    server_channel.close();
    server_selector.close();
  }
}

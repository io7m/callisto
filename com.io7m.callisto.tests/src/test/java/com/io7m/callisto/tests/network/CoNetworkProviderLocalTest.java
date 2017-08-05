package com.io7m.callisto.tests.network;

import com.io7m.callisto.prototype0.network.CoNetworkPacketSocketType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderLocal;
import com.io7m.callisto.prototype0.network.CoNetworkProviderType;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CoNetworkProviderLocalTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoNetworkProviderLocalTest.class);

  @Test
  public void testCreatePacketSource()
    throws Exception
  {
    final CoNetworkProviderType provider = new CoNetworkProviderLocal();

    final Properties server_props = new Properties();
    server_props.setProperty("local_address", "::1");
    server_props.setProperty("local_port", "9999");

    final Properties client_props = new Properties();
    client_props.setProperty("remote_address", "::1");
    client_props.setProperty("remote_port", "9999");

    final AtomicBoolean called = new AtomicBoolean(false);
    final byte[] sent = "HELLO".getBytes(StandardCharsets.UTF_8);
    try (final CoNetworkPacketSocketType server =
           provider.createSocket(server_props)) {
      try (final CoNetworkPacketSocketType client =
             provider.createSocket(client_props)) {
        client.send(new InetSocketAddress("::1", 9999), ByteBuffer.wrap(sent));
        server.poll((address, buffer) -> {
          final byte[] data = new byte[buffer.limit()];
          buffer.get(data);
          called.set(true);
          Assert.assertArrayEquals(sent, data);
        });
        Assert.assertTrue(called.get());
      }
    }
  }
}

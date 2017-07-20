package com.io7m.callisto.tests.network;

import com.io7m.callisto.prototype0.network.CoNetworkPacketSinkType;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSourceType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderLocal;
import com.io7m.callisto.prototype0.network.CoNetworkProviderType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderUDP;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    final Properties props = new Properties();
    props.setProperty("local_address", "::1");
    props.setProperty("local_port", "9999");
    props.setProperty("remote_address", "::1");
    props.setProperty("remote_port", "9999");

    final AtomicBoolean called = new AtomicBoolean(false);
    final byte[] sent = "HELLO".getBytes(StandardCharsets.UTF_8);
    try (final CoNetworkPacketSourceType source =
           provider.createPacketSource(props)) {
      try (final CoNetworkPacketSinkType sink =
             provider.createPacketSink(props)) {
        sink.send(ByteBuffer.wrap(sent));
        source.poll((address, buffer) -> {
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

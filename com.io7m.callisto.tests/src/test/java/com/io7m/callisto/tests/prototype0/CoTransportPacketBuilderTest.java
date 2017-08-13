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

import com.io7m.callisto.prototype0.stringconstants.CoStringConstantReference;
import com.io7m.callisto.prototype0.transport.CoTransportPacketBuilder;
import com.io7m.callisto.prototype0.transport.CoTransportPacketBuilderListenerType;
import com.io7m.callisto.prototype0.transport.CoTransportSequenceNumberTracker;
import com.io7m.callisto.prototype0.transport.messages.CoDataReliable;
import com.io7m.callisto.prototype0.transport.messages.CoDataReliableFragment;
import com.io7m.callisto.prototype0.transport.messages.CoDataUnreliable;
import com.io7m.callisto.prototype0.transport.messages.CoPacket;
import com.io7m.callisto.tests.rules.PercentagePassRule;
import com.io7m.callisto.tests.rules.PercentagePassing;
import com.io7m.jserial.core.SerialNumber24;
import com.io7m.jserial.core.SerialNumberIntType;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;

public final class CoTransportPacketBuilderTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoTransportPacketBuilderTest.class);

  @Rule public PercentagePassRule percent =
    new PercentagePassRule(1000);

  /**
   * Build a pile of packets. Check that they're correctly sequenced and have
   * sizes within the specified bounds.
   */

  private static void testPacketBuildUnreliable(
    final int message_size,
    final int packet_size)
  {
    final CoTransportSequenceNumberTracker sequences =
      new CoTransportSequenceNumberTracker();
    final CoTransportPacketBuilder b =
      new CoTransportPacketBuilder(sequences, packet_size, 0, 0x696f376d);

    final Random random = new Random();
    final QueueListener listener = new QueueListener();

    int size = packet_size;
    final IntOpenHashSet messages_appended = new IntOpenHashSet();
    int index = 0;
    while (size > 0) {
      final int data_size = random.nextInt(message_size);
      final byte[] data = new byte[data_size];
      final ByteBuffer message = ByteBuffer.wrap(data);
      b.unreliableAppend(
        listener,
        CoStringConstantReference.of(index),
        message);
      messages_appended.add(index);
      ++index;
      size -= data_size;
    }

    b.unreliableFinishRemaining(listener);
    LOG.debug("queue size: {}", Integer.valueOf(listener.queue.size()));
    b.unreliableFinishRemaining(listener);
    LOG.debug("queue size: {}", Integer.valueOf(listener.queue.size()));

    final SerialNumberIntType serial = SerialNumber24.get();
    final IntOpenHashSet messages_received = new IntOpenHashSet();
    boolean first = true;
    int sequence_previous = 0;
    final Iterator<CoPacket> iter = listener.queue.iterator();
    while (iter.hasNext()) {
      final CoPacket p = iter.next();
      final CoDataUnreliable pu = p.getDataUnreliable();

      LOG.debug(
        "size:          {}",
        Integer.valueOf(p.getSerializedSize()));
      LOG.debug(
        "message count: {}",
        Integer.valueOf(pu.getMessagesCount()));
      LOG.debug(
        "id:            {}",
        Integer.valueOf(pu.getId().getSequence()));

      if (!first) {
        Assert.assertTrue(serial.compare(
          pu.getId().getSequence(),
          sequence_previous) > 0);
      }

      pu.getMessagesList().forEach(
        m -> messages_received.add(m.getMessageId()));

      Assert.assertThat(
        Integer.valueOf(p.getSerializedSize()),
        new PacketSizeIsWithinLimits(packet_size));

      sequence_previous = pu.getId().getSequence();
      first = false;
      iter.remove();
    }

    Assert.assertEquals(messages_appended, messages_received);
  }

  /**
   * Build a pile of packets. Check that they're correctly sequenced and have
   * sizes within the specified bounds.
   */

  private static void testPacketBuildReliable(
    final int message_size,
    final int packet_size)
  {
    final CoTransportSequenceNumberTracker sequences =
      new CoTransportSequenceNumberTracker();
    final CoTransportPacketBuilder b =
      new CoTransportPacketBuilder(sequences, packet_size, 0, 0x696f376d);

    final Random random = new Random();
    final QueueListener listener = new QueueListener();

    int size = packet_size;
    final IntOpenHashSet messages_appended = new IntOpenHashSet();
    int index = 0;
    while (size > 0) {
      final int data_size = random.nextInt(message_size);
      final byte[] data = new byte[data_size];
      final ByteBuffer message = ByteBuffer.wrap(data);
      b.reliableAppend(listener, CoStringConstantReference.of(index), message);
      messages_appended.add(index);
      ++index;
      size -= data_size;
    }

    b.reliableFinishRemaining(listener);
    LOG.debug("queue size: {}", Integer.valueOf(listener.queue.size()));
    b.reliableFinishRemaining(listener);
    LOG.debug("queue size: {}", Integer.valueOf(listener.queue.size()));

    final SerialNumberIntType serial = SerialNumber24.get();
    final IntOpenHashSet messages_received = new IntOpenHashSet();
    boolean first = true;
    int sequence_previous = 0;
    final Iterator<CoPacket> iter = listener.queue.iterator();
    while (iter.hasNext()) {
      final CoPacket p = iter.next();
      final CoDataReliable pr = p.getDataReliable();

      LOG.debug(
        "size:          {}",
        Integer.valueOf(p.getSerializedSize()));
      LOG.debug(
        "message count: {}",
        Integer.valueOf(pr.getMessagesCount()));

      if (!first) {
        Assert.assertTrue(serial.compare(
          pr.getId().getSequence(),
          sequence_previous) > 0);
      }

      Assert.assertThat(
        Integer.valueOf(p.getSerializedSize()),
        new PacketSizeIsWithinLimits(packet_size));

      pr.getMessagesList().forEach(
        m -> messages_received.add(m.getMessageId()));

      sequence_previous = pr.getId().getSequence();
      first = false;
      iter.remove();
    }

    Assert.assertEquals(messages_appended, messages_received);
  }

  @Test
  @PercentagePassing(passPercent = 100.0)
  public void testPacketBuildUnreliableSizesLargeMessages()
  {
    final int message_size = 400;
    final int packet_size = 1200;
    testPacketBuildUnreliable(message_size, packet_size);
  }

  @Test
  @PercentagePassing(passPercent = 100.0)
  public void testPacketBuildUnreliableSizesMediumMessages()
  {
    final int message_size = 100;
    final int packet_size = 1200;
    testPacketBuildUnreliable(message_size, packet_size);
  }

  @Test
  @PercentagePassing(passPercent = 100.0)
  public void testPacketBuildUnreliableSizesSmallMessages()
  {
    final int message_size = 20;
    final int packet_size = 1200;
    testPacketBuildUnreliable(message_size, packet_size);
  }

  @Test
  @PercentagePassing(passPercent = 100.0)
  public void testPacketBuildReliableSizesLargeMessages()
  {
    final int message_size = 400;
    final int packet_size = 1200;
    testPacketBuildReliable(message_size, packet_size);
  }

  @Test
  @PercentagePassing(passPercent = 100.0)
  public void testPacketBuildReliableSizesMediumMessages()
  {
    final int message_size = 100;
    final int packet_size = 1200;
    testPacketBuildReliable(message_size, packet_size);
  }

  @Test
  @PercentagePassing(passPercent = 100.0)
  public void testPacketBuildReliableSizesSmallMessages()
  {
    final int message_size = 20;
    final int packet_size = 1200;
    testPacketBuildReliable(message_size, packet_size);
  }

  @Test
  public void testPacketFragmentationReliable()
    throws Exception
  {
    final CoTransportSequenceNumberTracker sequences =
      new CoTransportSequenceNumberTracker();
    final CoTransportPacketBuilder b =
      new CoTransportPacketBuilder(sequences, 1200, 0, 0x696f376d);

    final Random random = new Random();
    final QueueListener listener = new QueueListener();

    final byte[] message_data = new byte[5000];
    random.nextBytes(message_data);
    final ByteBuffer message = ByteBuffer.wrap(message_data);

    Assert.assertEquals(0L, (long) listener.queue.size());
    b.reliableAppend(listener, CoStringConstantReference.of(23), message);
    Assert.assertEquals(5L, (long) listener.queue.size());
    b.reliableFinishRemaining(listener);
    Assert.assertEquals(5L, (long) listener.queue.size());

    LOG.debug("queue size: {}", Integer.valueOf(listener.queue.size()));

    try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      int index = 1;
      while (!listener.queue.isEmpty()) {
        final CoPacket p = listener.queue.remove();
        final CoDataReliableFragment pr = p.getDataReliableFragment();
        Assert.assertEquals(5L, (long) pr.getFragmentCount());
        Assert.assertEquals((long) index, (long) pr.getFragmentIndex());
        Assert.assertEquals(0L, (long) pr.getMessageId());
        out.write(pr.getMessageData().toByteArray());
        ++index;
      }

      Assert.assertArrayEquals(message_data, out.toByteArray());
    }
  }

  @Test
  public void testPacketFragmentationUnreliable()
    throws Exception
  {
    final CoTransportSequenceNumberTracker sequences =
      new CoTransportSequenceNumberTracker();
    final CoTransportPacketBuilder b =
      new CoTransportPacketBuilder(sequences, 1200, 0, 0x696f376d);

    final Random random = new Random();
    final QueueListener listener = new QueueListener();

    final byte[] message_data = new byte[5000];
    random.nextBytes(message_data);
    final ByteBuffer message = ByteBuffer.wrap(message_data);

    Assert.assertEquals(0L, (long) listener.queue.size());
    b.unreliableAppend(listener, CoStringConstantReference.of(23), message);
    Assert.assertEquals(5L, (long) listener.queue.size());
    b.unreliableFinishRemaining(listener);
    Assert.assertEquals(5L, (long) listener.queue.size());

    LOG.debug("queue size: {}", Integer.valueOf(listener.queue.size()));

    try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      int index = 1;
      while (!listener.queue.isEmpty()) {
        final CoPacket p = listener.queue.remove();
        final CoDataReliableFragment pr = p.getDataReliableFragment();
        Assert.assertEquals(5L, (long) pr.getFragmentCount());
        Assert.assertEquals((long) index, (long) pr.getFragmentIndex());
        Assert.assertEquals(0L, (long) pr.getMessageId());
        out.write(pr.getMessageData().toByteArray());
        ++index;
      }

      Assert.assertArrayEquals(message_data, out.toByteArray());
    }
  }

  @Test
  public void testPacketBuildReceiptMisc()
  {
    final CoTransportSequenceNumberTracker sequences =
      new CoTransportSequenceNumberTracker();
    final CoTransportPacketBuilder b =
      new CoTransportPacketBuilder(sequences, 1200, 0, 0x696f376d);

    sequences.reliableReceiverWindow().receive(1);
    sequences.reliableReceiverWindow().receive(2);
    sequences.reliableReceiverWindow().receive(3);

    final QueueListener listener = new QueueListener();
    b.acks(listener);

    listener.queue.forEach(p -> LOG.trace("packet: {}", p));
  }

  private static final class QueueListener
    implements CoTransportPacketBuilderListenerType
  {
    private final Queue<CoPacket> queue = new ArrayDeque<>();

    QueueListener()
    {

    }

    @Override
    public void onCreatedPacketReliable(
      final CoPacket p)
    {
      this.queue.add(p);
    }

    @Override
    public void onCreatedPacketUnreliable(
      final CoPacket p)
    {
      this.queue.add(p);
    }

    @Override
    public void onCreatedPacketReliableFragment(
      final CoPacket p)
    {
      this.queue.add(p);
    }

    @Override
    public void onCreatedPacketAck(
      final CoPacket p)
    {
      this.queue.add(p);
    }
  }

  private static final class PacketSizeIsWithinLimits
    extends TypeSafeMatcher<Integer>
  {
    private final int size;

    PacketSizeIsWithinLimits(
      final int in_size)
    {
      this.size = in_size;
    }

    @Override
    protected boolean matchesSafely(
      final Integer x_size)
    {
      return x_size.intValue() <= this.size;
    }

    @Override
    public void describeTo(final Description description)
    {
      description.appendText("Size must be < " + this.size);
    }
  }
}

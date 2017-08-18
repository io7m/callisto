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

import com.io7m.callisto.prototype0.transport.CoTransportReceiverFragmentTracker;
import com.io7m.callisto.prototype0.transport.CoTransportReceiverFragmentTrackerListenerType;
import it.unimi.dsi.fastutil.ints.IntSet;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

import static com.io7m.callisto.prototype0.transport.CoTransportReceiverFragmentTrackerListenerType.Error;
import static com.io7m.callisto.prototype0.transport.CoTransportReceiverFragmentTrackerListenerType.FragmentKeep;

public final class CoTransportReceiverFragmentTrackerTest
{
  private static int prime()
  {
    return BigInteger.probablePrime(32, new SecureRandom()).intValue();
  }

  @Test
  public void testReady(
    final @Mocked CoTransportReceiverFragmentTrackerListenerType listener)
  {
    final CoTransportReceiverFragmentTracker fr =
      new CoTransportReceiverFragmentTracker();

    final byte[] data0 = {
      (byte) 'a', (byte) 'a', (byte) 'a', (byte) 'a'
    };
    final byte[] data1 = {
      (byte) 'b', (byte) 'b', (byte) 'b', (byte) 'b'
    };
    final byte[] data2 = {
      (byte) 'c', (byte) 'c', (byte) 'c', (byte) 'c'
    };
    final byte[] all_data_a = {
      (byte) 'a', (byte) 'a', (byte) 'a', (byte) 'a',
      (byte) 'b', (byte) 'b', (byte) 'b', (byte) 'b',
      (byte) 'c', (byte) 'c', (byte) 'c', (byte) 'c'
    };
    final ByteBuffer bdata0 = ByteBuffer.wrap(data0);
    final ByteBuffer bdata1 = ByteBuffer.wrap(data1);
    final ByteBuffer bdata2 = ByteBuffer.wrap(data2);
    final ByteBuffer all_data = ByteBuffer.wrap(all_data_a);

    final int id = prime();

    new StrictExpectations()
    {{
      listener.onFragmentStart(id, 3, 0xc4f4d5e1, 0, bdata0);
      listener.onFragmentSegment(id, 1, bdata1);
      listener.onFragmentSegment(id, 2, bdata2);

      // On segment
      listener.onFragmentReady(
        0, (IntSet) this.any, id, 0xc4f4d5e1, 0, all_data);
      this.result = FragmentKeep.KEEP;

      // Tick
      listener.onFragmentReady(
        0, (IntSet) this.any, id, 0xc4f4d5e1, 0, all_data);
      this.result = FragmentKeep.KEEP;

      // Tick
      listener.onFragmentReady(
        0, (IntSet) this.any, id, 0xc4f4d5e1, 0, all_data);
      this.result = FragmentKeep.DISCARD;

      listener.onFragmentDiscarded(id);
      // Tick
    }};

    fr.receiveInitial(listener, 0, id, 3, 0, 0xc4f4d5e1, 12, bdata0);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.receiveSegment(listener, 1, id, 1, bdata1);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.receiveSegment(listener, 2, id, 2, bdata2);
    Assert.assertEquals(0L, (long) fr.incomplete().size());
    Assert.assertEquals(1L, (long) fr.completed().size());
    fr.poll(listener);
    Assert.assertEquals(0L, (long) fr.incomplete().size());
    Assert.assertEquals(1L, (long) fr.completed().size());
    fr.poll(listener);
    Assert.assertEquals(0L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
  }

  @Test
  public void testReadyDiscarded(
    final @Mocked CoTransportReceiverFragmentTrackerListenerType listener)
  {
    final CoTransportReceiverFragmentTracker fr =
      new CoTransportReceiverFragmentTracker();

    final byte[] data0 = {
      (byte) 'a', (byte) 'a', (byte) 'a', (byte) 'a'
    };
    final byte[] data1 = {
      (byte) 'b', (byte) 'b', (byte) 'b', (byte) 'b'
    };
    final byte[] data2 = {
      (byte) 'c', (byte) 'c', (byte) 'c', (byte) 'c'
    };
    final byte[] all_data_a = {
      (byte) 'a', (byte) 'a', (byte) 'a', (byte) 'a',
      (byte) 'b', (byte) 'b', (byte) 'b', (byte) 'b',
      (byte) 'c', (byte) 'c', (byte) 'c', (byte) 'c'
    };
    final ByteBuffer bdata0 = ByteBuffer.wrap(data0);
    final ByteBuffer bdata1 = ByteBuffer.wrap(data1);
    final ByteBuffer bdata2 = ByteBuffer.wrap(data2);
    final ByteBuffer all_data = ByteBuffer.wrap(all_data_a);

    final int id = prime();

    new StrictExpectations()
    {{
      listener.onFragmentStart(id, 3, 0xc4f4d5e1, 0, bdata0);
      listener.onFragmentSegment(id, 1, bdata1);
      listener.onFragmentSegment(id, 2, bdata2);
      listener.onFragmentReady(0, (IntSet) this.any, id, 0xc4f4d5e1, 0, all_data);
      this.result = FragmentKeep.DISCARD;
      listener.onFragmentDiscarded(id);
    }};

    fr.receiveInitial(listener, 0, id, 3, 0, 0xc4f4d5e1, 12, bdata0);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.receiveSegment(listener, 1, id, 1, bdata1);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.receiveSegment(listener, 2, id, 2, bdata2);
    Assert.assertEquals(0L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.poll(listener);
    Assert.assertEquals(0L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.poll(listener);
    Assert.assertEquals(0L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
  }

  @Test
  public void testNotReady(
    final @Mocked CoTransportReceiverFragmentTrackerListenerType listener)
  {
    final CoTransportReceiverFragmentTracker fr =
      new CoTransportReceiverFragmentTracker();

    final byte[] data0 = {
      (byte) 'a', (byte) 'a', (byte) 'a', (byte) 'a'
    };
    final byte[] data1 = {
      (byte) 'b', (byte) 'b', (byte) 'b', (byte) 'b'
    };
    final ByteBuffer bdata0 = ByteBuffer.wrap(data0);
    final ByteBuffer bdata1 = ByteBuffer.wrap(data1);

    final int id = prime();

    new StrictExpectations()
    {{
      listener.onFragmentStart(id, 3, 0xc4f4d5e1, 0, bdata0);
      listener.onFragmentSegment(id, 1, bdata1);
      listener.onFragmentNotReady(id);
    }};

    fr.receiveInitial(listener, 0, id, 3, 0, 0xc4f4d5e1, 8, bdata0);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    Assert.assertTrue(fr.incompleteBySequence().containsKey(0));
    Assert.assertEquals(1L, (long) fr.incompleteBySequence().size());

    fr.receiveSegment(listener, 1, id, 1, bdata1);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    Assert.assertTrue(fr.incompleteBySequence().containsKey(0));
    Assert.assertTrue(fr.incompleteBySequence().containsKey(1));
    Assert.assertEquals(2L, (long) fr.incompleteBySequence().size());

    fr.poll(listener);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    Assert.assertTrue(fr.incompleteBySequence().containsKey(0));
    Assert.assertTrue(fr.incompleteBySequence().containsKey(1));
    Assert.assertEquals(2L, (long) fr.incompleteBySequence().size());
  }

  @Test
  public void testNonexistent(
    final @Mocked CoTransportReceiverFragmentTrackerListenerType listener)
  {
    final CoTransportReceiverFragmentTracker fr =
      new CoTransportReceiverFragmentTracker();

    final byte[] data1 = {
      (byte) 'b', (byte) 'b', (byte) 'b', (byte) 'b'
    };
    final ByteBuffer bdata1 = ByteBuffer.wrap(data1);

    final int id = prime();

    new StrictExpectations()
    {{
      listener.onFragmentError(
        id,
        Error.FRAGMENT_ERROR_NONEXISTENT,
        this.anyString);
    }};

    fr.receiveSegment(listener, 0, id, 1, bdata1);
    Assert.assertEquals(0L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
  }

  @Test
  public void testViewNonexistent(
    final @Mocked CoTransportReceiverFragmentTrackerListenerType listener)
  {
    final CoTransportReceiverFragmentTracker fr =
      new CoTransportReceiverFragmentTracker();

    final int id = prime();
    new StrictExpectations()
    {{
      listener.onFragmentError(
        id,
        Error.FRAGMENT_ERROR_NONEXISTENT,
        this.anyString);
    }};

    fr.viewCompletedById(listener, id);
  }

  @Test
  public void testAlreadyStarted(
    final @Mocked CoTransportReceiverFragmentTrackerListenerType listener)
  {
    final CoTransportReceiverFragmentTracker fr =
      new CoTransportReceiverFragmentTracker();

    final byte[] data1 = {
      (byte) 'b', (byte) 'b', (byte) 'b', (byte) 'b'
    };
    final ByteBuffer bdata1 = ByteBuffer.wrap(data1);

    final int id = prime();

    new StrictExpectations()
    {{
      listener.onFragmentStart(id, 3, 0xc4f4d5e1, 0, bdata1);
      listener.onFragmentError(
        id,
        Error.FRAGMENT_ERROR_ALREADY_STARTED,
        this.anyString);
    }};

    fr.receiveInitial(listener, 0, id, 3, 0, 0xc4f4d5e1, 8, bdata1);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.receiveInitial(listener, 1, id, 3, 0, 0xc4f4d5e1, 8, bdata1);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
  }

  @Test
  public void testSegmentAlreadyProvided(
    final @Mocked CoTransportReceiverFragmentTrackerListenerType listener)
  {
    final CoTransportReceiverFragmentTracker fr =
      new CoTransportReceiverFragmentTracker();

    final byte[] data0 = {
      (byte) 'a', (byte) 'a', (byte) 'a', (byte) 'a'
    };
    final byte[] data1 = {
      (byte) 'b', (byte) 'b', (byte) 'b', (byte) 'b'
    };
    final ByteBuffer bdata0 = ByteBuffer.wrap(data0);
    final ByteBuffer bdata1 = ByteBuffer.wrap(data1);

    final int id = prime();

    new StrictExpectations()
    {{
      listener.onFragmentStart(id, 3, 0xc4f4d5e1, 0, bdata0);
      listener.onFragmentSegment(id, 1, bdata1);
      listener.onFragmentError(
        id,
        Error.FRAGMENT_ERROR_SEGMENT_ALREADY_PROVIDED,
        this.anyString);
    }};

    fr.receiveInitial(listener, 0, id, 3, 0, 0xc4f4d5e1, 8, bdata0);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.receiveSegment(listener, 1, id, 1, bdata1);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.receiveSegment(listener, 2, id, 1, bdata1);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
  }

  @Test
  public void testShortData(
    final @Mocked CoTransportReceiverFragmentTrackerListenerType listener)
  {
    final CoTransportReceiverFragmentTracker fr =
      new CoTransportReceiverFragmentTracker();

    final byte[] data0 = {
      (byte) 'a', (byte) 'a'
    };
    final byte[] data1 = {
      (byte) 'b', (byte) 'b'
    };
    final byte[] data2 = {
      (byte) 'c', (byte) 'c'
    };
    final ByteBuffer bdata0 = ByteBuffer.wrap(data0);
    final ByteBuffer bdata1 = ByteBuffer.wrap(data1);
    final ByteBuffer bdata2 = ByteBuffer.wrap(data2);

    final int id = prime();

    new StrictExpectations()
    {{
      listener.onFragmentStart(id, 3, 0xc4f4d5e1, 0, bdata0);
      listener.onFragmentSegment(id, 1, bdata1);
      listener.onFragmentSegment(id, 2, bdata2);
      listener.onFragmentError(
        id, Error.FRAGMENT_ERROR_SIZE_MISMATCH, this.anyString);
    }};

    fr.receiveInitial(listener, 0, id, 3, 0, 0xc4f4d5e1, 12, bdata0);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.receiveSegment(listener, 1, id, 1, bdata1);
    Assert.assertEquals(1L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
    fr.receiveSegment(listener, 2, id, 2, bdata2);
    Assert.assertEquals(0L, (long) fr.incomplete().size());
    Assert.assertEquals(0L, (long) fr.completed().size());
  }
}

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

import com.io7m.callisto.prototype0.transport.CoTransportReliableReceiverWindow;
import com.io7m.jserial.core.SerialNumber24;
import com.io7m.jserial.core.SerialNumber8;
import com.io7m.junreachable.UnimplementedCodeException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.stream.IntStream;

public final class CoTransportReliableReceiverWindowTest
{
  @Rule public final ExpectedException expected = ExpectedException.none();

  @Test
  public void testBasicReceiveDistance()
  {
    final CoTransportReliableReceiverWindow win =
      new CoTransportReliableReceiverWindow(SerialNumber24.get(), 0, 160);

    final int[] args = {0, 8388607};
    this.expected.expect(UnimplementedCodeException.class);
    IntStream.of(args).forEach(win::receive);
  }

  @Test
  public void testBasicReceive_0()
  {
    final CoTransportReliableReceiverWindow win =
      new CoTransportReliableReceiverWindow(SerialNumber8.get(), 0, 10);

    final int[] args = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    IntStream.of(args).forEach(win::receive);

    Assert.assertEquals(0L, (long) win.missed().size());
    Assert.assertEquals(10L, (long) win.received().size());
    Assert.assertEquals(9L, (long) win.receivedBeforeMissing());
    IntStream.of(args).forEach(r -> Assert.assertTrue(win.received().contains(r)));

    win.reset();
    Assert.assertEquals(0L, (long) win.missed().size());
    Assert.assertEquals(0L, (long) win.received().size());
    Assert.assertEquals(9L, (long) win.receivedBeforeMissing());
  }

  @Test
  public void testBasicReceive_253()
  {
    final CoTransportReliableReceiverWindow win =
      new CoTransportReliableReceiverWindow(SerialNumber8.get(), 253, 10);

    final int[] args = {253, 254, 255, 0, 1, 2};
    IntStream.of(args).forEach(win::receive);

    Assert.assertEquals(0L, (long) win.missed().size());
    Assert.assertEquals(6L, (long) win.received().size());
    Assert.assertEquals(2L, (long) win.receivedBeforeMissing());
    IntStream.of(args).forEach(r -> Assert.assertTrue(win.received().contains(r)));

    win.reset();
    Assert.assertEquals(0L, (long) win.missed().size());
    Assert.assertEquals(0L, (long) win.received().size());
    Assert.assertEquals(2L, (long) win.receivedBeforeMissing());
  }

  @Test
  public void testBasicReceiveMissing_0()
  {
    final CoTransportReliableReceiverWindow win =
      new CoTransportReliableReceiverWindow(SerialNumber8.get(), 0, 10);

    final int[] args = {0, 1, 3, 4};
    IntStream.of(args).forEach(win::receive);

    Assert.assertEquals(1L, (long) win.missed().size());
    Assert.assertTrue(win.missed().contains(2));
    Assert.assertEquals(4L, (long) win.received().size());
    Assert.assertEquals(1L, (long) win.receivedBeforeMissing());
    IntStream.of(args).forEach(r -> Assert.assertTrue(win.received().contains(r)));

    win.reset();
    Assert.assertEquals(0L, (long) win.missed().size());
    Assert.assertEquals(0L, (long) win.received().size());
    Assert.assertEquals(4L, (long) win.receivedBeforeMissing());
  }

  @Test
  public void testBasicReceiveMissing_253()
  {
    final CoTransportReliableReceiverWindow win =
      new CoTransportReliableReceiverWindow(SerialNumber8.get(), 253, 10);

    final int[] args = {253, 254, 0, 1};
    IntStream.of(args).forEach(win::receive);

    Assert.assertEquals(1L, (long) win.missed().size());
    Assert.assertTrue(win.missed().contains(255));
    Assert.assertEquals(4L, (long) win.received().size());
    Assert.assertEquals(254L, (long) win.receivedBeforeMissing());
    IntStream.of(args).forEach(r -> Assert.assertTrue(win.received().contains(r)));

    win.reset();
    Assert.assertEquals(0L, (long) win.missed().size());
    Assert.assertEquals(0L, (long) win.received().size());
    Assert.assertEquals(1L, (long) win.receivedBeforeMissing());
  }
}

package com.io7m.callisto.tests.prototype0;

import com.io7m.callisto.prototype0.transport.pipeline.CoPipelineReliableSequenceTrackerWindow;
import com.io7m.jserial.core.SerialNumber24;
import com.io7m.jserial.core.SerialNumberIntType;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.junit.Assert;
import org.junit.Test;

public final class CoPipelineReliableSequenceTrackerWindowTest
{
  @Test
  public void testWindowDropOld(
    final @Mocked CoPipelineReliableSequenceTrackerWindow.RangeConsumerType consumer)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoPipelineReliableSequenceTrackerWindow w =
      new CoPipelineReliableSequenceTrackerWindow(serial, 100);

    new StrictExpectations()
    {{

    }};

    final boolean r = w.receive(99);
    Assert.assertFalse(r);
  }

  @Test
  public void testWindowMissing100_200(
    final @Mocked CoPipelineReliableSequenceTrackerWindow.RangeConsumerType consumer)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoPipelineReliableSequenceTrackerWindow w =
      new CoPipelineReliableSequenceTrackerWindow(serial, 0);

    new StrictExpectations()
    {{
      consumer.receive(0, 99);
      consumer.receive(0, 99);
      consumer.receive(101, 199);
    }};

    w.missed(consumer);

    boolean r = w.receive(100);
    Assert.assertTrue(r);
    w.missed(consumer);

    r = w.receive(200);
    Assert.assertTrue(r);
    w.missed(consumer);
  }

  @Test
  public void testWindowStraightforward(
    final @Mocked CoPipelineReliableSequenceTrackerWindow.RangeConsumerType consumer)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoPipelineReliableSequenceTrackerWindow w =
      new CoPipelineReliableSequenceTrackerWindow(serial, 0);

    new StrictExpectations()
    {{

    }};

    boolean r = w.receive(0);
    Assert.assertTrue(r);
    Assert.assertEquals(1L, (long) w.sequenceLow());

    r = w.receive(1);
    Assert.assertTrue(r);
    Assert.assertEquals(2L, (long) w.sequenceLow());

    r = w.receive(2);
    Assert.assertTrue(r);
    Assert.assertEquals(3L, (long) w.sequenceLow());

    w.missed(consumer);
  }

  @Test
  public void testWindowLargestGap0_7fffff(
    final @Mocked CoPipelineReliableSequenceTrackerWindow.RangeConsumerType consumer)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoPipelineReliableSequenceTrackerWindow w =
      new CoPipelineReliableSequenceTrackerWindow(serial, 0);

    new StrictExpectations()
    {{
      consumer.receive(0, 0x7ffffe);
    }};

    final boolean r = w.receive(0x7fffff);
    Assert.assertTrue(r);

    w.missed(consumer);
  }
}

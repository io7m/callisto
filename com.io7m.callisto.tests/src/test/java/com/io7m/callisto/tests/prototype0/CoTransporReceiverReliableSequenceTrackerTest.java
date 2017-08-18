package com.io7m.callisto.tests.prototype0;

import com.io7m.callisto.prototype0.transport.CoTransportReceiverReliableSequenceTracker;
import com.io7m.callisto.prototype0.transport.CoTransportReceiverReliableSequenceTrackerListenerType;
import com.io7m.callisto.prototype0.transport.messages.CoDataReliable;
import com.io7m.callisto.prototype0.transport.messages.CoDataReliableFragmentInitial;
import com.io7m.callisto.prototype0.transport.messages.CoDataReliableFragmentSegment;
import com.io7m.callisto.prototype0.transport.messages.CoPacket;
import com.io7m.callisto.prototype0.transport.messages.CoPacketID;
import com.io7m.jserial.core.SerialNumber24;
import com.io7m.jserial.core.SerialNumberIntType;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static com.io7m.callisto.prototype0.transport.CoTransportReceiverReliableSequenceTrackerListenerType.Error;
import static com.io7m.callisto.prototype0.transport.CoTransportReceiverReliableSequenceTrackerListenerType.Keep;

public final class CoTransporReceiverReliableSequenceTrackerTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoTransporReceiverReliableSequenceTrackerTest.class);

  private static CoPacket reliable(
    final int sequence)
  {
    return CoPacket.newBuilder()
      .setDataReliable(
        CoDataReliable.newBuilder()
          .setId(CoPacketID.newBuilder()
                   .setChannel(0)
                   .setConnectionId(0)
                   .setSequence(sequence)
                   .build())
          .build())
      .build();
  }

  private static CoPacket fragmentInitial(
    final int sequence,
    final int frag_id,
    final int frag_count)
  {
    return CoPacket.newBuilder()
      .setDataReliableFragmentInitial(
        CoDataReliableFragmentInitial.newBuilder()
          .setId(CoPacketID.newBuilder()
                   .setSequence(sequence)
                   .setConnectionId(0)
                   .setChannel(0)
                   .build())
          .setFragmentId(frag_id)
          .setFragmentCount(frag_count)
          .setMessageId(0x8a3cd409)
          .setMessageType(0xab734e49)
          .build())
      .build();
  }

  private static CoPacket fragmentSegment(
    final int sequence,
    final int frag_id,
    final int frag_index)
  {
    return CoPacket.newBuilder()
      .setDataReliableFragmentSegment(
        CoDataReliableFragmentSegment.newBuilder()
          .setId(CoPacketID.newBuilder()
                   .setSequence(sequence)
                   .setConnectionId(0)
                   .setChannel(0)
                   .build())
          .setFragmentId(frag_id)
          .setFragmentIndex(frag_index)
          .build())
      .build();
  }

  @Test
  public void testDeliver(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, reliable(0));
      listener.onReceivePacketArrived(1, reliable(1));
      listener.onReceivePacketArrived(2, reliable(2));
      listener.onReceivePacketReadyForDelivery(0, reliable(0));
      listener.onReceivePacketReadyForDelivery(1, reliable(1));
      listener.onReceivePacketReadyForDelivery(2, reliable(2));
    }};

    r.receive(listener, reliable(0));
    r.receive(listener, reliable(1));
    r.receive(listener, reliable(2));
    r.poll(listener);
  }

  @Test
  public void testDeliverFragment(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, fragmentInitial(0, 0xc5fc92f3, 3));
      listener.onReceivePacketArrived(1, fragmentSegment(1, 0xc5fc92f3, 1));
      listener.onReceivePacketArrived(2, fragmentSegment(2, 0xc5fc92f3, 2));

      listener.onReceivePacketReassembledFragmentReadyForDelivery(
        0, 0xc5fc92f3, 0x8a3cd409, 0xab734e49, (ByteBuffer) this.any);
      this.result = Keep.KEEP;

      listener.onReceivePacketReassembledFragmentReadyForDelivery(
        0, 0xc5fc92f3, 0x8a3cd409, 0xab734e49, (ByteBuffer) this.any);
      this.result = Keep.KEEP;

      listener.onReceivePacketReassembledFragmentReadyForDelivery(
        0, 0xc5fc92f3, 0x8a3cd409, 0xab734e49, (ByteBuffer) this.any);
      this.result = Keep.DISCARD;

      listener.onReceivePacketDiscarded(0, fragmentInitial(0, 0xc5fc92f3, 3));
    }};

    r.receive(listener, fragmentInitial(0, 0xc5fc92f3, 3));
    r.receive(listener, fragmentSegment(1, 0xc5fc92f3, 1));
    r.receive(listener, fragmentSegment(2, 0xc5fc92f3, 2));
    r.poll(listener);
    r.poll(listener);
    r.poll(listener);
    r.poll(listener);
  }

  @Test
  public void testFragmentStallingDelivery(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, fragmentInitial(0, 0xf7b7678d, 3));
      listener.onReceivePacketArrived(1, reliable(1));
      listener.onReceivePacketFragmentNotReady(0, 0xf7b7678d);
    }};

    r.receive(listener, fragmentInitial(0, 0xf7b7678d, 3));
    r.receive(listener, reliable(1));
    r.poll(listener);
  }

  @Test
  public void testNothingReceived(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{

    }};

    r.poll(listener);
  }

  @Test
  public void testGapTiny(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, reliable(0));
      listener.onReceivePacketArrived(2, reliable(2));

      listener.onReceivePacketRangeMissing(1, 1);
      listener.onReceivePacketReadyForDelivery(0, reliable(0));
    }};

    r.receive(listener, reliable(0));
    r.receive(listener, reliable(2));
    r.poll(listener);
  }

  @Test
  public void testGapSimple(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, reliable(0));
      listener.onReceivePacketArrived(5, reliable(5));
      listener.onReceivePacketArrived(6, reliable(6));
      listener.onReceivePacketArrived(8, reliable(8));
      listener.onReceivePacketArrived(9, reliable(9));
      listener.onReceivePacketArrived(15, reliable(15));
      listener.onReceivePacketArrived(100000, reliable(100000));

      listener.onReceivePacketRangeMissing(1, 4);
      listener.onReceivePacketRangeMissing(7, 7);
      listener.onReceivePacketRangeMissing(10, 14);
      listener.onReceivePacketRangeMissing(16, 99999);
      listener.onReceivePacketReadyForDelivery(0, reliable(0));
    }};

    r.receive(listener, reliable(0));
    r.receive(listener, reliable(5));
    r.receive(listener, reliable(6));
    r.receive(listener, reliable(8));
    r.receive(listener, reliable(9));
    r.receive(listener, reliable(15));
    r.receive(listener, reliable(100000));
    r.poll(listener);
  }

  @Test
  public void testGapLargeMissingMultiple(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, reliable(0));
      listener.onReceivePacketArrived(5, reliable(5));
      listener.onReceivePacketArrived(6, reliable(6));
      listener.onReceivePacketArrived(8, reliable(8));
      listener.onReceivePacketArrived(9, reliable(9));
      listener.onReceivePacketArrived(15, reliable(15));
      listener.onReceivePacketArrived(100000, reliable(100000));

      listener.onReceivePacketRangeMissing(1, 4);
      listener.onReceivePacketRangeMissing(7, 7);
      listener.onReceivePacketRangeMissing(10, 14);
      listener.onReceivePacketRangeMissing(16, 99999);
      listener.onReceivePacketReadyForDelivery(0, reliable(0));
    }};

    r.receive(listener, reliable(0));
    r.receive(listener, reliable(5));
    r.receive(listener, reliable(6));
    r.receive(listener, reliable(8));
    r.receive(listener, reliable(9));
    r.receive(listener, reliable(15));
    r.receive(listener, reliable(100000));
    r.poll(listener);
  }

  @Test
  public void testGapLargest(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, reliable(0));
      listener.onReceivePacketArrived(0x7fffff, reliable(0x7fffff));
      listener.onReceivePacketRangeMissing(1, 0x7ffffe);
      listener.onReceivePacketReadyForDelivery(0, reliable(0));
    }};

    r.receive(listener, reliable(0));
    r.receive(listener, reliable(0x7fffff));
    r.poll(listener);
  }

  @Test
  public void testReordered(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(2, reliable(2));
      listener.onReceivePacketArrived(1, reliable(1));
      listener.onReceivePacketArrived(0, reliable(0));
      listener.onReceivePacketReadyForDelivery(0, reliable(0));
      listener.onReceivePacketReadyForDelivery(1, reliable(1));
      listener.onReceivePacketReadyForDelivery(2, reliable(2));
    }};

    r.receive(listener, reliable(2));
    r.receive(listener, reliable(1));
    r.receive(listener, reliable(0));
    r.poll(listener);
  }

  @Test
  public void testDeliveryTooOldReliable(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 1);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(1, reliable(1));
      listener.onReceivePacketArrived(2, reliable(2));
      listener.onReceivePacketArrived(3, reliable(3));

      listener.onReceivePacketReadyForDelivery(1, reliable(1));
      this.result = Keep.DISCARD;
      listener.onReceivePacketDiscarded(1, reliable(1));

      listener.onReceivePacketReadyForDelivery(2, reliable(2));
      this.result = Keep.DISCARD;
      listener.onReceivePacketDiscarded(2, reliable(2));

      listener.onReceivePacketReadyForDelivery(3, reliable(3));
      this.result = Keep.DISCARD;
      listener.onReceivePacketDiscarded(3, reliable(3));

      listener.onReceiveError(0, Error.RECEIVER_TOO_OLD, this.anyString);
    }};

    r.receive(listener, reliable(1));
    r.receive(listener, reliable(2));
    r.receive(listener, reliable(3));
    r.poll(listener);
    r.receive(listener, reliable(0));
    r.poll(listener);
  }

  @Test
  public void testDeliveryTooOldFragmentInitial(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 1);

    new StrictExpectations()
    {{
      listener.onReceiveError(0, Error.RECEIVER_TOO_OLD, "Packet 0 is too old");
    }};

    r.receive(listener, fragmentInitial(0, 0xde52c8cd, 2));
    r.poll(listener);
  }

  @Test
  public void testDeliveryTooOldFragmentSegment(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 1);

    new StrictExpectations()
    {{
      listener.onReceiveError(0, Error.RECEIVER_TOO_OLD, "Packet 0 is too old");
    }};

    r.receive(listener, fragmentSegment(0, 0xde52c8cd, 1));
    r.poll(listener);
  }

  @Test
  public void testDuplicateReliable(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, reliable(0));
      listener.onReceiveError(0, Error.RECEIVER_DUPLICATE, this.anyString);
      listener.onReceiveError(0, Error.RECEIVER_DUPLICATE, this.anyString);
      listener.onReceivePacketReadyForDelivery(0, reliable(0));
    }};

    r.receive(listener, reliable(0));
    r.receive(listener, reliable(0));
    r.receive(listener, reliable(0));
    r.poll(listener);
  }

  @Test
  public void testDuplicateFragmentInitial(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, fragmentInitial(0, 0xa57ddfb9, 3));
      listener.onReceiveError(0, Error.RECEIVER_DUPLICATE, this.anyString);
    }};

    r.receive(listener, fragmentInitial(0, 0xa57ddfb9, 3));
    r.receive(listener, fragmentInitial(0, 0xc852f3bf, 3));
  }

  @Test
  public void testDuplicateFragmentSegment(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, fragmentInitial(0, 0xa57ddfb9, 3));
      listener.onReceiveError(0, Error.RECEIVER_DUPLICATE, this.anyString);
    }};

    r.receive(listener, fragmentInitial(0, 0xa57ddfb9, 3));
    r.receive(listener, fragmentSegment(0, 0xc852f3bf, 1));
  }

  @Test
  public void testDuplicateFragmentInitialID(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, fragmentInitial(0, 0xa57ddfb9, 3));
      listener.onReceiveError(1, Error.RECEIVER_FRAGMENT_PROBLEM, this.anyString);
    }};

    r.receive(listener, fragmentInitial(0, 0xa57ddfb9, 3));
    r.receive(listener, fragmentInitial(1, 0xa57ddfb9, 3));
  }

  @Test
  public void testDuplicateFragmentSegmentID(
    final @Mocked CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final SerialNumberIntType serial = SerialNumber24.get();
    final CoTransportReceiverReliableSequenceTracker r =
      new CoTransportReceiverReliableSequenceTracker(serial, 0);

    new StrictExpectations()
    {{
      listener.onReceivePacketArrived(0, fragmentInitial(0, 0xa57ddfb9, 3));
      listener.onReceiveError(1, Error.RECEIVER_FRAGMENT_PROBLEM, this.anyString);
    }};

    r.receive(listener, fragmentInitial(0, 0xa57ddfb9, 3));
    r.receive(listener, fragmentSegment(1, 0xc852f3bf, 1));
  }
}

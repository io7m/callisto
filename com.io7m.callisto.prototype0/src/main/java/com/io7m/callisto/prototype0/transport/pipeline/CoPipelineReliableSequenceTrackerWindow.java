package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.jnull.NullCheck;
import com.io7m.jserial.core.SerialNumberIntType;

import java.util.BitSet;

public final class CoPipelineReliableSequenceTrackerWindow
{
  private int sequence_lo;
  private BitSet received;
  private final SerialNumberIntType serial;

  public CoPipelineReliableSequenceTrackerWindow(
    final SerialNumberIntType in_serial,
    final int sequence_initial)
  {
    this.serial = NullCheck.notNull(in_serial, "Serial");
    this.sequence_lo = sequence_initial;
    this.received = new BitSet(64);
  }

  public int sequenceLow()
  {
    return this.sequence_lo;
  }

  public boolean receive(
    final int sequence_incoming)
  {
    final int distance =
      this.serial.distance(this.sequence_lo, sequence_incoming);

    if (distance < 0) {
      return false;
    }

    this.received.set(distance, true);

    final int first_missing_packet =
      this.received.nextClearBit(0);

    this.sequence_lo =
      this.serial.add(this.sequence_lo, first_missing_packet);

    this.received =
      this.received.get(
        first_missing_packet,
        Math.max(first_missing_packet, this.received.length()));

    return true;
  }

  public interface RangeConsumerType
  {
    void receive(
      int lower,
      int upper);
  }

  public void missed(
    final RangeConsumerType consumer)
  {
    final int length = this.received.length();
    final BitSet missed = this.received.get(0, length);
    missed.flip(0, length);

    int lower = missed.nextSetBit(0);
    while (lower >= 0) {
      final int upper = missed.nextClearBit(lower) - 1;
      if (upper == -1) {
        return;
      }

      consumer.receive(lower, upper);
      lower = missed.nextSetBit(upper + 1);
    }
  }
}

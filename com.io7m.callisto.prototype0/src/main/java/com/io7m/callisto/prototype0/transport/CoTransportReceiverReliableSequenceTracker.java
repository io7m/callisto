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

package com.io7m.callisto.prototype0.transport;

import com.io7m.callisto.prototype0.transport.CoTransportReceiverReliableSequenceTrackerListenerType.Error;
import com.io7m.callisto.prototype0.transport.messages.CoDataReliableFragmentInitial;
import com.io7m.callisto.prototype0.transport.messages.CoDataReliableFragmentSegment;
import com.io7m.callisto.prototype0.transport.messages.CoPacket;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jnull.NullCheck;
import com.io7m.jserial.core.SerialNumberIntType;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.nio.ByteBuffer;
import java.util.BitSet;

import static com.io7m.callisto.prototype0.transport.CoTransportReceiverReliableSequenceTrackerListenerType.Keep;

public final class CoTransportReceiverReliableSequenceTracker
{
  private final Int2ReferenceRBTreeMap<CoPacket> packets;
  private final BitSet received;
  private final SerialNumberIntType serial;
  private final CoTransportReceiverFragmentTracker fragments;
  private int sequence_start;
  private int sequence_current;
  private final FragmentListener fragment_listener;

  public CoTransportReceiverReliableSequenceTracker(
    final SerialNumberIntType in_serial,
    final int sequence)
  {
    this.serial = NullCheck.notNull(in_serial, "Serial");
    this.sequence_start = sequence;
    this.sequence_current = sequence;
    this.packets = new Int2ReferenceRBTreeMap<>(this.serial::compare);
    this.fragments = new CoTransportReceiverFragmentTracker();
    this.received = new BitSet(64);
    this.fragment_listener = new FragmentListener(this);
  }

  private static void postMissedIntervals(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final BitSet missed)
  {
    int p0 = missed.nextSetBit(0);
    int p1 = p0;
    for (int index = missed.nextSetBit(0);
         index >= 0;
         index = missed.nextSetBit(index + 1)) {
      if (index - p1 > 1) {
        listener.onReceivePacketRangeMissing(p0, p1);
        p0 = index;
      }
      p1 = index;
    }
    listener.onReceivePacketRangeMissing(p0, p1);
  }

  private static void onErrorPacketTooOld(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final int sequence_incoming)
  {
    listener.onReceiveError(
      sequence_incoming,
      Error.RECEIVER_TOO_OLD,
      "Packet " + sequence_incoming + " is too old");
  }

  private static void errorPacketAlreadyReceived(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final int sequence_incoming)
  {
    listener.onReceiveError(
      sequence_incoming,
      Error.RECEIVER_DUPLICATE,
      "Packet " + sequence_incoming + " already received");
  }

  private static void onErrorPacketFragmentProblem(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final int sequence_incoming,
    final String error_message)
  {
    listener.onReceiveError(
      sequence_incoming,
      Error.RECEIVER_FRAGMENT_PROBLEM,
      error_message);
  }

  private void receiveReliableFragmentInitial(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final CoPacket packet)
  {
    Preconditions.checkPrecondition(
      packet,
      packet.getValueCase() == CoPacket.ValueCase.DATA_RELIABLE_FRAGMENT_INITIAL,
      p -> "Must be initial fragment packet");

    final CoDataReliableFragmentInitial prfi =
      packet.getDataReliableFragmentInitial();
    final int sequence_incoming = prfi.getId().getSequence();

    if (this.packets.containsKey(sequence_incoming)) {
      errorPacketAlreadyReceived(listener, sequence_incoming);
      return;
    }

    final int distance =
      this.serial.compare(sequence_incoming, this.sequence_start);

    if (distance < 0) {
      onErrorPacketTooOld(listener, sequence_incoming);
      return;
    }

    this.fragment_listener.error = null;
    this.fragment_listener.error_message = null;
    this.fragments.receiveInitial(
      this.fragment_listener,
      sequence_incoming,
      prfi.getFragmentId(),
      prfi.getFragmentCount(),
      prfi.getMessageId(),
      prfi.getMessageType(),
      prfi.getMessageSize(),
      prfi.getMessageData().asReadOnlyByteBuffer());

    if (this.fragment_listener.error != null) {
      onErrorPacketFragmentProblem(
        listener,
        sequence_incoming,
        this.fragment_listener.error_message);
      return;
    }

    this.receivePacketSave(listener, packet, sequence_incoming, distance);
  }

  public void receive(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final CoPacket packet)
  {
    NullCheck.notNull(listener, "Listener");
    NullCheck.notNull(packet, "Packet");

    switch (packet.getValueCase()) {
      case HELLO:
      case HELLO_RESPONSE:
      case DATA_UNRELIABLE:
      case DATA_ACK:
      case BYE:
      case PING:
      case PONG:
      case VALUE_NOT_SET: {
        throw new IllegalArgumentException("Packet must be a reliable packet");
      }

      case DATA_RELIABLE: {
        this.receiveReliable(listener, packet);
        break;
      }
      case DATA_RELIABLE_FRAGMENT_INITIAL: {
        this.receiveReliableFragmentInitial(listener, packet);
        break;
      }
      case DATA_RELIABLE_FRAGMENT_SEGMENT: {
        this.receiveReliableFragmentSegment(listener, packet);
        break;
      }
    }
  }

  private void receiveReliableFragmentSegment(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final CoPacket packet)
  {
    Preconditions.checkPrecondition(
      packet,
      packet.getValueCase() == CoPacket.ValueCase.DATA_RELIABLE_FRAGMENT_SEGMENT,
      p -> "Must be fragment segment packet");

    final CoDataReliableFragmentSegment prfs =
      packet.getDataReliableFragmentSegment();
    final int sequence_incoming = prfs.getId().getSequence();

    if (this.packets.containsKey(sequence_incoming)) {
      errorPacketAlreadyReceived(listener, sequence_incoming);
      return;
    }

    final int distance =
      this.serial.compare(sequence_incoming, this.sequence_start);

    if (distance < 0) {
      onErrorPacketTooOld(listener, sequence_incoming);
      return;
    }

    this.fragment_listener.error = null;
    this.fragment_listener.error_message = null;
    this.fragments.receiveSegment(
      this.fragment_listener,
      sequence_incoming,
      prfs.getFragmentId(),
      prfs.getFragmentIndex(),
      prfs.getData().asReadOnlyByteBuffer());

    if (this.fragment_listener.error != null) {
      onErrorPacketFragmentProblem(
        listener,
        sequence_incoming,
        this.fragment_listener.error_message);
      return;
    }

    this.receivePacketSave(listener, packet, sequence_incoming, distance);
  }

  private void receiveReliable(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final CoPacket packet)
  {
    Preconditions.checkPrecondition(
      packet,
      packet.getValueCase() == CoPacket.ValueCase.DATA_RELIABLE,
      p -> "Must be reliable packet");

    final int sequence_incoming =
      packet.getDataReliable().getId().getSequence();

    if (this.packets.containsKey(sequence_incoming)) {
      errorPacketAlreadyReceived(listener, sequence_incoming);
      return;
    }

    final int distance =
      this.serial.compare(sequence_incoming, this.sequence_start);

    if (distance < 0) {
      onErrorPacketTooOld(listener, sequence_incoming);
      return;
    }

    this.receivePacketSave(listener, packet, sequence_incoming, distance);
  }

  private void receivePacketSave(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final CoPacket packet,
    final int sequence_incoming,
    final int distance)
  {
    this.received.set(distance, true);
    this.packets.put(sequence_incoming, packet);
    this.sequence_current = this.serial.add(this.sequence_start, distance);
    listener.onReceivePacketArrived(sequence_incoming, packet);
  }

  private static int fragmentId(
    final CoPacket packet)
  {
    switch (packet.getValueCase()) {
      case DATA_RELIABLE_FRAGMENT_INITIAL: {
        final CoDataReliableFragmentInitial pr =
          packet.getDataReliableFragmentInitial();
        return pr.getFragmentId();
      }
      case DATA_RELIABLE_FRAGMENT_SEGMENT: {
        final CoDataReliableFragmentSegment pr =
          packet.getDataReliableFragmentSegment();
        return pr.getFragmentId();
      }
      case HELLO:
      case HELLO_RESPONSE:
      case DATA_RELIABLE:
      case DATA_UNRELIABLE:
      case DATA_ACK:
      case BYE:
      case PING:
      case PONG:
      case VALUE_NOT_SET:
        break;
    }
    throw new UnreachableCodeException();
  }

  public void poll(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    NullCheck.notNull(listener, "Listener");

    final int ready_upper_exclusive = this.viewCheckMissing(listener);
    if (ready_upper_exclusive == -1) {
      return;
    }

    final IntRBTreeSet ready_sequences =
      new IntRBTreeSet(this.packets.keySet().subSet(
        this.sequence_start, ready_upper_exclusive));

    for (final int sequence : ready_sequences) {
      final CoPacket packet = this.packets.get(sequence);
      if (packet == null) {
        continue;
      }

      switch (packet.getValueCase()) {
        case DATA_RELIABLE: {
          onPollReliable(
            listener, sequence, packet, () -> {
              this.packets.remove(sequence);
              listener.onReceivePacketDiscarded(sequence, packet);
            });
          break;
        }

        case DATA_RELIABLE_FRAGMENT_INITIAL:
        case DATA_RELIABLE_FRAGMENT_SEGMENT: {
          if (!this.onPollFragment(listener, sequence, packet, () -> {
            this.packets.remove(sequence);
            listener.onReceivePacketDiscarded(sequence, packet);
          })) {
            return;
          }
          break;
        }

        case HELLO:
        case HELLO_RESPONSE:
        case DATA_UNRELIABLE:
        case DATA_ACK:
        case BYE:
        case PING:
        case PONG:
        case VALUE_NOT_SET: {
          throw new UnreachableCodeException();
        }
      }
    }
  }

  private boolean onPollFragment(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final int sequence,
    final CoPacket packet,
    final Runnable on_discard)
  {
    final Int2IntMap incomplete_seq = this.fragments.incompleteBySequence();
    if (incomplete_seq.containsKey(sequence)) {
      listener.onReceivePacketFragmentNotReady(
        sequence, fragmentId(packet));
      return false;
    }

    final int fragment_id = fragmentId(packet);
    this.fragment_listener.error_message = null;
    this.fragment_listener.error = null;
    this.fragments.viewCompletedById(this.fragment_listener, fragment_id);

    if (this.fragment_listener.error != null) {
      listener.onReceiveError(
        sequence,
        Error.RECEIVER_FRAGMENT_PROBLEM,
        this.fragment_listener.error_message);
      return false;
    }

    final Keep keep =
      listener.onReceivePacketReassembledFragmentReadyForDelivery(
        sequence,
        fragment_id,
        this.fragment_listener.message_id,
        this.fragment_listener.message_type,
        this.fragment_listener.message_data);

    for (final int frag_sequence : this.fragment_listener.sequences) {
      if (frag_sequence != this.fragment_listener.sequence_start) {
        this.packets.remove(frag_sequence);
      }
    }

    switch (keep) {
      case KEEP: {
        return true;
      }
      case DISCARD: {
        on_discard.run();
        return true;
      }
    }

    throw new UnreachableCodeException();
  }

  private static void onPollReliable(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener,
    final int sequence,
    final CoPacket packet,
    final Runnable on_discard)
  {
    final Keep r = listener.onReceivePacketReadyForDelivery(sequence, packet);
    switch (r) {
      case KEEP: {
        break;
      }
      case DISCARD: {
        on_discard.run();
        break;
      }
    }
  }

  /**
   * Check to see if any reliable packets are missing.
   */

  private int viewCheckMissing(
    final CoTransportReceiverReliableSequenceTrackerListenerType listener)
  {
    final int range =
      this.serial.distanceUnsigned(
        this.sequence_start, this.sequence_current);

    final BitSet missed = this.received.get(0, range);
    missed.flip(0, range);

    final int ready_upper_exclusive;
    if (missed.isEmpty()) {
      if (!this.packets.isEmpty()) {
        ready_upper_exclusive =
          this.serial.add(this.packets.lastIntKey(), 1);
      } else {
        ready_upper_exclusive = -1;
      }
    } else {
      postMissedIntervals(listener, missed);
      ready_upper_exclusive = this.serial.add(
        this.sequence_start, missed.nextSetBit(0));
    }
    return ready_upper_exclusive;
  }

  private static final class FragmentListener
    implements CoTransportReceiverFragmentTrackerListenerType
  {
    private final CoTransportReceiverReliableSequenceTracker tracker;
    private Error error;
    private String error_message;
    private IntOpenHashSet not_ready;
    private int message_type;
    private int message_id;
    private ByteBuffer message_data;
    private final IntOpenHashSet sequences;
    private int sequence_start;

    private FragmentListener(
      final CoTransportReceiverReliableSequenceTracker in_tracker)
    {
      this.tracker = NullCheck.notNull(in_tracker, "Tracker");
      this.not_ready = new IntOpenHashSet();
      this.sequences = new IntOpenHashSet();
    }

    @Override
    public void onFragmentStart(
      final int id,
      final int count,
      final int message_type,
      final int message_id,
      final ByteBuffer data)
    {

    }

    @Override
    public void onFragmentSegment(
      final int id,
      final int index,
      final ByteBuffer data)
    {

    }

    @Override
    public void onFragmentError(
      final int id,
      final Error e,
      final String message)
    {
      this.error = e;
      this.error_message = message;
    }

    @Override
    public FragmentKeep onFragmentReady(
      final int sequence_initial,
      final IntSet sequences,
      final int fragment_id,
      final int message_type,
      final int message_id,
      final ByteBuffer message_data)
    {
      this.message_type = message_type;
      this.message_id = message_id;
      this.message_data = message_data;
      this.sequence_start = sequence_initial;
      this.sequences.clear();
      this.sequences.addAll(sequences);
      return FragmentKeep.KEEP;
    }

    @Override
    public void onFragmentNotReady(
      final int id)
    {
      this.not_ready.add(id);
    }

    @Override
    public void onFragmentDiscarded(
      final int id)
    {
      throw new UnimplementedCodeException();
    }
  }
}

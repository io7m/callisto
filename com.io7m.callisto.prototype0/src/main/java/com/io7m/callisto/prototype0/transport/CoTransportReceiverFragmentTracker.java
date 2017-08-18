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

import com.io7m.callisto.prototype0.transport.CoTransportReceiverFragmentTrackerErrorReceiverType.Error;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;

import static com.io7m.callisto.prototype0.transport.CoTransportReceiverFragmentTrackerListenerType.FragmentKeep;

public final class CoTransportReceiverFragmentTracker
{
  private final Int2ReferenceOpenHashMap<FragmentInProgress> incomplete;
  private final Int2ReferenceOpenHashMap<FragmentCompleted> completed;
  private final IntSet incomplete_view;
  private final IntSet completed_view;
  private final Int2IntOpenHashMap incomplete_by_seq;
  private final Int2IntMap incomplete_by_seq_view;

  public CoTransportReceiverFragmentTracker()
  {
    this.incomplete =
      new Int2ReferenceOpenHashMap<>();
    this.incomplete_view =
      IntSets.unmodifiable(this.incomplete.keySet());
    this.incomplete_by_seq =
      new Int2IntOpenHashMap();
    this.incomplete_by_seq_view =
      Int2IntMaps.unmodifiable(this.incomplete_by_seq);
    this.completed =
      new Int2ReferenceOpenHashMap<>();
    this.completed_view =
      IntSets.unmodifiable(this.completed.keySet());
  }

  static Optional<FragmentCompleted> complete(
    final CoTransportReceiverFragmentTrackerListenerType listener,
    final FragmentInProgress fragment)
  {
    try (final ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
      try (final WritableByteChannel channel = Channels.newChannel(bao)) {
        for (final int index : fragment.message_data.keySet()) {
          final ByteBuffer data = fragment.message_data.get(index);
          channel.write(data);
        }
      }

      final byte[] data_array = bao.toByteArray();
      if (data_array.length != fragment.message_size) {
        listener.onFragmentError(
          fragment.id,
          Error.FRAGMENT_ERROR_SIZE_MISMATCH,
          new StringBuilder(64)
            .append("Incorrect data size.")
            .append(System.lineSeparator())
            .append("  Expected: ")
            .append(fragment.message_size)
            .append(" octets")
            .append(System.lineSeparator())
            .append("  Received: ")
            .append(data_array.length)
            .append(" octets")
            .append(System.lineSeparator())
            .toString());
        return Optional.empty();
      }

      final ByteBuffer data = ByteBuffer.wrap(data_array);
      final FragmentCompleted completed =
        new FragmentCompleted(
          fragment.id,
          fragment.message_id,
          fragment.message_type,
          data,
          fragment.sequences,
          fragment.sequence_start);

      return Optional.of(completed);
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    }
  }

  public void receiveInitial(
    final CoTransportReceiverFragmentTrackerListenerType listener,
    final int sequence,
    final int id,
    final int count,
    final int message_id,
    final int message_type,
    final int message_size,
    final ByteBuffer message_data)
  {
    NullCheck.notNull(listener, "Listener");
    NullCheck.notNull(message_data, "Data");

    if (this.incomplete.containsKey(id)) {
      listener.onFragmentError(
        id,
        Error.FRAGMENT_ERROR_ALREADY_STARTED,
        "Fragment " + id + " has already been started");
      return;
    }

    final FragmentInProgress frag =
      new FragmentInProgress(
        id, count, message_id, message_type, message_size, sequence);
    frag.message_data.put(0, message_data);
    frag.sequences.add(sequence);
    this.incomplete_by_seq.put(sequence, id);
    this.incomplete.put(id, frag);
    listener.onFragmentStart(id, count, message_type, message_id, message_data);
  }

  public void receiveSegment(
    final CoTransportReceiverFragmentTrackerListenerType listener,
    final int sequence,
    final int id,
    final int index,
    final ByteBuffer data)
  {
    NullCheck.notNull(listener, "Listener");
    NullCheck.notNull(data, "Data");

    if (!this.incomplete.containsKey(id)) {
      listener.onFragmentError(
        id,
        Error.FRAGMENT_ERROR_NONEXISTENT,
        "Fragment " + id + " does not exist");
      return;
    }

    final FragmentInProgress frag = this.incomplete.get(id);
    if (frag.message_data.containsKey(index)) {
      listener.onFragmentError(
        id,
        Error.FRAGMENT_ERROR_SEGMENT_ALREADY_PROVIDED,
        "Segment " + index + " already provided");
      return;
    }

    frag.sequences.add(sequence);
    frag.message_data.put(index, data);
    this.incomplete_by_seq.put(sequence, frag.id);
    listener.onFragmentSegment(id, index, data);

    if (frag.isCompleted()) {
      this.incomplete.remove(id);
      for (final int frag_seq : frag.sequences) {
        this.incomplete_by_seq.remove(frag_seq);
      }

      final Optional<FragmentCompleted> c_opt = complete(listener, frag);
      c_opt.ifPresent(c -> {
        this.completed.put(id, c);
        this.view(listener, id, () -> {
          this.completed.remove(id);
          listener.onFragmentDiscarded(id);
        });
      });
    }
  }

  public IntSet incomplete()
  {
    return this.incomplete_view;
  }

  public IntSet completed()
  {
    return this.completed_view;
  }

  public void viewCompletedById(
    final CoTransportReceiverFragmentTrackerMessageReceiverType receiver,
    final int frag_id)
  {
    NullCheck.notNull(receiver, "Receiver");

    if (this.completed.containsKey(frag_id)) {
      this.view(receiver, frag_id, () -> this.completed.remove(frag_id));
    } else {
      receiver.onFragmentError(
        frag_id,
        Error.FRAGMENT_ERROR_NONEXISTENT,
        "Fragment " + frag_id + " does not exist");
    }
  }

  private void view(
    final CoTransportReceiverFragmentTrackerMessageReceiverType receiver,
    final int id,
    final Runnable on_discard)
  {
    final FragmentCompleted f = this.completed.get(id);
    final FragmentKeep k =
      receiver.onFragmentReady(
        f.sequence_start,
        f.sequences,
        id,
        f.message_type,
        f.message_id,
        f.message_data);

    switch (k) {
      case KEEP: {
        break;
      }
      case DISCARD: {
        on_discard.run();
        break;
      }
    }
  }

  public void poll(
    final CoTransportReceiverFragmentTrackerListenerType listener)
  {
    NullCheck.notNull(listener, "Listener");

    for (final int id : this.incomplete.keySet()) {
      listener.onFragmentNotReady(id);
    }

    final IntIterator completed_iter = this.completed.keySet().iterator();
    while (completed_iter.hasNext()) {
      final int id = completed_iter.nextInt();
      this.view(listener, id, () -> {
        completed_iter.remove();
        listener.onFragmentDiscarded(id);
      });
    }
  }

  public Int2IntMap incompleteBySequence()
  {
    return this.incomplete_by_seq_view;
  }

  private static final class FragmentCompleted
  {
    private final int id;
    private final int message_type;
    private final int message_id;
    private final ByteBuffer message_data;
    private final IntSet sequences;
    private final int sequence_start;

    FragmentCompleted(
      final int in_id,
      final int in_message_id,
      final int in_message_type,
      final ByteBuffer in_message_data,
      final IntOpenHashSet in_sequences,
      final int in_sequence_start)
    {
      this.id = in_id;
      this.message_id = in_message_id;
      this.message_type = in_message_type;
      this.message_data =
        NullCheck.notNull(in_message_data, "Message Data");
      this.sequences =
        IntSets.unmodifiable(NullCheck.notNull(in_sequences, "Sequences"));
      this.sequence_start = in_sequence_start;
    }
  }

  private static final class FragmentInProgress
  {
    private final int id;
    private final int message_type;
    private final int message_id;
    private final Int2ReferenceRBTreeMap<ByteBuffer> message_data;
    private final int count;
    private final int message_size;
    private final IntOpenHashSet sequences;
    private final int sequence_start;

    FragmentInProgress(
      final int in_id,
      final int in_count,
      final int in_message_id,
      final int in_message_type,
      final int in_message_size,
      final int in_sequence_start)
    {
      this.id = in_id;
      this.count = in_count;
      this.message_type = in_message_type;
      this.message_id = in_message_id;
      this.message_data = new Int2ReferenceRBTreeMap<>();
      this.message_size = in_message_size;
      this.sequences = new IntOpenHashSet();
      this.sequence_start = in_sequence_start;
    }

    boolean isCompleted()
    {
      return this.message_data.size() == this.count;
    }
  }
}

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

import com.google.protobuf.ByteString;
import com.io7m.callisto.prototype0.messages.CoDataReliable;
import com.io7m.callisto.prototype0.messages.CoDataReliableFragment;
import com.io7m.callisto.prototype0.messages.CoDataUnreliable;
import com.io7m.callisto.prototype0.messages.CoMessage;
import com.io7m.callisto.prototype0.messages.CoPacket;
import com.io7m.callisto.prototype0.messages.CoPacketIdentifier;
import com.io7m.callisto.prototype0.messages.CoStringConstant;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantReference;
import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jnull.NullCheck;
import com.io7m.jserial.core.SerialNumber24;
import com.io7m.jserial.core.SerialNumberIntType;

import java.nio.ByteBuffer;

public final class CoTransportPacketBuilder
{
  /**
   * Assume that it costs at most 2 bytes to add a message to a packet.
   */

  private static final int PER_MESSAGE_OVERHEAD = 2;

  private final SerialNumberIntType serial;
  private final int channel;
  private final int id;
  private final int packet_size_limit;
  private final CoDataReliable.Builder packet_reliable;
  private final int packet_reliable_size_base;
  private final CoDataUnreliable.Builder packet_unreliable;
  private final int packet_unreliable_size_base;
  private final CoDataReliableFragment.Builder packet_reliable_fragment;
  private final int packet_reliable_fragment_size_base;
  private final int packet_reliable_fragment_body_size_limit;
  private int packet_reliable_size;
  private int packet_unreliable_size;
  private int sequence_packet_sent_reliable;
  private int sequence_packet_sent_unreliable;
  private int sequence_packet_received_reliable;
  private int sequence_packet_received_ack;
  private int sequence_message_sent;

  public CoTransportPacketBuilder(
    final int in_packet_size_limit,
    final int in_channel,
    final int in_id)
  {
    this.packet_size_limit = in_packet_size_limit;
    this.channel = in_channel;
    this.id = in_id;
    this.serial = SerialNumber24.get();

    this.packet_reliable = CoDataReliable.newBuilder();
    this.packet_reliable_size_base = reliableBaseSize();
    this.packet_reliable_size = this.packet_reliable_size_base;

    this.packet_unreliable = CoDataUnreliable.newBuilder();
    this.packet_unreliable_size_base = unreliableBaseSize();
    this.packet_unreliable_size = this.packet_unreliable_size_base;

    this.packet_reliable_fragment = CoDataReliableFragment.newBuilder();
    this.packet_reliable_fragment_size_base = reliableFragmentBaseSize();
    this.packet_reliable_fragment_body_size_limit =
      this.packet_size_limit - this.packet_reliable_fragment_size_base;
  }

  /**
   * @return The base size of a reliable packet with the largest possible packet
   * identifier.
   */

  private static int reliableBaseSize()
  {
    return CoPacket.newBuilder()
      .setDataReliable(
        CoDataReliable.newBuilder().setId(packetIDLargest()))
      .build()
      .getSerializedSize();
  }

  /**
   * @return The base size of a reliable fragmented packet with the largest
   * possible packet identifier and fragment metadata.
   */

  private static int reliableFragmentBaseSize()
  {
    final CoDataReliableFragment frag =
      CoDataReliableFragment.newBuilder()
        .setId(packetIDLargest())
        .setFragmentCount(0xffffffff)
        .setFragmentIndex(0xffffffff)
        .setMessageId(0xffffffff)
        .setMessageType(CoStringConstant.newBuilder().setValue(0xffffffff))
        .build();

    return CoPacket.newBuilder()
      .setDataReliableFragment(frag)
      .build()
      .getSerializedSize();
  }

  /**
   * @return The largest possible packet identifier
   */

  private static CoPacketIdentifier packetIDLargest()
  {
    return CoPacketIdentifier.newBuilder()
      .setSequenceReceivedReliable(0xffffffff)
      .setSequence(0xffffffff)
      .setSequenceReceivedAck(0b11111111_11111111_11111111_11111111)
      .setConnectionId(0xffffffff)
      .setChannel(0xffffffff)
      .build();
  }

  /**
   * @return The base size of an unreliable packet with the largest possible
   * packet identifier.
   */

  private static int unreliableBaseSize()
  {
    return CoPacket.newBuilder()
      .setDataUnreliable(CoDataUnreliable.newBuilder().setId(packetIDLargest()))
      .build()
      .getSerializedSize();
  }

  private boolean unreliableMessageCanFit(
    final ByteBuffer message)
  {
    final int size = message.remaining();
    return this.packet_unreliable_size + size < this.packet_size_limit;
  }

  private boolean reliableMessageCanFit(
    final ByteBuffer message)
  {
    final int size = message.remaining();
    return this.packet_reliable_size + size < this.packet_size_limit;
  }

  private void unreliableMessageAppend(
    final CoStringConstantReference message_type,
    final ByteBuffer message_data)
  {
    final CoStringConstant p_type =
      CoStringConstant.newBuilder()
        .setValue(message_type.value())
        .build();

    final ByteString p_data =
      ByteString.copyFrom(message_data, message_data.remaining());

    final CoMessage message =
      CoMessage.newBuilder()
        .setMessageId(this.sequence_message_sent)
        .setMessageType(p_type)
        .setMessageData(p_data)
        .build();

    this.packet_unreliable.addMessages(message);
    this.packet_unreliable_size += message.getSerializedSize() + PER_MESSAGE_OVERHEAD;
    this.sequence_message_sent = this.serial.add(this.sequence_message_sent, 1);
  }

  private void reliableMessageAppend(
    final CoStringConstantReference message_type,
    final ByteBuffer message_data)
  {
    final CoStringConstant p_type =
      CoStringConstant.newBuilder()
        .setValue(message_type.value())
        .build();

    final ByteString p_data =
      ByteString.copyFrom(message_data, message_data.remaining());

    final CoMessage message =
      CoMessage.newBuilder()
        .setMessageId(this.sequence_message_sent)
        .setMessageType(p_type)
        .setMessageData(p_data)
        .build();

    this.packet_reliable.addMessages(message);
    this.packet_reliable_size += message.getSerializedSize() + PER_MESSAGE_OVERHEAD;
    this.sequence_message_sent = this.serial.add(this.sequence_message_sent, 1);
  }

  private CoPacket unreliableFinish()
  {
    final CoDataUnreliable pd =
      this.packet_unreliable.build();
    final CoPacket p =
      CoPacket.newBuilder().setDataUnreliable(pd).build();

    this.sequence_packet_sent_unreliable =
      this.serial.add(this.sequence_packet_sent_unreliable, 1);

    this.packet_unreliable.clear();
    this.packet_unreliable_size = this.packet_unreliable_size_base;
    return p;
  }

  private void unreliableStart()
  {
    this.packet_unreliable.setId(
      CoPacketIdentifier.newBuilder()
        .setChannel(this.channel)
        .setConnectionId(this.id)
        .setSequence(this.sequence_packet_sent_unreliable)
        .setSequenceReceivedAck(this.sequence_packet_received_ack)
        .setSequenceReceivedReliable(this.sequence_packet_received_reliable)
        .build());
  }

  /**
   * @param message The message
   *
   * @return {@code true} if the given message could never fit in a single
   * unfragmented packet
   */

  private boolean messageRequiresFragmentation(
    final ByteBuffer message)
  {
    return message.remaining() >= this.packet_size_limit;
  }

  /**
   * Append data to the current unreliable packet.
   *
   * @param output       A queue that will receive any completed packets
   * @param message_type The message type
   * @param message_data The message data
   */

  public void unreliableAppend(
    final ListenerType output,
    final CoStringConstantReference message_type,
    final ByteBuffer message_data)
  {
    NullCheck.notNull(output, "Output");
    NullCheck.notNull(message_type, "Message type");
    NullCheck.notNull(message_data, "Message data");

    if (this.unreliableMessageCanFit(message_data)) {
      this.unreliableMessageAppend(message_type, message_data);
      return;
    }

    if (this.unreliableAnyRemaining()) {
      output.onCreatedPacketUnreliable(this.unreliableFinish());
      this.unreliableStart();
    }

    if (this.messageRequiresFragmentation(message_data)) {
      this.makeFragments(output, message_type, message_data);
      return;
    }

    this.unreliableMessageAppend(message_type, message_data);
  }

  /**
   * Complete any remaining unreliable packets.
   *
   * @param output A queue that will receive any completed packets
   */

  public void unreliableFinishRemaining(
    final ListenerType output)
  {
    NullCheck.notNull(output, "Output");

    if (this.unreliableAnyRemaining()) {
      output.onCreatedPacketUnreliable(this.unreliableFinish());
      Invariants.checkInvariant(
        !this.unreliableAnyRemaining(),
        "No unreliable data remaining");
    }
  }

  private CoPacket reliableFinish()
  {
    final CoDataReliable pd =
      this.packet_reliable.build();
    final CoPacket p =
      CoPacket.newBuilder().setDataReliable(pd).build();

    this.sequence_packet_sent_reliable =
      this.serial.add(this.sequence_packet_sent_reliable, 1);

    this.packet_reliable.clear();
    this.packet_reliable_size = this.packet_reliable_size_base;
    return p;
  }

  private void reliableStart()
  {
    this.packet_reliable.setId(
      CoPacketIdentifier.newBuilder()
        .setChannel(this.channel)
        .setConnectionId(this.id)
        .setSequence(this.sequence_packet_sent_reliable)
        .setSequenceReceivedAck(this.sequence_packet_received_ack)
        .setSequenceReceivedReliable(this.sequence_packet_received_reliable)
        .build());
  }

  /**
   * Append data to the current reliable packet.
   *
   * @param output       A listener that will receive any completed packets
   * @param message_type The message type
   * @param message_data The message data
   */

  public void reliableAppend(
    final ListenerType output,
    final CoStringConstantReference message_type,
    final ByteBuffer message_data)
  {
    NullCheck.notNull(output, "Output");
    NullCheck.notNull(message_type, "Message type");
    NullCheck.notNull(message_data, "Message data");

    if (this.reliableMessageCanFit(message_data)) {
      this.reliableMessageAppend(message_type, message_data);
      return;
    }

    if (this.reliableAnyRemaining()) {
      output.onCreatedPacketReliable(this.reliableFinish());
      this.reliableStart();
    }

    if (this.messageRequiresFragmentation(message_data)) {
      this.makeFragments(output, message_type, message_data);
      return;
    }

    this.reliableMessageAppend(message_type, message_data);
  }

  /**
   * Complete any remaining reliable packets.
   *
   * @param output A listener that will receive any completed packets
   */

  public void reliableFinishRemaining(
    final ListenerType output)
  {
    NullCheck.notNull(output, "Output");

    if (this.reliableAnyRemaining()) {
      output.onCreatedPacketReliable(this.reliableFinish());
      Invariants.checkInvariant(
        !this.reliableAnyRemaining(),
        "No reliable data remaining");
    }
  }

  private boolean reliableAnyRemaining()
  {
    return this.packet_reliable_size > this.packet_reliable_size_base;
  }

  private boolean unreliableAnyRemaining()
  {
    return this.packet_unreliable_size > this.packet_unreliable_size_base;
  }

  private void reliableFragmentStart()
  {
    this.packet_reliable_fragment.setId(
      CoPacketIdentifier.newBuilder()
        .setChannel(this.channel)
        .setConnectionId(this.id)
        .setSequence(this.sequence_packet_sent_reliable)
        .setSequenceReceivedAck(this.sequence_packet_received_ack)
        .setSequenceReceivedReliable(this.sequence_packet_received_reliable)
        .build());
  }

  private CoPacket reliableFragmentFinish()
  {
    final CoDataReliableFragment pd =
      this.packet_reliable_fragment.build();
    final CoPacket p =
      CoPacket.newBuilder().setDataReliableFragment(pd).build();

    this.sequence_packet_sent_reliable =
      this.serial.add(this.sequence_packet_sent_reliable, 1);

    this.packet_reliable_fragment.clear();
    return p;
  }

  private void makeFragments(
    final ListenerType output,
    final CoStringConstantReference type,
    final ByteBuffer message)
  {
    Preconditions.checkPreconditionI(
      message.remaining(),
      message.remaining() >= this.packet_size_limit,
      value -> "Message size must be >= " + this.packet_size_limit);

    final int frag_size_limit =
      this.packet_reliable_fragment_body_size_limit;
    final int frag_count =
      (message.limit() / frag_size_limit) + 1;

    for (int frag_index = 1; frag_index <= frag_count; ++frag_index) {
      final int size =
        Math.min(message.remaining(), frag_size_limit);

      this.reliableFragmentStart();
      this.packet_reliable_fragment.setFragmentCount(frag_count);
      this.packet_reliable_fragment.setFragmentIndex(frag_index);
      this.packet_reliable_fragment.setMessageType(
        CoStringConstant.newBuilder().setValue(type.value()).build());
      this.packet_reliable_fragment.setMessageId(
        this.sequence_message_sent);
      this.packet_reliable_fragment.setMessageData(
        ByteString.copyFrom(message, size));

      output.onCreatedPacketReliableFragment(this.reliableFragmentFinish());
    }

    Postconditions.checkPostconditionI(
      message.remaining(),
      message.remaining() == 0,
      value -> "No message data must remain.");
  }

  public interface ListenerType
  {
    void onCreatedPacketReliable(CoPacket p);

    void onCreatedPacketUnreliable(CoPacket p);

    void onCreatedPacketReliableFragment(CoPacket p);
  }
}

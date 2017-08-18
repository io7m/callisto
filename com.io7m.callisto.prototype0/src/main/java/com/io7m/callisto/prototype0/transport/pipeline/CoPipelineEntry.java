package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.callisto.prototype0.transport.messages.CoDataAck;
import com.io7m.callisto.prototype0.transport.messages.CoDataReliable;
import com.io7m.callisto.prototype0.transport.messages.CoDataReliableFragmentInitial;
import com.io7m.callisto.prototype0.transport.messages.CoDataReliableFragmentSegment;
import com.io7m.callisto.prototype0.transport.messages.CoDataUnreliable;
import com.io7m.callisto.prototype0.transport.messages.CoPacket;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import io.reactivex.Observable;

public final class CoPipelineEntry
{
  private CoPipelineEntry()
  {
    throw new UnreachableCodeException();
  }

  public static Observable<CoPipelinePacketType> filter(
    final Observable<CoPacket> packets)
  {
    NullCheck.notNull(packets, "Packets");

    return packets.filter(CoPipelineEntry::isConvertible)
      .map(CoPipelineEntry::toPipelinePacket);
  }

  private static CoPipelinePacketType toPipelinePacket(
    final CoPacket packet)
  {
    switch (packet.getValueCase()) {
      case HELLO:
      case HELLO_RESPONSE:
      case BYE:
      case PING:
      case PONG:
      case VALUE_NOT_SET: {
        throw new UnreachableCodeException();
      }

      case DATA_ACK: {
        final CoDataAck inner = packet.getDataAck();
        return CoPipelinePacketNegativeACK.of(
          inner.getId().getSequence(),
          inner.getSequencesReliableNotReceivedList());
      }

      case DATA_RELIABLE: {
        final CoDataReliable inner = packet.getDataReliable();
        return CoPipelinePacketReliableSingle.of(
          inner.getId().getSequence(),
          inner.getMessagesList());
      }

      case DATA_UNRELIABLE: {
        final CoDataUnreliable inner = packet.getDataUnreliable();
        return CoPipelinePacketUnreliable.of(
          inner.getId().getSequence(),
          inner.getMessagesList());
      }

      case DATA_RELIABLE_FRAGMENT_INITIAL: {
        final CoDataReliableFragmentInitial inner =
          packet.getDataReliableFragmentInitial();
        return CoPipelinePacketReliableFragmentInitial.of(
          inner.getId().getSequence(),
          inner.getFragmentId(),
          inner.getFragmentCount(),
          inner.getMessageSize(),
          inner.getMessageType(),
          inner.getMessageData().asReadOnlyByteBuffer());
      }

      case DATA_RELIABLE_FRAGMENT_SEGMENT: {
        final CoDataReliableFragmentSegment inner =
          packet.getDataReliableFragmentSegment();
        return CoPipelinePacketReliableFragmentSuccessor.of(
          inner.getId().getSequence(),
          inner.getFragmentId(),
          inner.getFragmentIndex(),
          inner.getData().asReadOnlyByteBuffer());
      }
    }

    throw new UnreachableCodeException();
  }

  private static boolean isConvertible(
    final CoPacket packet)
  {
    switch (packet.getValueCase()) {
      case HELLO:
      case HELLO_RESPONSE:
      case DATA_ACK:
      case BYE:
      case PING:
      case PONG:
      case VALUE_NOT_SET:
        return false;
      case DATA_RELIABLE:
      case DATA_UNRELIABLE:
      case DATA_RELIABLE_FRAGMENT_INITIAL:
      case DATA_RELIABLE_FRAGMENT_SEGMENT:
        return true;
    }
    throw new UnreachableCodeException();
  }
}

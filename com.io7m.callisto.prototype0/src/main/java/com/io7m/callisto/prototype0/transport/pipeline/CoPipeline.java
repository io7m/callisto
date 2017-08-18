package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.callisto.prototype0.transport.messages.CoPacket;
import com.io7m.jnull.NullCheck;
import com.io7m.jserial.core.SerialNumber24;
import com.io7m.jserial.core.SerialNumberIntType;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public final class CoPipeline
{
  public static CoPipeline create(
    final CoPipelineConfiguration config,
    final Observable<CoPacket> packets)
  {
    NullCheck.notNull(config, "Config");
    NullCheck.notNull(packets, "Packets");

    final SerialNumberIntType serial =
      SerialNumber24.get();
    final CoPipelineErrorsType errors =
      CoPipelineErrors.create();
    final PublishSubject<CoPipelinePoll> poll =
      PublishSubject.create();

    final Observable<CoPipelinePacketType> entry =
      CoPipelineEntry.filter(packets);
    final Observable<CoPipelinePacketUnreliable> only_unreliables =
      entry.ofType(CoPipelinePacketUnreliable.class);
    final Observable<CoPipelinePacketReliableSingle> only_reliables =
      entry.ofType(CoPipelinePacketReliableSingle.class);
    final Observable<CoPipelinePacketReliableFragmentType> only_fragments =
      entry.ofType(CoPipelinePacketReliableFragmentType.class);

    final Observable<CoPipelinePacketReliableType> reliables =
      Observable.merge(only_reliables, only_fragments);

    final CoPipelineReliableSequenceTracker sequence_tracker =
      CoPipelineReliableSequenceTracker.create(serial, errors, poll, reliables);

    final CoPipelineFragmentAssembly fragment_assembly =
      CoPipelineFragmentAssembly.create(sequence_tracker.fragments());

    final Observable<CoPipelineMessageReliable> messages_reliable =
      Observable.merge(
        sequence_tracker.messages(),
        fragment_assembly.messages());

    final CoPipelineUnreliableSequenceTracker messages_unreliable =
      CoPipelineUnreliableSequenceTracker.create(only_unreliables);

    final CoPipelineMessageOrdering ordering =
      CoPipelineMessageOrdering.create(
        messages_reliable,
        messages_unreliable.messages());

    return new CoPipeline();
  }

  public void poll()
  {

  }

  private CoPipeline()
  {

  }
}

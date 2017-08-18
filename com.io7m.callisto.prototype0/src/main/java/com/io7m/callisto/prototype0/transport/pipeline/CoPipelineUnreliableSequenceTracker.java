package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.jnull.NullCheck;
import io.reactivex.Observable;

public final class CoPipelineUnreliableSequenceTracker
{
  private final Observable<CoPipelineMessageUnreliable> messages;

  private CoPipelineUnreliableSequenceTracker(
    final Observable<CoPipelinePacketUnreliable> unreliables,
    final Observable<CoPipelineMessageUnreliable> in_messages)
  {
    NullCheck.notNull(unreliables, "Unreliables");
    this.messages = NullCheck.notNull(in_messages, "Messages");
  }

  public static CoPipelineUnreliableSequenceTracker create(
    final Observable<CoPipelinePacketUnreliable> unreliables)
  {
    final Observable<CoPipelineMessageUnreliable> messages =
      unreliables.concatMap(packet -> Observable.fromIterable(packet.messages()))
        .map(CoPipelineMessageUnreliable::of);

    return new CoPipelineUnreliableSequenceTracker(unreliables, messages);
  }

  public Observable<CoPipelineMessageUnreliable> messages()
  {
    return this.messages;
  }
}

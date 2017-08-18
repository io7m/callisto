package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.callisto.prototype0.transport.messages.CoMessage;
import com.io7m.jnull.NullCheck;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public final class CoPipelineMessageOrdering
{
  private final Observable<CoPipelineMessageReliable> reliables;
  private final Observable<CoPipelineMessageUnreliable> unreliables;
  private final PublishSubject<CoMessage> messages;

  private CoPipelineMessageOrdering(
    final Observable<CoPipelineMessageReliable> in_reliables,
    final Observable<CoPipelineMessageUnreliable> in_unreliables,
    final PublishSubject<CoMessage> in_messages)
  {
    this.reliables = NullCheck.notNull(in_reliables, "Reliables");
    this.unreliables = NullCheck.notNull(in_unreliables, "Unreliables");
    this.messages = NullCheck.notNull(in_messages, "Messages");
    this.reliables.subscribe(this::onReliable);
    this.unreliables.subscribe(this::onUnreliable);
  }

  public static CoPipelineMessageOrdering create(
    final Observable<CoPipelineMessageReliable> reliables,
    final Observable<CoPipelineMessageUnreliable> unreliables)
  {
    final PublishSubject<CoMessage> messages = PublishSubject.create();
    return new CoPipelineMessageOrdering(reliables, unreliables, messages);
  }

  public Observable<CoMessage> messages()
  {
    return this.messages;
  }

  private void onUnreliable(
    final CoPipelineMessageUnreliable r)
  {

  }

  private void onReliable(
    final CoPipelineMessageReliable r)
  {

  }
}

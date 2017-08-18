package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public final class CoPipelineFragmentAssembly
{
  private final PublishSubject<CoPipelineMessageReliable> messages;
  private final PublishSubject<CoPipelineFragmentEventType> events;

  public CoPipelineFragmentAssembly(
    final Observable<CoPipelinePacketReliableFragmentType> in_fragments,
    final PublishSubject<CoPipelineMessageReliable> in_messages,
    final PublishSubject<CoPipelineFragmentEventType> in_events)
  {
    NullCheck.notNull(in_fragments, "Fragments");
    this.messages = NullCheck.notNull(in_messages, "Messages");
    this.events = NullCheck.notNull(in_events, "Events");
    in_fragments.subscribe(this::onFragment);
  }

  public static CoPipelineFragmentAssembly create(
    final Observable<CoPipelinePacketReliableFragmentType> fragments)
  {
    final PublishSubject<CoPipelineMessageReliable> messages =
      PublishSubject.create();
    final PublishSubject<CoPipelineFragmentEventType> events =
      PublishSubject.create();

    return new CoPipelineFragmentAssembly(fragments, messages, events);
  }

  public Observable<CoPipelineFragmentEventType> fragmentEvents()
  {
    return this.events;
  }

  public Observable<CoPipelineMessageReliable> messages()
  {
    return this.messages;
  }

  private void onFragment(
    final CoPipelinePacketReliableFragmentType f)
  {
    switch (f.type()) {
      case UNRELIABLE:
      case RELIABLE_SINGLE: {
        throw new UnreachableCodeException();
      }
      case RELIABLE_FRAGMENT_INITIAL: {
        throw new UnimplementedCodeException();
      }
      case RELIABLE_FRAGMENT_SUCCESSOR: {
        throw new UnimplementedCodeException();
      }
    }
  }
}

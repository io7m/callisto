package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public final class CoPipelineFragmentsPending
{
  private final PublishSubject<CoPipelineMessageReliable> messages;

  public CoPipelineFragmentsPending(
    final Observable<CoPipelinePacketReliableFragmentType> fragments)
  {
    NullCheck.notNull(fragments, "Fragments")
      .subscribe(this::onFragment);
    this.messages = PublishSubject.create();
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

  public Observable<CoPipelineMessageReliable> messages()
  {
    return this.messages;
  }

  public void poll()
  {

  }
}

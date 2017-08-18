package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.callisto.prototype0.transport.messages.CoMessage;
import com.io7m.jnull.NullCheck;
import com.io7m.jserial.core.SerialNumberIntType;
import com.io7m.junreachable.UnreachableCodeException;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import java.util.List;

public final class CoPipelineReliableSequenceTracker
{
  private final PublishSubject<CoPipelineMessageReliable> messages;
  private final PublishSubject<CoPipelinePacketReliableFragmentType> fragments;
  private final SerialNumberIntType serial;
  private final CoPipelineErrorsType errors;
  private final PublishSubject<CoPipelineReliableSequenceTrackerEventType> events;
  private final CoPipelineReliableSequenceTrackerWindow window;

  private CoPipelineReliableSequenceTracker(
    final SerialNumberIntType in_serial,
    final CoPipelineErrorsType in_errors,
    final Observable<CoPipelinePoll> in_poll,
    final PublishSubject<CoPipelineReliableSequenceTrackerEventType> in_events,
    final PublishSubject<CoPipelineMessageReliable> in_messages,
    final PublishSubject<CoPipelinePacketReliableFragmentType> in_fragments,
    final Observable<CoPipelinePacketReliableType> in_reliables)
  {
    this.events =
      NullCheck.notNull(in_events, "in_events");
    this.serial =
      NullCheck.notNull(in_serial, "Serial");
    this.errors =
      NullCheck.notNull(in_errors, "Errors");
    this.messages =
      NullCheck.notNull(in_messages, "Messages");
    this.fragments =
      NullCheck.notNull(in_fragments, "Fragments");
    this.window =
      new CoPipelineReliableSequenceTrackerWindow(in_serial, 0);

    NullCheck.notNull(in_reliables, "Reliables")
      .subscribe(this::onReliable);
    NullCheck.notNull(in_poll, "Polls")
      .subscribe(this::onPoll);
  }

  public static CoPipelineReliableSequenceTracker create(
    final SerialNumberIntType serial,
    final CoPipelineErrorsType errors,
    final Observable<CoPipelinePoll> poll,
    final Observable<CoPipelinePacketReliableType> reliables)
  {
    NullCheck.notNull(reliables, "Reliables");

    final PublishSubject<CoPipelineMessageReliable> messages =
      PublishSubject.create();
    final PublishSubject<CoPipelinePacketReliableFragmentType> fragments =
      PublishSubject.create();
    final PublishSubject<CoPipelineReliableSequenceTrackerEventType> events =
      PublishSubject.create();

    return new CoPipelineReliableSequenceTracker(
      serial, errors, poll, events, messages, fragments, reliables);
  }

  private void onPoll(
    final CoPipelinePoll poll)
  {

  }

  public Observable<CoPipelineReliableSequenceTrackerEventType> events()
  {
    return this.events;
  }

  private void onReliable(
    final CoPipelinePacketReliableType p)
  {
    final int sequence_incoming = p.sequence();

    final boolean r = this.window.receive(sequence_incoming);
    if (!r) {
      this.events.onNext(
        CoPipelineReliableSequenceTrackerEventDropped.of(sequence_incoming));
      return;
    }

    switch (p.type()) {
      case UNRELIABLE: {
        throw new UnreachableCodeException();
      }
      case RELIABLE_SINGLE: {
        this.onReliableSingle(
          (CoPipelinePacketReliableSingle) p);
        return;
      }
      case RELIABLE_FRAGMENT_INITIAL: {
        this.onReliableFragmentInitial(
          (CoPipelinePacketReliableFragmentInitial) p);
        return;
      }
      case RELIABLE_FRAGMENT_SUCCESSOR: {
        this.onReliableFragmentSuccessor(
          (CoPipelinePacketReliableFragmentSuccessor) p);
        return;
      }
    }
  }

  private void onReliableFragmentSuccessor(
    final CoPipelinePacketReliableFragmentSuccessor p)
  {
    this.fragments.onNext(
      CoPipelinePacketReliableFragmentSuccessor.of(
        p.sequence(),
        p.fragmentID(),
        p.fragmentIndex(),
        p.fragmentData()));
  }

  private void onReliableFragmentInitial(
    final CoPipelinePacketReliableFragmentInitial p)
  {
    this.fragments.onNext(
      CoPipelinePacketReliableFragmentInitial.of(
        p.sequence(),
        p.fragmentID(),
        p.fragmentCount(),
        p.fragmentMessageSizeTotal(),
        p.fragmentMessageType(),
        p.fragmentMessageData()));
  }

  private void onReliableSingle(
    final CoPipelinePacketReliableSingle p)
  {
    final List<CoMessage> m = p.messages();
    for (int index = 0; index < m.size(); ++index) {
      this.messages.onNext(CoPipelineMessageReliable.of(m.get(index)));
    }
  }

  public Observable<CoPipelinePacketReliableFragmentType> fragments()
  {
    return this.fragments;
  }

  public Observable<CoPipelineMessageReliable> messages()
  {
    return this.messages;
  }
}

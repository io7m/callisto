package com.io7m.callisto.tests.prototype0;

import com.io7m.callisto.prototype0.transport.pipeline.CoPipelineErrorsType;
import com.io7m.callisto.prototype0.transport.pipeline.CoPipelinePacketReliableSingle;
import com.io7m.callisto.prototype0.transport.pipeline.CoPipelinePacketReliableType;
import com.io7m.callisto.prototype0.transport.pipeline.CoPipelinePoll;
import com.io7m.callisto.prototype0.transport.pipeline.CoPipelineReliableSequenceTracker;
import com.io7m.callisto.prototype0.transport.pipeline.CoPipelineReliableSequenceTrackerEventDropped;
import com.io7m.callisto.prototype0.transport.pipeline.CoPipelineReliableSequenceTrackerEventType;
import com.io7m.jserial.core.SerialNumber24;
import com.io7m.jserial.core.SerialNumberIntType;
import io.reactivex.subjects.PublishSubject;
import io.vavr.collection.List;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.junit.Test;

import java.util.function.Consumer;

public final class CoPipelineReliableSequenceTrackerTest
{
  @Test
  public void testDropDuplicate(
    final @Mocked CoPipelineErrorsType errors,
    final @Mocked Consumer<CoPipelineReliableSequenceTrackerEventType> event_consumer)
  {
    final SerialNumberIntType serial =
      SerialNumber24.get();
    final PublishSubject<CoPipelinePoll> poll =
      PublishSubject.create();
    final PublishSubject<CoPipelinePacketReliableType> reliables =
      PublishSubject.create();

    final CoPipelineReliableSequenceTracker tr =
      CoPipelineReliableSequenceTracker.create(serial, errors, poll, reliables);

    tr.events().subscribe(event_consumer::accept);

    new StrictExpectations()
    {{
      event_consumer.accept(
        CoPipelineReliableSequenceTrackerEventDropped.of(0));
    }};

    reliables.onNext(CoPipelinePacketReliableSingle.of(0, List.empty()));
    reliables.onNext(CoPipelinePacketReliableSingle.of(0, List.empty()));
  }
}

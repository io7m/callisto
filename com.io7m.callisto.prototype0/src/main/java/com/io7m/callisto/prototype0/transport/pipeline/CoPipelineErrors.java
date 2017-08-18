package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.jnull.NullCheck;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public final class CoPipelineErrors implements CoPipelineErrorsType
{
  private final PublishSubject<CoPipelineError> errors;

  private CoPipelineErrors(
    final PublishSubject<CoPipelineError> in_errors)
  {
    this.errors = NullCheck.notNull(in_errors, "Errors");
  }

  public static CoPipelineErrorsType create()
  {
    return new CoPipelineErrors(PublishSubject.create());
  }

  @Override
  public void emit(
    final CoPipelineError error)
  {
    this.errors.onNext(NullCheck.notNull(error, "Error"));
  }

  @Override
  public Observable<CoPipelineError> errors()
  {
    return this.errors;
  }
}

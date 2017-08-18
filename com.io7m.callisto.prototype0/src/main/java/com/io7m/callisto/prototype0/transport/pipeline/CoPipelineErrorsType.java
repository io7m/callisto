package com.io7m.callisto.prototype0.transport.pipeline;

import io.reactivex.Observable;

public interface CoPipelineErrorsType
{
  void emit(CoPipelineError error);

  Observable<CoPipelineError> errors();
}

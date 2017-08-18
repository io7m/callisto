package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.callisto.core.CoImmutableStyleType;
import org.immutables.value.Value;

@CoImmutableStyleType
@Value.Immutable
public interface CoPipelineErrorType
{
  enum Severity {
    WARNING,
    FATAL
  }

  @Value.Parameter
  Severity severity();

  @Value.Parameter
  String message();
}

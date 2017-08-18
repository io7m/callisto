package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.callisto.prototype0.transport.messages.CoMessage;

public interface CoPipelineMessageType
{
  enum Type {
    RELIABLE,
    UNRELIABLE
  }

  Type type();

  CoMessage message();
}

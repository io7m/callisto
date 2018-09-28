package com.io7m.callisto.client.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Thing
{
  private static final Logger LOG = LoggerFactory.getLogger(Thing.class);

  private Thing()
  {
    LOG.debug("created Thing");
  }
}

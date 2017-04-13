/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.callisto.container;

import org.apache.felix.framework.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;

/**
 * An SLF4J logger for the Felix framework.
 */

final class CoFelixLogger extends Logger
{
  private static final org.slf4j.Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoContainer.class);
  }

  CoFelixLogger()
  {

  }

  @Override
  protected void doLog(
    final Bundle bundle,
    final ServiceReference sr,
    final int level,
    final String msg,
    final Throwable throwable)
  {
    switch (level) {
      case Logger.LOG_DEBUG: {
        LOG.debug("[{}]: {}: ", bundle.getSymbolicName(), msg, throwable);
        break;
      }
      case Logger.LOG_ERROR: {
        LOG.error("[{}]: {}: ", bundle.getSymbolicName(), msg, throwable);
        break;
      }
      case Logger.LOG_INFO: {
        LOG.info("[{}]: {}: ", bundle.getSymbolicName(), msg, throwable);
        break;
      }
      case Logger.LOG_WARNING: {
        LOG.warn("[{}]: {}: ", bundle.getSymbolicName(), msg, throwable);
        break;
      }
      default: {
        LOG.trace("[{}]: {}: ", bundle.getSymbolicName(), msg, throwable);
        break;
      }
    }
  }

  @Override
  protected void doLog(
    final int level,
    final String msg,
    final Throwable throwable)
  {
    switch (level) {
      case Logger.LOG_DEBUG: {
        LOG.debug("{}: ", msg, throwable);
        break;
      }
      case Logger.LOG_ERROR: {
        LOG.error("{}: ", msg, throwable);
        break;
      }
      case Logger.LOG_INFO: {
        LOG.info("{}: ", msg, throwable);
        break;
      }
      case Logger.LOG_WARNING: {
        LOG.warn("{}: ", msg, throwable);
        break;
      }
      default: {
        LOG.trace("{}: ", msg, throwable);
        break;
      }
    }
  }
}

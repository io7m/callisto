/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.callisto.container;

import com.io7m.jnull.NullCheck;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mindlessly simple SLF4J log service.
 */

public final class CoLogService implements LogService
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoLogService.class);
  }

  private final Bundle bundle;

  CoLogService(
    final Bundle in_bundle)
  {
    this.bundle = NullCheck.notNull(in_bundle, "Bundle");

    LOG.debug("creating log service for {}", this.bundle.getSymbolicName());
  }

  @Override
  public void log(
    final int level,
    final String message)
  {
    final String name = this.bundle.getSymbolicName();
    switch (level) {
      case LogService.LOG_DEBUG: {
        LOG.debug("[{}]: {}", name, message);
        break;
      }
      case LogService.LOG_ERROR: {
        LOG.error("[{}]: {}", name, message);
        break;
      }
      case LogService.LOG_INFO: {
        LOG.info("[{}]: {}", name, message);
        break;
      }
      case LogService.LOG_WARNING: {
        LOG.warn("[{}]: {}", name, message);
        break;
      }
      default: {
        LOG.warn("[{}]: {}", name, message);
        break;
      }
    }
  }

  @Override
  public void log(
    final int level,
    final String message,
    final Throwable exception)
  {
    final String name = this.bundle.getSymbolicName();
    switch (level) {
      case LogService.LOG_DEBUG: {
        LOG.debug("[{}]: {}: ", name, message, exception);
        break;
      }
      case LogService.LOG_ERROR: {
        LOG.error("[{}]: {}: ", name, message, exception);
        break;
      }
      case LogService.LOG_INFO: {
        LOG.info("[{}]: {}: ", name, message, exception);
        break;
      }
      case LogService.LOG_WARNING: {
        LOG.warn("[{}]: {}: ", name, message, exception);
        break;
      }
      default: {
        LOG.warn("[{}]: {}: ", name, message, exception);
        break;
      }
    }
  }

  @Override
  public void log(
    final ServiceReference sr,
    final int level,
    final String message)
  {
    if (sr == null) {
      this.log(level, message);
      return;
    }

    final String name = sr.getBundle().getSymbolicName();
    switch (level) {
      case LogService.LOG_DEBUG: {
        LOG.debug("[{}]: {}", name, message);
        break;
      }
      case LogService.LOG_ERROR: {
        LOG.error("[{}]: {}", name, message);
        break;
      }
      case LogService.LOG_INFO: {
        LOG.info("[{}]: {}", name, message);
        break;
      }
      case LogService.LOG_WARNING: {
        LOG.warn("[{}]: {}", name, message);
        break;
      }
      default: {
        LOG.warn("[{}]: {}", name, message);
        break;
      }
    }
  }

  @Override
  public void log(
    final ServiceReference sr,
    final int level,
    final String message,
    final Throwable exception)
  {
    if (sr == null) {
      this.log(level, message, exception);
      return;
    }

    final String name = sr.getBundle().getSymbolicName();
    switch (level) {
      case LogService.LOG_DEBUG: {
        LOG.debug("[{}]: {}: ", name, message, exception);
        break;
      }
      case LogService.LOG_ERROR: {
        LOG.error("[{}]: {}: ", name, message, exception);
        break;
      }
      case LogService.LOG_INFO: {
        LOG.info("[{}]: {}: ", name, message, exception);
        break;
      }
      case LogService.LOG_WARNING: {
        LOG.warn("[{}]: {}: ", name, message, exception);
        break;
      }
      default: {
        LOG.warn("[{}]: {}: ", name, message, exception);
        break;
      }
    }
  }
}

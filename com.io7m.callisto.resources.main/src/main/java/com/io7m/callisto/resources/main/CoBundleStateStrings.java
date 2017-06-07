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

package com.io7m.callisto.resources.main;

import com.io7m.junreachable.UnreachableCodeException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

final class CoBundleStateStrings
{
  private CoBundleStateStrings()
  {
    throw new UnreachableCodeException();
  }

  static String stateName(
    final int state)
  {
    switch (state) {
      case Bundle.ACTIVE:
        return "active";
      case Bundle.INSTALLED:
        return "installed";
      case Bundle.RESOLVED:
        return "resolved";
      case Bundle.STARTING:
        return "starting";
      case Bundle.STOPPING:
        return "stopping";
      case Bundle.UNINSTALLED:
        return "uninstalled";
      default:
        return "unknown";
    }
  }

  static String eventName(
    final int state)
  {
    switch (state) {
      case BundleEvent.INSTALLED:
        return "installed";
      case BundleEvent.LAZY_ACTIVATION:
        return "lazy activation";
      case BundleEvent.RESOLVED:
        return "resolved";
      case BundleEvent.STARTED:
        return "started";
      case BundleEvent.STARTING:
        return "starting";
      case BundleEvent.STOPPED:
        return "stopped";
      case BundleEvent.STOPPING:
        return "stopping";
      case BundleEvent.UNINSTALLED:
        return "uninstalled";
      case BundleEvent.UNRESOLVED:
        return "unresolved";
      case BundleEvent.UPDATED:
        return "updated";
      default:
        return "unknown";
    }
  }
}

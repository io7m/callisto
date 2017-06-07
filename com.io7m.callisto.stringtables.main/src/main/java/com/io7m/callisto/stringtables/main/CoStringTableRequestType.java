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

package com.io7m.callisto.stringtables.main;

import com.io7m.callisto.core.CoImmutableStyleType;
import com.io7m.callisto.resources.api.CoResourceID;
import org.immutables.value.Value;
import org.osgi.framework.Bundle;

/**
 * A string table request.
 */

@CoImmutableStyleType
@Value.Immutable
public interface CoStringTableRequestType
{
  /**
   * @return The requesting bundle
   */

  @Value.Parameter
  Bundle requester();

  /**
   * @return The resource ID
   */

  @Value.Parameter
  CoResourceID resourceID();

  /**
   * @return The current language
   */

  @Value.Parameter
  String language();
}

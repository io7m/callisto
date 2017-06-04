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

package com.io7m.callisto.stringtables.api;

import com.io7m.callisto.resources.api.CoResourceException;
import com.io7m.callisto.resources.api.CoResourceID;
import org.osgi.framework.Bundle;

/**
 * The type of string table providers.
 */

public interface CoStringTableProviderType
{
  /**
   * The persistent identifier used to configure string table providers.
   */

  String CONFIGURATION_PERSISTENT_IDENTITY =
    "com.io7m.callisto.stringtables.provider";

  /**
   * Retrieve a string table.
   *
   * @param requester The requesting bundle
   * @param resource  The resource that identifies the string table
   * @param language  The language
   *
   * @return A string table
   *
   * @throws CoResourceException On failures loading the table
   */

  CoStringTableType get(
    Bundle requester,
    CoResourceID resource,
    String language)
    throws CoResourceException;

  /**
   * @return The current size of the cache in octets
   */

  long sizeUsed();

  /**
   * @return The current maximum size of the cache in octets
   */

  long sizeMaximum();
}

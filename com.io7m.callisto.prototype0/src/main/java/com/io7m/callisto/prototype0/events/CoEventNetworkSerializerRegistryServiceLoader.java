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

package com.io7m.callisto.prototype0.events;

import com.io7m.callisto.prototype0.services.CoAbstractService;
import com.io7m.jnull.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

public final class CoEventNetworkSerializerRegistryServiceLoader
  extends CoAbstractService implements CoEventNetworkSerializerRegistryType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoEventNetworkSerializerRegistryServiceLoader.class);

  private final ConcurrentSkipListMap<String, CoEventNetworkSerializerType> serializers;
  private final SortedMap<String, CoEventNetworkSerializerType> serializers_view;
  private final CoEventServiceType events;

  public CoEventNetworkSerializerRegistryServiceLoader(
    final CoEventServiceType events)
  {
    this.events = NullCheck.notNull(events, "Events");
    this.serializers = new ConcurrentSkipListMap<>();
    this.serializers_view = Collections.unmodifiableSortedMap(this.serializers);
  }

  @Override
  public void onActivate()
  {
    this.onActivateActual();

    final ServiceLoader<CoEventNetworkSerializerType> loader =
      ServiceLoader.load(CoEventNetworkSerializerType.class);

    final Iterator<CoEventNetworkSerializerType> service_iter = loader.iterator();
    while (service_iter.hasNext()) {
      final CoEventNetworkSerializerType serializer = service_iter.next();
      final String name = serializer.eventTypeName();
      if (this.serializers.containsKey(name)) {
        LOG.warn("multiple serializers registered for type: {}", name);
        LOG.warn("the last serializer registered will be used - this may give unexpected results!");
      }

      this.serializers.put(name, serializer);
      LOG.trace("registered serializer: {} -> {}", name, serializer);
    }

    final Iterator<String> name_iter = this.serializers.keySet().iterator();
    while (name_iter.hasNext()) {
      final String name = name_iter.next();
      this.events.post(CoEventNetworkSerializerRegistered.of(name));
    }
  }

  @Override
  public void shutDown()
  {

  }

  @Override
  public SortedMap<String, CoEventNetworkSerializerType> serializers()
  {
    this.checkActivated();
    return this.serializers_view;
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }
}

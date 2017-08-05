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

package com.io7m.callisto.prototype0.entities;

import com.io7m.callisto.core.CoException;
import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.idpool.CoIDPool;
import com.io7m.callisto.prototype0.services.CoAbstractService;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import io.vavr.collection.Map;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.jcip.annotations.GuardedBy;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static com.io7m.callisto.prototype0.entities.CoEntityLifecycle.ENTITY_CREATED;
import static com.io7m.callisto.prototype0.entities.CoEntityLifecycle.ENTITY_DESTROYED;
import static com.io7m.callisto.prototype0.entities.CoEntityLifecycle.ENTITY_TRAITS_CHANGED;

@Component
public final class CoEntityService
  extends CoAbstractService implements CoEntityServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoEntityService.class);

  private final Int2ReferenceOpenHashMap<Entity> entities;
  private final CoIDPool ids;
  private volatile CoEventServiceType events;

  private CoEntityService()
  {
    this.ids = new CoIDPool();
    this.entities = new Int2ReferenceOpenHashMap<>();
  }

  @Reference
  public void onEventServiceRegister(
    final CoEventServiceType in_events)
  {
    this.events = NullCheck.notNull(in_events, "Events");
  }

  @Activate
  public void onActivate()
  {
    this.onActivateActual();
  }

  @Override
  public CoEntityType createEntity(
    final CoAssemblyType assembly)
  {
    NullCheck.notNull(assembly, "Assembly");
    this.checkActivated();

    final Entity e =
      new Entity(this.events, this.ids.fresh(), this::onEntityDestroyed);

    final Map<Class<CoEntityTraitType>,
      CoEntityTraitProviderType<CoEntityTraitType>> providers =
      assembly.traitProviders();

    /*
     * Create all of the required traits.
     */

    CoException ex = null;
    for (final Class<CoEntityTraitType> c : providers.keySet()) {
      final CoEntityTraitProviderType<CoEntityTraitType> provider =
        providers.get(c).get();

      try {
        final CoEntityTraitType trait = provider.create(e);
        synchronized (e.traits_lock) {
          e.traits.put(c, trait);
        }
      } catch (final Exception t_ex) {
        ex = CoException.chain(ex, new CoEntityTraitOnCreateException(t_ex));
      }
    }

    /*
     * If any of the traits failed, destroy any that did not.
     */

    if (ex != null) {
      final ReferenceOpenHashSet<CoEntityTraitType> existing;

      synchronized (e.traits_lock) {
        existing = new ReferenceOpenHashSet<>(e.traits.values());
      }

      for (final CoEntityTraitType trait : existing) {
        try {
          trait.onDestroy();
        } catch (final Exception t_ex) {
          ex = CoException.chain(ex, new CoEntityTraitOnDestroyException(t_ex));
        }
      }

      this.ids.release(e.id.value());
      throw ex;
    }

    synchronized (this.entities) {
      this.entities.put(e.id.value(), e);
    }

    this.events.post(CoEntityLifecycleEvent.of(e, ENTITY_CREATED));
    return e;
  }

  private void onEntityDestroyed(
    final Entity entity,
    final Optional<CoException> ex_opt)
  {
    synchronized (this.entities) {
      this.entities.remove(entity.id.value());
      this.ids.release(entity.id.value());
    }

    this.events.post(CoEntityLifecycleEvent.of(entity, ENTITY_DESTROYED));
  }

  @Deactivate
  @Override
  public void shutDown()
  {
    this.checkActivated();
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }

  private static final class Entity implements CoEntityType
  {
    private final CoEntityID id;
    private final AtomicBoolean destroyed;
    private final CoEventServiceType events;
    private final BiConsumer<Entity, Optional<CoException>> on_destroy;
    private final Object traits_lock;
    private @GuardedBy("traits_lock")
    Reference2ReferenceOpenHashMap<Class<CoEntityTraitType>, CoEntityTraitType> traits;

    private Entity(
      final CoEventServiceType in_events,
      final int in_id,
      final BiConsumer<Entity, Optional<CoException>> in_on_destroy)
    {
      this.events = NullCheck.notNull(in_events, "Events");
      this.on_destroy = NullCheck.notNull(in_on_destroy, "On Destroy");
      this.id = CoEntityID.of(in_id);
      this.traits = new Reference2ReferenceOpenHashMap<>();
      this.traits_lock = new Object();
      this.destroyed = new AtomicBoolean(false);
    }

    @Override
    public boolean equals(
      final Object o)
    {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }

      final Entity entity = (Entity) o;
      return Objects.equals(this.id, entity.id);
    }

    @Override
    public String toString()
    {
      return new StringBuilder(20)
        .append("[Entity ")
        .append(Integer.toUnsignedString(this.id.value()))
        .append("]")
        .toString();
    }

    @Override
    public int hashCode()
    {
      return this.id.hashCode();
    }

    @Override
    public CoEntityID id()
    {
      return this.id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CoEntityTraitType> Optional<T> trait(
      final Class<T> c)
      throws CoEntityDestroyedException
    {
      NullCheck.notNull(c, "Class");
      this.checkDestroyed();

      synchronized (this.traits_lock) {
        final Class<CoEntityTraitType> cc = (Class<CoEntityTraitType>) c;
        return Optional.ofNullable((T) this.traits.get(cc));
      }
    }

    private void checkDestroyed()
    {
      if (this.isDestroyed()) {
        throw new CoEntityDestroyedException(
          new StringBuilder(128)
            .append("Entity has been destroyed.")
            .append(System.lineSeparator())
            .append("  Entity: ")
            .append(this)
            .append(System.lineSeparator())
            .toString());
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CoEntityTraitType> T traitGet(
      final Class<T> c)
      throws CoEntityDestroyedException, CoEntityTraitNonexistentException
    {
      NullCheck.notNull(c, "Class");
      this.checkDestroyed();

      synchronized (this.traits_lock) {
        final Class<CoEntityTraitType> cc = (Class<CoEntityTraitType>) c;
        return Optional.ofNullable((T) this.traits.get(cc))
          .orElseThrow(() -> this.traitNonexistent(c));
      }
    }

    private CoEntityTraitNonexistentException traitNonexistent(
      final Class<?> c)
    {
      return new CoEntityTraitNonexistentException(
        new StringBuilder(128)
          .append("Entity does not have a trait registered for the given class.")
          .append(System.lineSeparator())
          .append("  Entity: ")
          .append(this)
          .append(System.lineSeparator())
          .append("  Class: ")
          .append(c)
          .append(System.lineSeparator())
          .toString());
    }

    private CoEntityTraitDuplicateException traitDuplicate(
      final Class<?> c)
    {
      return new CoEntityTraitDuplicateException(
        new StringBuilder(128)
          .append("Entity already has a trait registered for the given class.")
          .append(System.lineSeparator())
          .append("  Entity: ")
          .append(this)
          .append(System.lineSeparator())
          .append("  Class: ")
          .append(c)
          .append(System.lineSeparator())
          .toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CoEntityTraitType> void traitSet(
      final Class<T> c,
      final T trait)
      throws CoEntityDestroyedException, CoEntityTraitDuplicateException
    {
      NullCheck.notNull(c, "Class");
      NullCheck.notNull(trait, "Trait");
      this.checkDestroyed();

      synchronized (this.traits_lock) {
        final Class<CoEntityTraitType> cc = (Class<CoEntityTraitType>) c;
        if (this.traits.containsKey(cc)) {
          throw this.traitDuplicate(cc);
        }
        this.traits.put(cc, trait);
      }

      this.events.post(CoEntityLifecycleEvent.of(this, ENTITY_TRAITS_CHANGED));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CoEntityTraitType> void traitDestroy(
      final Class<T> c)
      throws CoEntityDestroyedException
    {
      NullCheck.notNull(c, "Class");
      this.checkDestroyed();

      final Class<CoEntityTraitType> cc = (Class<CoEntityTraitType>) c;
      final Optional<CoEntityTraitType> tr_opt;
      synchronized (this.traits_lock) {
        tr_opt = Optional.ofNullable(this.traits.get(cc));
        this.traits.remove(cc);
      }

      if (tr_opt.isPresent()) {
        final CoEntityTraitType tr = tr_opt.get();
        try {
          tr.onDestroy();
        } catch (final Exception ex) {
          throw new UnimplementedCodeException();
        }
      }
    }

    @Override
    public boolean isDestroyed()
    {
      return this.destroyed.get();
    }

    @Override
    public void destroy()
    {
      if (this.destroyed.compareAndSet(false, true)) {
        final Set<CoEntityTraitType> t;
        synchronized (this.traits_lock) {
          t = new ReferenceOpenHashSet<>(this.traits.values());
        }

        CoException e = null;
        for (final CoEntityTraitType trait : t) {
          try {
            trait.onDestroy();
          } catch (final Exception ex) {
            e = CoException.chain(e, new CoEntityOnDestroyException(ex));
          }
        }

        this.on_destroy.accept(this, Optional.ofNullable(e));
      }
    }
  }
}

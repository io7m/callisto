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

package com.io7m.callisto.tests.prototype0;

import com.io7m.callisto.core.CoException;
import com.io7m.callisto.prototype0.entities.CoEntity;
import com.io7m.callisto.prototype0.entities.CoEntityDestroyedException;
import com.io7m.callisto.prototype0.entities.CoEntityID;
import com.io7m.callisto.prototype0.entities.CoEntityListenerType;
import com.io7m.callisto.prototype0.entities.CoEntityMessageType;
import com.io7m.callisto.prototype0.entities.CoEntityOnAttachException;
import com.io7m.callisto.prototype0.entities.CoEntityTraitAbstract;
import com.io7m.callisto.prototype0.entities.CoEntityTraitDuplicateException;
import com.io7m.callisto.prototype0.entities.CoEntityTraitType;
import io.reactivex.functions.Action;
import mockit.Delegate;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Objects;
import java.util.function.Consumer;

public final class CoEntityTest
{
  @Rule
  public final ExpectedException expected = ExpectedException.none();

  @Test
  public void testCreateDestroy(
    final @Mocked CoEntityListenerType listener,
    final @Mocked Consumer<SimpleMessage> receiver,
    final @Mocked Action on_complete)
    throws Exception
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    new StrictExpectations()
    {{
      receiver.accept(new SimpleMessage(23));
      on_complete.run();
      listener.onEntityDestroyed(e);
    }};

    Assert.assertEquals(CoEntityID.of(0L), e.id());

    e.subscribe(SimpleMessage.class, receiver);
    e.send(new SimpleMessage(23));
    e.messages().subscribe(
      m -> {
      },
      t -> {
      },
      on_complete);
    e.destroy();
    Assert.assertTrue(e.isDestroyed());
  }

  @Test
  public void testCreateDestroyRedundant(
    final @Mocked CoEntityListenerType listener,
    final @Mocked Consumer<SimpleMessage> receiver,
    final @Mocked Action on_complete)
    throws Exception
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    new StrictExpectations()
    {{
      receiver.accept(new SimpleMessage(23));
      on_complete.run();
      listener.onEntityDestroyed(e);
    }};

    Assert.assertEquals(CoEntityID.of(0L), e.id());

    e.subscribe(SimpleMessage.class, receiver);
    e.send(new SimpleMessage(23));
    e.messages().subscribe(
      m -> {
      },
      t -> {
      },
      on_complete);
    e.destroy();
    Assert.assertTrue(e.isDestroyed());
    e.destroy();
    Assert.assertTrue(e.isDestroyed());
  }

  @Test
  public void testCreateDestroyTrait(
    final @Mocked CoEntityListenerType listener,
    final @Mocked CoEntityTraitType trait)
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    new StrictExpectations()
    {{
      trait.onAttach(e);
      listener.onEntityTraitAdded(e, trait);
      trait.onDestroy();
      listener.onEntityDestroyed(e);
    }};

    e.traitSet(CoEntityTraitType.class, trait);
    Assert.assertEquals(trait, e.traitGet(CoEntityTraitType.class));
    e.destroy();
  }

  @Test
  public void testCreateTraitSetGet(
    final @Mocked CoEntityListenerType listener,
    final @Mocked CoEntityTraitType trait)
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    e.traitSet(CoEntityTraitType.class, trait);
    Assert.assertSame(e.traitGet(CoEntityTraitType.class), trait);
    Assert.assertSame(e.trait(CoEntityTraitType.class).get(), trait);
  }

  @Test
  public void testCreateTraitSetDestroyed(
    final @Mocked CoEntityListenerType listener,
    final @Mocked CoEntityTraitType trait)
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    new StrictExpectations()
    {{
      listener.onEntityDestroyed(e);
    }};

    e.destroy();
    this.expected.expect(CoEntityDestroyedException.class);
    e.traitSet(CoEntityTraitType.class, trait);
  }

  @Test
  public void testCreateDestroyTraitWithError(
    final @Mocked CoEntityListenerType listener,
    final @Mocked CoEntityTraitType trait)
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    new StrictExpectations()
    {{
      trait.onAttach(e);
      listener.onEntityTraitAdded(e, trait);
      trait.onDestroy();
      this.result = new IllegalArgumentException("CRASH!");
      listener.onEntityDestroyError(
        this.with(new Delegate<CoException>()
        {
          boolean check(final CoException e)
          {
            return Objects.equals(e.getMessage(), "CRASH!");
          }
        }));
      listener.onEntityDestroyed(e);
    }};

    e.traitSet(CoEntityTraitType.class, trait);
    Assert.assertEquals(trait, e.traitGet(CoEntityTraitType.class));
    e.destroy();
  }

  @Test
  public void testCreateTraitDuplicate(
    final @Mocked CoEntityListenerType listener,
    final @Mocked CoEntityTraitType trait)
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    new StrictExpectations()
    {{
      trait.onAttach(e);
      trait.onDestroy();
    }};

    e.traitSet(CoEntityTraitType.class, trait);
    this.expected.expect(CoEntityTraitDuplicateException.class);
    e.traitSet(CoEntityTraitType.class, trait);
  }

  @Test
  public void testCreateTraitWithError(
    final @Mocked CoEntityListenerType listener,
    final @Mocked CoEntityTraitType trait)
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    new StrictExpectations()
    {{
      trait.onAttach(e);
      this.result = new IllegalArgumentException("Crash Add!");
      trait.onDestroy();
      this.result = new IllegalArgumentException("Crash Destroy!");

      listener.onEntityTraitAddError(e, trait,
        this.with(new Delegate<CoException>()
        {
          boolean check(final CoException e)
          {
            return e.getMessage().contains("Crash Add!");
          }
        }));

      listener.onEntityTraitAdded(e, trait);
    }};

    this.expected.expect(CoEntityOnAttachException.class);
    this.expected.expectMessage(StringContains.containsString("Crash Add!"));
    e.traitSet(CoEntityTraitType.class, trait);
  }

  @Test
  public void testCreateTraitDestroyTrait(
    final @Mocked CoEntityListenerType listener,
    final @Mocked CoEntityTraitType trait)
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    new StrictExpectations()
    {{
      trait.onAttach(e);
      listener.onEntityTraitAdded(e, trait);
      trait.onDestroy();
      listener.onEntityTraitDestroyed(e, trait);
    }};

    e.traitSet(CoEntityTraitType.class, trait);
    Assert.assertEquals(trait, e.traitGet(CoEntityTraitType.class));
    e.traitDestroy(CoEntityTraitType.class);
    Assert.assertFalse(e.trait(CoEntityTraitType.class).isPresent());
  }

  @Test
  public void testCreateTraitDestroyTraitWithError(
    final @Mocked CoEntityListenerType listener,
    final @Mocked CoEntityTraitType trait)
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    new StrictExpectations()
    {{
      trait.onAttach(e);
      listener.onEntityTraitAdded(e, trait);
      trait.onDestroy();
      this.result = new IllegalArgumentException("CRASH!");

      listener.onEntityTraitDestroyError(
        e, trait, this.with(new Delegate<CoException>()
      {
        boolean check(final CoException ex) {
          return ex.getMessage().contains("CRASH!");
        }
      }));
    }};

    e.traitSet(CoEntityTraitType.class, trait);
    Assert.assertEquals(trait, e.traitGet(CoEntityTraitType.class));
    e.traitDestroy(CoEntityTraitType.class);
    Assert.assertFalse(e.trait(CoEntityTraitType.class).isPresent());
  }

  @Test
  public void testCreateTraitDestroyTraitNonexistent(
    final @Mocked CoEntityListenerType listener)
  {
    final CoEntity e = CoEntity.create(listener, CoEntityID.of(0L));
    Assert.assertFalse(e.isDestroyed());

    new StrictExpectations()
    {{

    }};

    Assert.assertFalse(e.trait(CoEntityTraitType.class).isPresent());
    e.traitDestroy(CoEntityTraitType.class);
    Assert.assertFalse(e.trait(CoEntityTraitType.class).isPresent());
  }

  @Test
  public void testEquals(
    final @Mocked CoEntityListenerType listener)
  {
    final CoEntity e0 = CoEntity.create(listener, CoEntityID.of(0L));
    final CoEntity e1 = CoEntity.create(listener, CoEntityID.of(0L));
    final CoEntity e2 = CoEntity.create(listener, CoEntityID.of(2L));
    Assert.assertEquals(e0, e0);
    Assert.assertEquals(e0, e1);
    Assert.assertNotEquals(e0, e2);
    Assert.assertNotEquals(e0, null);
    Assert.assertNotEquals(e0, Integer.valueOf(23));

    Assert.assertEquals((long) e0.hashCode(), (long) e0.hashCode());
  }

  private static final class SimpleMessage implements CoEntityMessageType
  {
    private final int x;

    SimpleMessage(
      final int in_x)
    {
      this.x = in_x;
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

      final SimpleMessage that = (SimpleMessage) o;
      return this.x == that.x;
    }

    @Override
    public int hashCode()
    {
      return this.x;
    }
  }

  private static final class SimpleTrait0 extends CoEntityTraitAbstract
  {
    SimpleTrait0()
    {

    }

    @Override
    public void onDestroy()
      throws CoException
    {

    }
  }

  private static final class SimpleTrait1 extends CoEntityTraitAbstract
  {
    SimpleTrait1()
    {

    }

    @Override
    public void onDestroy()
      throws CoException
    {

    }
  }
}

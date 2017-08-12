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

package com.io7m.callisto.prototype0.stringconstants;

import com.io7m.jnull.NullCheck;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.jcip.annotations.GuardedBy;

import java.util.Optional;

public final class CoStringConstantPool implements CoStringConstantPoolType
{
  private final Object lock;
  private final @GuardedBy("lock") Object2IntOpenHashMap<String> text_to_int;
  private final @GuardedBy("lock") Int2ReferenceRBTreeMap<String> int_to_text;
  private final ListenerType listener;

  public CoStringConstantPool(
    final ListenerType in_listener)
  {
    this.listener = NullCheck.notNull(in_listener, "Listener");
    this.lock = new Object();
    this.int_to_text = new Int2ReferenceRBTreeMap<>();
    this.text_to_int = new Object2IntOpenHashMap<>();
  }

  @Override
  public Optional<String> lookupString(
    final CoStringConstantReference r)
  {
    NullCheck.notNull(r, "Reference");

    synchronized (this.lock) {
      final int k = r.value();
      if (this.int_to_text.containsKey(k)) {
        return Optional.of(this.int_to_text.get(k));
      }
    }

    return Optional.empty();
  }

  @Override
  public Optional<CoStringConstantReference> lookupReference(
    final String text)
  {
    NullCheck.notNull(text, "Text");

    synchronized (this.lock) {
      if (this.text_to_int.containsKey(text)) {
        return Optional.of(CoStringConstantReference.of(
          this.text_to_int.getInt(text)));
      }
    }

    return Optional.empty();
  }

  @Override
  public Int2ReferenceMap<String> view()
  {
    synchronized (this.lock) {
      return new Int2ReferenceOpenHashMap<>(this.int_to_text);
    }
  }

  @Override
  public CoStringConstantPoolUpdateType newUpdate()
  {
    return new Update(this);
  }

  @Override
  public CoStringConstantReference add(
    final String text)
  {
    NullCheck.notNull(text, "Text");

    try {
      synchronized (this.lock) {
        final int k = this.int_to_text.lastIntKey() + 1;
        this.int_to_text.put(k, text);
        this.text_to_int.put(text, k);
        return CoStringConstantReference.of(k);
      }
    } finally {
      this.listener.onUpdate();
    }
  }

  public interface ListenerType
  {
    void onUpdate();
  }

  private static final class Update implements CoStringConstantPoolUpdateType
  {
    private final CoStringConstantPool owner;
    private final Int2ReferenceOpenHashMap<String> update_int_to_text =
      new Int2ReferenceOpenHashMap<>();
    private final Object2IntOpenHashMap<String> update_text_to_int =
      new Object2IntOpenHashMap<>();

    Update(final CoStringConstantPool in_owner)
    {
      this.owner = in_owner;
    }

    @Override
    public CoStringConstantPoolUpdateType set(
      final int index,
      final String value)
    {
      final String text = NullCheck.notNull(value, "Value");
      this.update_int_to_text.put(index, text);
      this.update_text_to_int.put(text, index);
      return this;
    }

    @Override
    public void execute()
    {
      synchronized (this.owner.lock) {
        this.owner.int_to_text.putAll(this.update_int_to_text);
        this.owner.text_to_int.putAll(this.update_text_to_int);
        this.update_int_to_text.clear();
        this.update_text_to_int.clear();
      }

      this.owner.listener.onUpdate();
    }
  }
}

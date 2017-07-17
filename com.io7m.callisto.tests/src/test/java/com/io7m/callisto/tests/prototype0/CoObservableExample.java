package com.io7m.callisto.tests.prototype0;

import io.reactivex.subjects.PublishSubject;

public final class CoObservableExample
{
  private CoObservableExample()
  {

  }

  interface T
  {

  }

  static final class A implements T
  {

  }

  static final class B implements T
  {

  }

  public static void main(
    final String[] args)
  {
    final PublishSubject<T> s = PublishSubject.create();
    s.ofType(T.class).subscribe(x -> System.out.println("T: " + x));
    s.ofType(A.class).subscribe(x -> System.out.println("A: " + x));
    s.ofType(A.class).subscribe(x -> { throw new IllegalArgumentException("BOOM!"); });
    s.ofType(B.class).subscribe(x -> System.out.println("B: " + x));

    s.onNext(new A());
    s.onNext(new B());
    s.onNext(new T() {});
  }
}

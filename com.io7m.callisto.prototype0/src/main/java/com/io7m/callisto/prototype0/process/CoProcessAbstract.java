package com.io7m.callisto.prototype0.process;

import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.jnull.NullCheck;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public abstract class CoProcessAbstract implements CoProcessType
{
  private final ExecutorService exec;
  private final CoEventServiceType events;
  private final CoProcessSupervisorEventResponse response;
  private final Scheduler scheduler;
  private volatile State state;
  private Disposable sub_supervisor;

  protected CoProcessAbstract(
    final CoEventServiceType in_events,
    final ThreadFactory factory)
  {
    this.events =
      NullCheck.notNull(in_events, "Events");
    this.exec =
      Executors.newSingleThreadExecutor(
        NullCheck.notNull(factory, "Factory"));
    this.scheduler =
      Schedulers.from(this.exec);

    this.response =
      CoProcessSupervisorEventResponse.of(this);

    this.state = State.PROCESS_UNINITIALIZED;
  }

  private static Future<Void> failedFuture()
  {
    final CompletableFuture<Void> f = new CompletableFuture<>();
    f.completeExceptionally(new IllegalStateException("Already stopped"));
    return f;
  }

  protected abstract Logger log();

  protected abstract void doInitialize();

  protected abstract void doStart();

  protected abstract void doStop();

  protected abstract void doDestroy();

  protected final ExecutorService executor()
  {
    return this.exec;
  }

  protected final Scheduler scheduler()
  {
    return this.scheduler;
  }

  protected final CoEventServiceType events()
  {
    return this.events;
  }

  @Override
  public final Future<Void> initialize()
  {
    if (this.exec.isShutdown()) {
      return CompletableFuture.completedFuture(null);
    }

    this.sub_supervisor = this.events().events()
      .ofType(CoProcessSupervisorEventRequest.class)
      .subscribeOn(this.scheduler)
      .subscribe(this::onSupervisorRequest);

    return this.exec.submit(() -> {
      switch (this.state) {
        case PROCESS_UNINITIALIZED: {
          this.doInitialize();
          this.state = State.PROCESS_INITIALIZED;
          break;
        }
        case PROCESS_INITIALIZED:
        case PROCESS_STOPPED:
        case PROCESS_DESTROYED:
        case PROCESS_STARTED: {
          break;
        }
      }

      return null;
    });
  }

  private void onSupervisorRequest(
    final CoProcessSupervisorEventRequest event)
  {
    this.events.post(this.response);
  }

  @Override
  public final String toString()
  {
    return String.format("[%s]", this.name());
  }

  @Override
  public final Future<Void> start()
  {
    if (this.exec.isShutdown()) {
      return failedFuture();
    }

    return this.exec.submit(() -> {
      switch (this.state) {
        case PROCESS_UNINITIALIZED:
        case PROCESS_STOPPED:
        case PROCESS_DESTROYED:
        case PROCESS_STARTED: {
          break;
        }
        case PROCESS_INITIALIZED: {
          this.doStart();
          this.state = State.PROCESS_STARTED;
          break;
        }
      }
      return null;
    });
  }

  @Override
  public final Future<Void> stop()
  {
    if (this.exec.isShutdown()) {
      return failedFuture();
    }

    return this.exec.submit(() -> {
      switch (this.state) {
        case PROCESS_UNINITIALIZED:
        case PROCESS_STOPPED:
        case PROCESS_DESTROYED: {
          break;
        }
        case PROCESS_INITIALIZED:
        case PROCESS_STARTED: {
          this.sub_supervisor.dispose();
          this.doStop();
          this.state = State.PROCESS_STOPPED;
          break;
        }
      }

      return null;
    });
  }

  @Override
  public final Future<Void> destroy()
  {
    if (this.exec.isShutdown()) {
      return failedFuture();
    }

    return this.exec.submit(() -> {
      try {
        switch (this.state) {
          case PROCESS_UNINITIALIZED:
          case PROCESS_INITIALIZED:
          case PROCESS_STARTED:
          case PROCESS_DESTROYED: {
            break;
          }
          case PROCESS_STOPPED: {
            this.doDestroy();
            this.state = State.PROCESS_DESTROYED;
            break;
          }
        }
        return null;
      } finally {
        this.exec.shutdown();
      }
    });
  }

  private enum State
  {
    PROCESS_UNINITIALIZED,
    PROCESS_INITIALIZED,
    PROCESS_STARTED,
    PROCESS_STOPPED,
    PROCESS_DESTROYED
  }
}

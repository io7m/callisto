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

package com.io7m.callisto.prototype0.client;

import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.process.CoProcessAbstract;
import io.reactivex.disposables.Disposable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CoClientRendering extends CoProcessAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoClientRendering.class);

  private final ExecutorService exec_back;
  private final Disposable tick_sub;
  private volatile long context_rendering;
  private volatile long context_background;

  public CoClientRendering(
    final CoEventServiceType in_events)
  {
    super(
      in_events,
      r -> {
        final Thread th = new Thread(r);
        th.setName("com.io7m.callisto.client.rendering.main." + th.getId());
        return th;
      });

    this.exec_back =
      Executors.newSingleThreadExecutor(
        r -> {
          final Thread th = new Thread(r);
          th.setName("com.io7m.callisto.client.rendering.back." + th.getId());
          return th;
        });

    this.tick_sub =
      this.events().events()
        .ofType(CoClientTickEvent.class)
        .subscribeOn(this.scheduler())
        .subscribe(this::onTickEvent, CoClientRendering::onTickEventError);
  }

  private static void onTickEventError(
    final Throwable ex)
  {
    LOG.error("onTickEvent: ", ex);
  }

  private void onTickEvent(
    final CoClientTickEvent event)
  {
    GLFW.glfwPollEvents();

    if (GLFW.glfwWindowShouldClose(this.context_rendering)) {
      LOG.debug("window close requested");
    }
  }

  @Override
  public String name()
  {
    return "rendering";
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }

  @Override
  protected void doInitialize()
  {
    LOG.debug("initialize");

    GLFWErrorCallback.create(
      (error, description) ->
        LOG.error(
          "[{}]: {}",
          Integer.valueOf(error),
          GLFWErrorCallback.getDescription(description))).set();

    if (!GLFW.glfwInit()) {
      throw new IllegalStateException("Unable to initialize GLFW");
    }

    LOG.debug("creating main window");

    GLFW.glfwDefaultWindowHints();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
    GLFW.glfwWindowHint(
      GLFW.GLFW_OPENGL_PROFILE,
      GLFW.GLFW_OPENGL_CORE_PROFILE);

    this.context_rendering =
      GLFW.glfwCreateWindow(
        640,
        480,
        "Callisto",
        MemoryUtil.NULL,
        MemoryUtil.NULL);

    LOG.debug("creating back window");

    GLFW.glfwDefaultWindowHints();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
    GLFW.glfwWindowHint(
      GLFW.GLFW_OPENGL_PROFILE,
      GLFW.GLFW_OPENGL_CORE_PROFILE);

    this.context_background =
      GLFW.glfwCreateWindow(
        2,
        2,
        "Callisto-Background",
        MemoryUtil.NULL,
        this.context_rendering);

    this.executor().execute(this::clearMainWindow);
    this.exec_back.execute(this::clearBackWindow);
  }

  @Override
  protected void doStart()
  {
    LOG.debug("start");
  }

  @Override
  protected void doStop()
  {
    LOG.debug("stop");
    this.tick_sub.dispose();
    this.stopMain();
    this.exec_back.submit(this::stopBackground);
  }

  @Override
  protected void doDestroy()
  {
    LOG.debug("destroy");
    this.exec_back.shutdown();
  }

  private void clearBackWindow()
  {
    LOG.debug("clearing back");
    GLFW.glfwMakeContextCurrent(this.context_background);
    GL.createCapabilities();
    GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    GLFW.glfwSwapBuffers(this.context_background);
  }

  private void clearMainWindow()
  {
    LOG.debug("clearing main");
    GLFW.glfwMakeContextCurrent(this.context_rendering);
    GL.createCapabilities();
    GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    GLFW.glfwSwapBuffers(this.context_rendering);
  }

  private void stopMain()
  {
    GLFW.glfwDestroyWindow(this.context_rendering);
    this.context_rendering = MemoryUtil.NULL;
  }

  private void stopBackground()
  {
    GLFW.glfwDestroyWindow(this.context_background);
    this.context_background = MemoryUtil.NULL;
  }
}

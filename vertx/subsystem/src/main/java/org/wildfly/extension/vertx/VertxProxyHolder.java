/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxProxyHolder {
  private static final VertxProxyHolder INSTANCE = new VertxProxyHolder();

  public static VertxProxyHolder instance() {
    return INSTANCE;
  }

  private final AtomicReference<VertxProxy> vertxProxyRef = new AtomicReference<>();

  private VertxProxyHolder() {}

  void instrument(VertxProxy vertxProxy) {
    this.vertxProxyRef.set(vertxProxy);
  }

  void release() {
    this.vertxProxyRef.set(null);
  }

  public VertxProxy getVertxProxy() {
    return this.vertxProxyRef.get();
  }

}

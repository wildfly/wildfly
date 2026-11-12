/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import io.vertx.core.VertxOptions;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class NamedVertxOptions {

  public static final NamedVertxOptions DEFAULT = new NamedVertxOptions("", new VertxOptions());

  /** The name of the configured VertxOptions **/
  private final String name;

  /** The VertxOptions used to construct the Vertx instance **/
  private final VertxOptions vertxOptions;

  public NamedVertxOptions(String name, VertxOptions vertxOptions) {
    this.name = name;
    this.vertxOptions = vertxOptions;
  }

  public String getName() {
    return name;
  }

  public VertxOptions getVertxOptions() {
    return vertxOptions;
  }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A central place to register the defined VertxOptions.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public final class VertxOptionsRegistry {

  // All cached NamedVertxOptions
  private final Map<String, NamedVertxOptions> namedVertxOptionsMap = new ConcurrentHashMap<>();

  private VertxOptionsRegistry() {}

  private static final VertxOptionsRegistry INSTANCE = new VertxOptionsRegistry();
  public static VertxOptionsRegistry getInstance() {
    return INSTANCE;
  }

  /**
   * Gets the NamedVertxOptions by the name.
   *
   * @param name the defined name for the NamedVertxOptions
   * @return the NamedVertxOptions
   */
  public NamedVertxOptions getNamedVertxOptions(String name) {
    return namedVertxOptionsMap.get(name);
  }

  /**
   * Adds a NamedVertxOptions into this registry.
   *
   * @param namedVertxOptions the NamedVertxOptions to add
   */
  public void addVertxOptions(NamedVertxOptions namedVertxOptions) {
    this.namedVertxOptionsMap.put(namedVertxOptions.getName(), namedVertxOptions);
  }

  /**
   * Removes a NamedVertxOption by its name
   *
   * @param name the name of the NamedVertxOptions to remove
   */
  public void removeVertxOptions(String name) {
    this.namedVertxOptionsMap.remove(name);
  }

}

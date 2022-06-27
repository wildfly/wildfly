/*
 *  Copyright (c) 2022 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
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

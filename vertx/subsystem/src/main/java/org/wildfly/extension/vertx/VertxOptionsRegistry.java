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

import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A central place to register the defined VertxOptions and its sub configurations.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public final class VertxOptionsRegistry {

  // All cached NamedVertxOptions
  private final Map<String, NamedVertxOptions> namedVertxOptionsMap = new ConcurrentHashMap<>();
  // All defined AddressResolverOptions
  private final Map<String, AddressResolverOptions> addressResolverOptionsMap = new ConcurrentHashMap<>();
  // All defined EventBusOptions
  private final Map<String, EventBusOptions> eventBusOptionsMap = new ConcurrentHashMap<>();
  // All defined cluster-node-metata in JsonObject
  private final Map<String, JsonObject> clusterNodeMetaMap = new ConcurrentHashMap<>();

  private VertxOptionsRegistry() {}

  private static VertxOptionsRegistry INSTANCE = new VertxOptionsRegistry();
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

  public AddressResolverOptions getAddressResolverOptions(String name) {
    return this.addressResolverOptionsMap.get(name);
  }

  public void addAddressResolverOptions(String name, AddressResolverOptions addressOptions) {
    this.addressResolverOptionsMap.put(name, addressOptions);
  }

  public void removeAddressResolverOptions(String name) {
    this.addressResolverOptionsMap.remove(name);
  }

  public EventBusOptions getEventBusOptions(String name) {
    return this.eventBusOptionsMap.get(name);
  }

  public void addEventBusOptions(String name, EventBusOptions eventBusOptions) {
    this.eventBusOptionsMap.put(name, eventBusOptions);
  }

  public void removeEventBusOptions(String name) {
    this.eventBusOptionsMap.remove(name);
  }

  public JsonObject getClusterNodeMeta(String name) {
    return this.clusterNodeMetaMap.get(name);
  }

  public void addClusterNodeMeta(String name, JsonObject clusterNodeMeta) {
    this.clusterNodeMetaMap.put(name, clusterNodeMeta);
  }

  public void removeClusterNodeMeta(String name) {
    this.clusterNodeMetaMap.remove(name);
  }
}

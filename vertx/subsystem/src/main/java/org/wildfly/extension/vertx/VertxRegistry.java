/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.vertx;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The central registry of Vertx installations.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxRegistry {
    private final Map<String, VertxProxy> vertxProxyMap;
    static VertxRegistry INSTANCE = new VertxRegistry();

    public static VertxRegistry getInstance() {
        return INSTANCE;
    }

    private VertxRegistry() {
        this.vertxProxyMap = new ConcurrentHashMap<>();
    }

    /**
     * Registers a VertxProxy to this registry. The name is used as the global unique key.
     * <p>
     *   This is called when VertxProxyService is started.
     * </p>
     *
     * @param vertxProxy the VertxProxy to register.
     */
    public void registerVertx(VertxProxy vertxProxy) {
        this.vertxProxyMap.put(vertxProxy.getName(), vertxProxy);
    }

    /**
     * Un-registers a VertxProxy by it's name.
     * <p>
     *   This is called when VertxProxyService is stopped.
     * </p>
     *
     * @param name the VertxProxy name
     */
    public void unRegister(String name) {
        this.vertxProxyMap.remove(name);
    }

    /**
     * Lists all registered VertxProxy
     *
     * @return the list of all registered VertxProxy
     */
    public Collection<VertxProxy> listVertx() {
        return this.vertxProxyMap.values();
    }

    /**
     * Gets the VertxProxy by it's name
     *
     * @param name the name of the VertxProxy
     * @return the VertxProxy or null if not found.
     */
    public VertxProxy getVertx(String name) {
        return this.vertxProxyMap.get(name);
    }

    /**
     * Gets the VertxProxy by searching the name or aliases.
     *
     * @param alias the alias or name to be searched.
     * @return the VertxProxy or null if not found
     */
    public VertxProxy getByNameOrAlias(String alias) {
        VertxProxy vp = getVertx(alias);
        if (vp != null) {
            return vp;
        }
        for (VertxProxy vertxProxy: listVertx()) {
            if (vertxProxy.getAliases().contains(alias)) {
                return vertxProxy;
            }
        }
        return null;
    }

}

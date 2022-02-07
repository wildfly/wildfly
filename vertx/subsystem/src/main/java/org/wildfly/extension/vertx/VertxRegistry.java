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

    public void registerVertx(VertxProxy vertxProxy) {
        this.vertxProxyMap.put(vertxProxy.getName(), vertxProxy);
    }

    public void unRegister(String name) {
        this.vertxProxyMap.remove(name);
    }

    public Collection<VertxProxy> listVertx() {
        return this.vertxProxyMap.values();
    }

    public VertxProxy getVertx(String name) {
        return this.vertxProxyMap.get(name);
    }

}

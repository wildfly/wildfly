package org.wildfly.extension.vertx;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class VertxRegistry {
    static VertxRegistry INSTANCE = new VertxRegistry();
    private final Map<String, VertxProxy> vertxProxyMap;
    private VertxRegistry() {
        this.vertxProxyMap = new ConcurrentHashMap<>();
    }

    /**
     * Register the VertxProxy, if it has been registered already, it is updated.
     *
     * @param vertxProxy the VertxProxy
     */
    void registerVertx(VertxProxy vertxProxy) {
        this.vertxProxyMap.put(vertxProxy.getName(), vertxProxy);
    }

    void unRegister(String name) {
        this.vertxProxyMap.remove(name);
    }

    Collection<VertxProxy> listVertx() {
        return this.vertxProxyMap.values();
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import io.vertx.core.Vertx;

import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resolves the optional Vert.x proxy without linking this module to the Vert.x extension implementation.
 */
final class VertxProxyReflection {
    private static final String PROXY_CLASS_NAME = "org.wildfly.extension.vertx.VertxProxy";
    private static final String VERTX_SERVICE_NAME = "org.wildfly.extension.vertx";

    private VertxProxyReflection() {
    }

    /**
     * Creates the optional Vert.x service dependency.
     *
     * @return the Vert.x dependency, or an empty dependency when the Vert.x extension is unavailable
     */
    static ServiceDependency<Vertx> dependency() {
        try {
            Class<?> proxyClass = Class.forName(PROXY_CLASS_NAME, false, VertxProxyReflection.class.getClassLoader());
            return ServiceDependency.on(VERTX_SERVICE_NAME, proxyClass)
                    .map(proxy -> getVertx(proxy));
        } catch (ClassNotFoundException | LinkageError ignored) {
            return ServiceDependency.empty();
        }
    }

    private static Vertx getVertx(Object proxy) {
        try {
            return (Vertx) proxy.getClass().getMethod("getVertx").invoke(proxy);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to obtain Vert.x from VertxProxy", e);
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import io.vertx.core.Vertx;

import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resolves the optional Vert.x proxy without linking this module to the Vert.x extension implementation.
 */
final class VertxProxyReflection {
    static final String VERTX_CAPABILITY_NAME = "org.wildfly.extension.vertx";

    private VertxProxyReflection() {
    }

    /**
     * Creates the Vert.x service dependency without loading the optional extension class through this module's classloader.
     *
     * @return the Vert.x dependency for the configured optional capability
     */
    static ServiceDependency<Vertx> dependency() {
        return ServiceDependency.on(VERTX_CAPABILITY_NAME, Object.class)
                .map(VertxProxyReflection::getVertx);
    }

    private static Vertx getVertx(Object proxy) {
        try {
            return (Vertx) proxy.getClass().getMethod("getVertx").invoke(proxy);
        } catch (ReflectiveOperationException e) {
            throw OTEL_LOGGER.unableToObtainVertxFromVertxProxy(e);
        }
    }
}

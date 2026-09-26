/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import io.opentelemetry.api.OpenTelemetry;
import io.smallrye.opentelemetry.api.OpenTelemetryLogHandler;

/**
 * Routes log records to the OpenTelemetry resource associated with the current deployment class loader.
 */
final class RoutingOpenTelemetryLogHandler extends Handler {
    private final OpenTelemetryLogHandler serverHandler;
    private final Map<ClassLoader, OpenTelemetryLogHandler> deploymentHandlers = new ConcurrentHashMap<>();

    /**
     * Creates a handler whose fallback route uses the server OpenTelemetry instance.
     *
     * @param serverOpenTelemetry the server telemetry instance
     */
    RoutingOpenTelemetryLogHandler(OpenTelemetry serverOpenTelemetry) {
        serverHandler = new OpenTelemetryLogHandler(serverOpenTelemetry);
    }

    /**
     * Registers the telemetry route for a deployment class loader.
     *
     * @param deploymentClassLoader the deployment class loader
     * @param deploymentOpenTelemetry the deployment telemetry instance
     */
    void register(ClassLoader deploymentClassLoader, OpenTelemetry deploymentOpenTelemetry) {
        deploymentHandlers.put(deploymentClassLoader, new OpenTelemetryLogHandler(deploymentOpenTelemetry));
    }

    /**
     * Removes and closes the telemetry route for a deployment class loader.
     *
     * @param deploymentClassLoader the deployment class loader
     */
    void unregister(ClassLoader deploymentClassLoader) {
        OpenTelemetryLogHandler handler = deploymentHandlers.remove(deploymentClassLoader);
        if (handler != null) {
            handler.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void publish(LogRecord record) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        OpenTelemetryLogHandler handler = contextClassLoader == null
                ? null
                : deploymentHandlers.get(contextClassLoader);
        (handler != null ? handler : serverHandler).publish(record);
    }

    /** {@inheritDoc} */
    @Override
    public void flush() {
        serverHandler.flush();
        deploymentHandlers.values().forEach(Handler::flush);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        deploymentHandlers.values().forEach(Handler::close);
        deploymentHandlers.clear();
        serverHandler.close();
    }
}

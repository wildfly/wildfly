/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;

/**
 * Deployment-specific OpenTelemetry wrapper that delegates:
 * - Tracer to server (shared)
 * - Logger to server (shared)
 * - Meter to deployment-specific provider (isolated)
 * - Propagators to server (shared)
 */
public class DeploymentOpenTelemetry implements OpenTelemetry {
    private final TracerProvider tracerProvider;
    private final LoggerProvider loggerProvider;
    private final MeterProvider meterProvider;
    private final ContextPropagators propagators;

    public DeploymentOpenTelemetry(TracerProvider tracerProvider,
                                   LoggerProvider loggerProvider,
                                   MeterProvider meterProvider,
                                   ContextPropagators propagators) {
        this.tracerProvider = tracerProvider;
        this.loggerProvider = loggerProvider;
        this.meterProvider = meterProvider;
        this.propagators = propagators;
    }

    @Override
    public TracerProvider getTracerProvider() {
        return tracerProvider;
    }

    @Override
    public LoggerProvider getLogsBridge() {
        return loggerProvider;
    }

    @Override
    public MeterProvider getMeterProvider() {
        return meterProvider;
    }

    @Override
    public ContextPropagators getPropagators() {
        return propagators;
    }
}

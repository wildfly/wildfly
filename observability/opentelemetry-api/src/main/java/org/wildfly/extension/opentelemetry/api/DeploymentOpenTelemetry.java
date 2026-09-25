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
 * Deployment-specific OpenTelemetry wrapper with resource-aware tracer/logger providers, an isolated meter provider,
 * and the server's shared propagators.
 */
public class DeploymentOpenTelemetry implements OpenTelemetry {
    private final TracerProvider tracerProvider;
    private final LoggerProvider loggerProvider;
    private final MeterProvider meterProvider;
    private final ContextPropagators propagators;

    /**
     * Creates the OpenTelemetry view exposed to one deployment.
     *
     * @param tracerProvider the deployment tracer provider
     * @param loggerProvider the deployment logger provider
     * @param meterProvider the deployment-isolated meter provider
     * @param propagators the server propagators
     */
    public DeploymentOpenTelemetry(TracerProvider tracerProvider,
                                   LoggerProvider loggerProvider,
                                   MeterProvider meterProvider,
                                   ContextPropagators propagators) {
        this.tracerProvider = tracerProvider;
        this.loggerProvider = loggerProvider;
        this.meterProvider = meterProvider;
        this.propagators = propagators;
    }

    /** {@inheritDoc} */
    @Override
    public TracerProvider getTracerProvider() {
        return tracerProvider;
    }

    /** {@inheritDoc} */
    @Override
    public LoggerProvider getLogsBridge() {
        return loggerProvider;
    }

    /** {@inheritDoc} */
    @Override
    public MeterProvider getMeterProvider() {
        return meterProvider;
    }

    /** {@inheritDoc} */
    @Override
    public ContextPropagators getPropagators() {
        return propagators;
    }
}

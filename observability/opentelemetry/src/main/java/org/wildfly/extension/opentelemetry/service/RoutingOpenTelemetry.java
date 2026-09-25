/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.opentelemetry.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;

/** Routes global OpenTelemetry lookups and cached tracers to their deployment registrations. */
public final class RoutingOpenTelemetry implements OpenTelemetry {
    private volatile OpenTelemetry fallback;
    private final Map<ClassLoader, OpenTelemetry> deployments = new ConcurrentHashMap<>();

    /** Creates a process-wide OpenTelemetry router. */
    public RoutingOpenTelemetry(OpenTelemetry fallback) {
        this.fallback = fallback;
    }

    /** Replaces the server fallback used when no deployment matches the current class loader. */
    public void setFallback(OpenTelemetry fallback) {
        this.fallback = fallback;
    }

    /** Registers a deployment delegate. */
    public void register(ClassLoader classLoader, OpenTelemetry openTelemetry) {
        deployments.put(classLoader, openTelemetry);
    }

    /** Removes a deployment delegate. */
    public void unregister(ClassLoader classLoader) {
        deployments.remove(classLoader);
    }

    /** Returns the current delegate for a class loader, or the active server fallback. */
    private OpenTelemetry current(ClassLoader classLoader) {
        OpenTelemetry deployment = deployments.get(classLoader);
        return deployment == null ? fallback : deployment;
    }

    /** Returns the delegate for the calling thread's context class loader. */
    private OpenTelemetry current() {
        return current(Thread.currentThread().getContextClassLoader());
    }

    /** Returns a provider whose cached tracers follow later deployment registrations. */
    @Override
    public TracerProvider getTracerProvider() {
        // Reactive Messaging caches its tracer during CDI startup. Retain the deployment identity,
        // but resolve its provider when a span starts, after deployment telemetry is registered.
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new TracerProvider() {
            /** {@inheritDoc} */
            @Override
            public Tracer get(String instrumentationScopeName) {
                return tracerBuilder(instrumentationScopeName).build();
            }

            /** {@inheritDoc} */
            @Override
            public Tracer get(String instrumentationScopeName, String instrumentationScopeVersion) {
                return tracerBuilder(instrumentationScopeName)
                        .setInstrumentationVersion(instrumentationScopeVersion).build();
            }

            /** {@inheritDoc} */
            @Override
            public TracerBuilder tracerBuilder(String instrumentationScopeName) {
                return new TracerBuilder() {
                    private String version;
                    private String schemaUrl;

                    /** {@inheritDoc} */
                    @Override
                    public TracerBuilder setSchemaUrl(String schemaUrl) {
                        this.schemaUrl = schemaUrl;
                        return this;
                    }

                    /** {@inheritDoc} */
                    @Override
                    public TracerBuilder setInstrumentationVersion(String version) {
                        this.version = version;
                        return this;
                    }

                    /** {@inheritDoc} */
                    @Override
                    public Tracer build() {
                        String selectedVersion = version;
                        String selectedSchemaUrl = schemaUrl;
                        return spanName -> {
                            TracerBuilder delegate = current(classLoader).getTracerProvider()
                                    .tracerBuilder(instrumentationScopeName);
                            if (selectedVersion != null) {
                                delegate.setInstrumentationVersion(selectedVersion);
                            }
                            if (selectedSchemaUrl != null) {
                                delegate.setSchemaUrl(selectedSchemaUrl);
                            }
                            return delegate.build().spanBuilder(spanName);
                        };
                    }
                };
            }
        };
    }

    @Override
    public MeterProvider getMeterProvider() {
        return current().getMeterProvider();
    }

    @Override
    public LoggerProvider getLogsBridge() {
        return current().getLogsBridge();
    }

    @Override
    public ContextPropagators getPropagators() {
        return current().getPropagators();
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.opentelemetry.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.propagation.ContextPropagators;

/** Routes global OpenTelemetry lookups and cached tracers to their deployment registrations. */
public final class RoutingOpenTelemetry implements OpenTelemetry {
    private final AtomicReference<OpenTelemetry> fallback;
    private final Map<ClassLoader, Registration> deployments = new ConcurrentHashMap<>();

    /** Creates a process-wide OpenTelemetry router. */
    public RoutingOpenTelemetry(OpenTelemetry fallback) {
        this.fallback = new AtomicReference<>(fallback);
    }

    /** Replaces the server fallback used when no deployment matches the current class loader. */
    public void setFallback(OpenTelemetry fallback) {
        this.fallback.set(fallback);
    }

    /** Clears a fallback only when it still belongs to the service being stopped. */
    public void clearFallback(OpenTelemetry expectedFallback) {
        fallback.compareAndSet(expectedFallback, OpenTelemetry.noop());
    }

    /** Registers a deployment delegate. */
    public void register(ClassLoader classLoader, OpenTelemetry openTelemetry) {
        register(classLoader, openTelemetry, null);
    }

    /** Registers a deployment delegate owned by a service lifecycle. */
    public void register(ClassLoader classLoader, OpenTelemetry openTelemetry, Object owner) {
        deployments.put(classLoader, new Registration(openTelemetry, owner));
    }

    /** Removes a deployment delegate. */
    public void unregister(ClassLoader classLoader) {
        deployments.remove(classLoader);
    }

    /** Removes a deployment delegate only when it belongs to the expected owner. */
    public void unregister(ClassLoader classLoader, Object owner) {
        deployments.computeIfPresent(classLoader,
                (ignored, registration) -> registration.owner == owner ? null : registration);
    }

    /** Removes all registrations owned by one service lifecycle. */
    public void unregisterAll(Object owner) {
        deployments.entrySet().removeIf(entry -> entry.getValue().owner == owner);
    }

    /** Returns the current delegate for a class loader, or the active server fallback. */
    private OpenTelemetry current(ClassLoader classLoader) {
        Registration registration = deployments.get(classLoader);
        return registration == null ? fallback.get() : registration.openTelemetry;
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
                        return new Tracer() {
                            private volatile OpenTelemetry selectedDelegate;
                            private volatile Tracer delegateTracer;

                            /** {@inheritDoc} */
                            @Override
                            public SpanBuilder spanBuilder(String spanName) {
                                OpenTelemetry delegate = current(classLoader);
                                Tracer tracer = delegateTracer;
                                if (selectedDelegate != delegate) {
                                    synchronized (this) {
                                        delegate = current(classLoader);
                                        if (selectedDelegate != delegate) {
                                            TracerBuilder delegateBuilder = delegate.getTracerProvider()
                                                    .tracerBuilder(instrumentationScopeName);
                                            if (selectedVersion != null) {
                                                delegateBuilder.setInstrumentationVersion(selectedVersion);
                                            }
                                            if (selectedSchemaUrl != null) {
                                                delegateBuilder.setSchemaUrl(selectedSchemaUrl);
                                            }
                                            tracer = delegateBuilder.build();
                                            delegateTracer = tracer;
                                            selectedDelegate = delegate;
                                        } else {
                                            tracer = delegateTracer;
                                        }
                                    }
                                }
                                return tracer.spanBuilder(spanName);
                            }
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

    /** Holds a deployment delegate and the lifecycle that owns its registration. */
    private static final class Registration {
        private final OpenTelemetry openTelemetry;
        private final Object owner;

        /** Creates a routing registration. */
        private Registration(OpenTelemetry openTelemetry, Object owner) {
            this.openTelemetry = openTelemetry;
            this.owner = owner;
        }
    }
}

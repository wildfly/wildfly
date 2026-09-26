/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import java.util.Map;
import java.util.function.BiConsumer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;

/**
 * CDI producer for OpenTelemetry artifacts in each deployment.
 * The OpenTelemetry instance itself is injected by OpenTelemetryCdiExtension
 * (either server-level or deployment-specific wrapper).
 */
@Singleton
public class WildFlyOpenTelemetryProducer {
    private static final String INSTRUMENTATION_NAME = "org.wildfly.extension.opentelemetry";
    private static final String INSTRUMENTATION_VERSION =
            java.util.Optional.ofNullable(WildFlyOpenTelemetryProducer.class.getPackage().getImplementationVersion())
                    .orElse("SNAPSHOT");

    /**
     * Produces the deployment tracer using WildFly's instrumentation identity.
     *
     * @return the deployment tracer
     */
    @Produces
    @Singleton
    public Tracer getTracer() {
        return CDI.current().select(OpenTelemetry.class).get()
                .getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
    }

    /**
     * Produces the deployment meter using WildFly's instrumentation identity.
     *
     * @return the deployment meter
     */
    @Produces
    @Singleton
    public Meter getMeter() {
        return CDI.current().select(OpenTelemetry.class).get()
                .getMeter(INSTRUMENTATION_NAME);
    }

    /**
     * Produces a request-scoped proxy that always delegates to the span in the current context.
     *
     * @return the current-span proxy
     */
    @Produces
    @RequestScoped
    public Span getSpan() {
        return new Span() {
            /** {@inheritDoc} */
            @Override
            public <T> Span setAttribute(io.opentelemetry.api.common.AttributeKey<T> key, T value) {
                return Span.current().setAttribute(key, value);
            }

            /** {@inheritDoc} */
            @Override
            public Span addEvent(String name, io.opentelemetry.api.common.Attributes attributes) {
                return Span.current().addEvent(name, attributes);
            }

            /** {@inheritDoc} */
            @Override
            public Span addEvent(String name, io.opentelemetry.api.common.Attributes attributes, long timestamp,
                                 java.util.concurrent.TimeUnit unit) {
                return Span.current().addEvent(name, attributes, timestamp, unit);
            }

            /** {@inheritDoc} */
            @Override
            public Span setStatus(io.opentelemetry.api.trace.StatusCode statusCode, String description) {
                return Span.current().setStatus(statusCode, description);
            }

            /** {@inheritDoc} */
            @Override
            public Span recordException(Throwable exception, io.opentelemetry.api.common.Attributes additionalAttributes) {
                return Span.current().recordException(exception, additionalAttributes);
            }

            /** {@inheritDoc} */
            @Override
            public Span updateName(String name) {
                return Span.current().updateName(name);
            }

            /** {@inheritDoc} */
            @Override
            public void end() {
                Span.current().end();
            }

            /** {@inheritDoc} */
            @Override
            public void end(long timestamp, java.util.concurrent.TimeUnit unit) {
                Span.current().end(timestamp, unit);
            }

            /** {@inheritDoc} */
            @Override
            public io.opentelemetry.api.trace.SpanContext getSpanContext() {
                return Span.current().getSpanContext();
            }

            /** {@inheritDoc} */
            @Override
            public boolean isRecording() {
                return Span.current().isRecording();
            }
        };
    }

    /**
     * Produces a request-scoped proxy that always delegates to baggage in the current context.
     *
     * @return the current-baggage proxy
     */
    @Produces
    @RequestScoped
    public Baggage getBaggage() {
        return new Baggage() {
            /** {@inheritDoc} */
            @Override
            public int size() {
                return Baggage.current().size();
            }

            /** {@inheritDoc} */
            @Override
            public void forEach(
                    BiConsumer<? super String,
                            ? super io.opentelemetry.api.baggage.BaggageEntry> consumer) {
                Baggage.current().forEach(consumer);
            }

            /** {@inheritDoc} */
            @Override
            public Map<String, io.opentelemetry.api.baggage.BaggageEntry> asMap() {
                return Baggage.current().asMap();
            }

            /** {@inheritDoc} */
            @Override
            public String getEntryValue(String entryKey) {
                return Baggage.current().getEntryValue(entryKey);
            }

            /** {@inheritDoc} */
            @Override
            public io.opentelemetry.api.baggage.BaggageBuilder toBuilder() {
                return Baggage.current().toBuilder();
            }
        };
    }
}

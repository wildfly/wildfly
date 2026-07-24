/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.INSTRUMENTATION_VERSION;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_SDK_DISABLED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.runtimemetrics.java8.RuntimeMetrics;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.smallrye.opentelemetry.api.OpenTelemetryLogHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;

@Singleton
public class WildFlyOpenTelemetryProducer {
    private static final String INSTRUMENTATION_NAME = "org.wildfly.extension.opentelemetry";

    private final List<AutoCloseable> closeables = new ArrayList<>();

    @Produces
    @Singleton
    public OpenTelemetry getOpenTelemetry(WildFlyOpenTelemetryConfig config) {
        Map<String, String> properties = config.getProperties();

        if ("true".equals(properties.get(OTEL_SDK_DISABLED))) {
            return OpenTelemetry.noop();
        }

        GlobalOpenTelemetry.resetForTest();

        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder()
                .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                .addPropertiesSupplier(config::getProperties)
                .disableShutdownHook()
                .setResultAsGlobal();

        if (!config.getFilters().isEmpty()) {
            builder.addMetricExporterCustomizer(
                    (e, cp) -> new FilteringMetricExporter(e, config.getFilters()));
        }

        OpenTelemetrySdk otel = builder.build().getOpenTelemetrySdk();
        if (config.exposeSystemMetrics()) {
            closeables.add(RuntimeMetrics.create(otel));
        }
        OpenTelemetryLogHandler.install(otel);
        return otel;
    }

    @Produces
    @Singleton
    public Tracer getTracer() {
        return CDI.current().select(OpenTelemetry.class).get()
                .getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
    }

    @Produces
    @Singleton
    public Meter getMeter() {
        return CDI.current().select(OpenTelemetry.class).get()
                .getMeter(INSTRUMENTATION_NAME);
    }

    @Produces
    @RequestScoped
    public Span getSpan() {
        return new Span() {
            @Override
            public <T> Span setAttribute(io.opentelemetry.api.common.AttributeKey<T> key, T value) {
                return Span.current().setAttribute(key, value);
            }

            @Override
            public Span addEvent(String name, Attributes attributes) {
                return Span.current().addEvent(name, attributes);
            }

            @Override
            public Span addEvent(String name, Attributes attributes, long timestamp,
                                 java.util.concurrent.TimeUnit unit) {
                return Span.current().addEvent(name, attributes, timestamp, unit);
            }

            @Override
            public Span setStatus(io.opentelemetry.api.trace.StatusCode statusCode, String description) {
                return Span.current().setStatus(statusCode, description);
            }

            @Override
            public Span recordException(Throwable exception, Attributes additionalAttributes) {
                return Span.current().recordException(exception, additionalAttributes);
            }

            @Override
            public Span updateName(String name) {
                return Span.current().updateName(name);
            }

            @Override
            public void end() {
                Span.current().end();
            }

            @Override
            public void end(long timestamp, java.util.concurrent.TimeUnit unit) {
                Span.current().end(timestamp, unit);
            }

            @Override
            public io.opentelemetry.api.trace.SpanContext getSpanContext() {
                return Span.current().getSpanContext();
            }

            @Override
            public boolean isRecording() {
                return Span.current().isRecording();
            }
        };
    }

    @Produces
    @RequestScoped
    public Baggage getBaggage() {
        return new Baggage() {
            @Override
            public int size() {
                return Baggage.current().size();
            }

            @Override
            public void forEach(
                    BiConsumer<? super String,
                            ? super io.opentelemetry.api.baggage.BaggageEntry> consumer) {
                Baggage.current().forEach(consumer);
            }

            @Override
            public Map<String, io.opentelemetry.api.baggage.BaggageEntry> asMap() {
                return Baggage.current().asMap();
            }

            @Override
            public String getEntryValue(String entryKey) {
                return Baggage.current().getEntryValue(entryKey);
            }

            @Override
            public io.opentelemetry.api.baggage.BaggageBuilder toBuilder() {
                return Baggage.current().toBuilder();
            }
        };
    }

    public void close(@Disposes final OpenTelemetry openTelemetry) throws Exception {
        GlobalOpenTelemetry.resetForTest();

        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }

        if (openTelemetry instanceof OpenTelemetrySdk openTelemetrySdk) {
            List<CompletableResultCode> shutdown = new ArrayList<>();
            shutdown.add(openTelemetrySdk.getSdkTracerProvider().shutdown());
            shutdown.add(openTelemetrySdk.getSdkMeterProvider().shutdown());
            shutdown.add(openTelemetrySdk.getSdkLoggerProvider().shutdown());
            CompletableResultCode.ofAll(shutdown).join(10, TimeUnit.SECONDS);
        }
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.INSTRUMENTATION_VERSION;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_SDK_DISABLED;

import java.security.AccessController;
import java.security.PrivilegedAction;
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
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.smallrye.opentelemetry.api.OpenTelemetryLogHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;
import org.wildfly.security.manager.WildFlySecurityManager;

@Singleton
public class WildFlyOpenTelemetryProducer {
    private static final String INSTRUMENTATION_NAME = "org.wildfly.extension.opentelemetry";

    private final List<AutoCloseable> closeables = new ArrayList<>();

    /**
     * Customizes the meter provider builder. Subclasses can override to add custom configuration.
     *
     * @param builder the meter provider builder
     * @param config the OpenTelemetry configuration properties
     * @return the customized builder
     */
    protected SdkMeterProviderBuilder mpc(SdkMeterProviderBuilder builder, ConfigProperties config) {
        return builder;
    }

    /**
     * Produces the OpenTelemetry instance configured with WildFly settings including
     * metric filtering if filters are defined.
     *
     * @param config the WildFly OpenTelemetry configuration
     * @return the configured OpenTelemetry instance
     */
    @Produces
    @Singleton
    public OpenTelemetry getOpenTelemetry(WildFlyOpenTelemetryConfig config) {
        Map<String, String> properties = config.getProperties();

        if (Boolean.parseBoolean(properties.get(OTEL_SDK_DISABLED))) {
            return OpenTelemetry.noop();
        }

        GlobalOpenTelemetry.resetForTest();

        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder()
                .addMeterProviderCustomizer(this::mpc)
                .setServiceClassLoader(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged())
                .addPropertiesSupplier(config::getProperties)
                .disableShutdownHook()
                .setResultAsGlobal();

        if (!config.getFilters().isEmpty()) {
            builder.addMetricExporterCustomizer(
                    (e, cp) -> new FilteringMetricExporter(e, config.getFilters()));
        }

        OpenTelemetrySdk otel;
        if (WildFlySecurityManager.isChecking()) {
            // Requires FilePermission/RuntimePermission
            otel = AccessController.doPrivileged(
                    (PrivilegedAction<OpenTelemetrySdk>) () -> builder.build().getOpenTelemetrySdk());
        } else {
            otel = builder.build().getOpenTelemetrySdk();
        }

        if (config.exposeSystemMetrics()) {
            if (WildFlySecurityManager.isChecking()) {
                // Requires FilePermission/RuntimePermission/ManagementPermission
                closeables.add(AccessController.doPrivileged(
                        (PrivilegedAction<RuntimeMetrics>) () -> RuntimeMetrics.create(otel)));
            } else {
                closeables.add(RuntimeMetrics.create(otel));
            }
        }

        if (WildFlySecurityManager.isChecking()) {
            // Requires LoggingPermission
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                OpenTelemetryLogHandler.install(otel);
                return null;
            });
        } else {
            OpenTelemetryLogHandler.install(otel);
        }
        return otel;
    }

    /**
     * Produces a Tracer instance for creating spans.
     *
     * @return the OpenTelemetry tracer
     */
    @Produces
    @Singleton
    public Tracer getTracer() {
        return CDI.current().select(OpenTelemetry.class).get()
                .getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
    }

    /**
     * Produces a Meter instance for recording metrics.
     *
     * @return the OpenTelemetry meter
     */
    @Produces
    @Singleton
    public Meter getMeter() {
        return CDI.current().select(OpenTelemetry.class).get()
                .getMeter(INSTRUMENTATION_NAME);
    }

    /**
     * Produces a request-scoped Span that delegates to the current span.
     *
     * @return a Span wrapper that delegates to Span.current()
     */
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

    /**
     * Produces a request-scoped Baggage that delegates to the current baggage.
     *
     * @return a Baggage wrapper that delegates to Baggage.current()
     */
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

    /**
     * Disposes the OpenTelemetry instance and shuts down all providers.
     *
     * @param openTelemetry the OpenTelemetry instance to dispose
     * @throws Exception if shutdown fails
     */
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

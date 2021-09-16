/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.opentelemetry;

import static org.wildfly.extension.opentelemetry.deployment.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class OpenTelemetryHolder {
    public final OpenTelemetryConfig config;
    private static final String DEFAULT_SERVICE_NAME = "wildfly";
    private volatile OpenTelemetry openTelemetry;

    public OpenTelemetryHolder(OpenTelemetryConfig config) {
        this.config = config;
        this.openTelemetry = build(config);
    }

    public OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    private OpenTelemetry build(OpenTelemetryConfig config) {
        String serviceName = (config.serviceName != null) ? config.serviceName : DEFAULT_SERVICE_NAME;
        final SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder()
                .addSpanProcessor(getSpanProcessor(config))
                .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)));

        if (config.sampler != null) {
            tracerProviderBuilder.setSampler(getSampler(config));
        }

        try {
            return OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProviderBuilder.build())
                    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                    .buildAndRegisterGlobal();
        } catch (IllegalStateException ex) {
            return GlobalOpenTelemetry.get();
        }
    }

    private SpanProcessor getSpanProcessor(OpenTelemetryConfig config) {
        final SpanExporter spanExporter = getSpanExporter(config);
        switch (config.spanProcessor) {
            case "batch": {
                return BatchSpanProcessor.builder(spanExporter)
                        .setScheduleDelay(config.batchDelay, TimeUnit.MILLISECONDS)
                        .setMaxQueueSize(config.maxQueueSize)
                        .setMaxExportBatchSize(config.maxExportBatchSize)
                        .setExporterTimeout(config.exportTimeout, TimeUnit.MILLISECONDS)
                        .build();
            }
            case "simple": {
                return SimpleSpanProcessor.create(spanExporter);
            }
            default: {
                throw OTEL_LOGGER.unsupportedSpanProcessor(config.spanProcessor);
            }
        }
    }

    private Sampler getSampler(OpenTelemetryConfig config) {
        switch (config.sampler) {
            case "on":
                return Sampler.alwaysOn();
            case "off":
                return Sampler.alwaysOff();
            case "ratio":
                return Sampler.traceIdRatioBased(config.ratio);
            default:
                throw OTEL_LOGGER.unsupportedSampler(config.sampler);
        }
    }

    private SpanExporter getSpanExporter(OpenTelemetryConfig config) {
        switch (config.exporter) {
            case "jaeger": {
                return JaegerGrpcSpanExporter.builder()
                        .setEndpoint(config.endpoint)
                        .setTimeout(config.exportTimeout, TimeUnit.MILLISECONDS)
                        .build();
            }
            case "otlp": {
                return OtlpGrpcSpanExporter.builder()
                        .setEndpoint(config.endpoint)
                        .setTimeout(config.exportTimeout, TimeUnit.MILLISECONDS)
                        .build();
            }
            default:
                throw OTEL_LOGGER.unsupportedExporter(config.exporter);
        }
    }
}

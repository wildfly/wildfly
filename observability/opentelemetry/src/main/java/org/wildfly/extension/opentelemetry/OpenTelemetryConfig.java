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

public class OpenTelemetryConfig {
    public final String serviceName;
    public final String exporter;
    public final String endpoint;
    public final String spanProcessor;
    public final long batchDelay;
    public final int maxQueueSize;
    public final int maxExportBatchSize;
    public final long exportTimeout;
    public final String sampler;
    public final Double ratio;

    private OpenTelemetryConfig(String serviceName, String exporter, String endpoint, String spanProcessor,
                                long batchDelay, int maxQueueSize, int maxExportBatchSize, long exportTimeout,
                                String sampler, Double ratio) {
        this.serviceName = serviceName;
        this.exporter = exporter;
        this.endpoint = endpoint;
        this.spanProcessor = spanProcessor;
        this.batchDelay = batchDelay;
        this.maxQueueSize = maxQueueSize;
        this.maxExportBatchSize = maxExportBatchSize;
        this.exportTimeout = exportTimeout;
        this.sampler = sampler;
        this.ratio = ratio;
    }

    public static class OpenTelemetryConfigBuilder {
        private String serviceName;
        private String exporter;
        private String endpoint;
        private String spanProcessor;
        private long batchDelay;
        private int maxQueueSize;
        private int maxExportBatchSize;
        private long exportTimeout;
        private String sampler;
        private Double ratio;

        private OpenTelemetryConfigBuilder() {
        }

        public static OpenTelemetryConfigBuilder config() {
            return new OpenTelemetryConfigBuilder();
        }

        public OpenTelemetryConfigBuilder withServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public OpenTelemetryConfigBuilder withExporter(String exporter) {
            this.exporter = exporter;
            return this;
        }

        public OpenTelemetryConfigBuilder withEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public OpenTelemetryConfigBuilder withSpanProcessor(String spanProcessor) {
            this.spanProcessor = spanProcessor;
            return this;
        }

        public OpenTelemetryConfigBuilder withBatchDelay(long batchDelay) {
            this.batchDelay = batchDelay;
            return this;
        }

        public OpenTelemetryConfigBuilder withMaxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
            return this;
        }

        public OpenTelemetryConfigBuilder withMaxExportBatchSize(int maxExportBatchSize) {
            this.maxExportBatchSize = maxExportBatchSize;
            return this;
        }

        public OpenTelemetryConfigBuilder withExportTimeout(long exportTimeout) {
            this.exportTimeout = exportTimeout;
            return this;
        }

        public OpenTelemetryConfigBuilder withSampler(String sampler) {
            this.sampler = sampler;
            return this;
        }

        public OpenTelemetryConfigBuilder withRatio(Double ratio) {
            this.ratio = ratio;
            return this;
        }

        public OpenTelemetryConfig build() {
            return new OpenTelemetryConfig(serviceName, exporter, endpoint, spanProcessor, batchDelay, maxQueueSize,
                    maxExportBatchSize, exportTimeout, sampler, ratio);
        }
    }
}

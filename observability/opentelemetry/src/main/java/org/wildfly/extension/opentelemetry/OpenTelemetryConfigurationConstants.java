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

import java.util.Optional;

import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;

final class OpenTelemetryConfigurationConstants {
    private OpenTelemetryConfigurationConstants() {}
    private String INSTRUMENTATION_NAME = "io.smallrye.opentelemetry";

    private String INSTRUMENTATION_VERSION = Optional.ofNullable(WildFlyOpenTelemetryConfig.class.getPackage().getImplementationVersion())
            .orElse("SNAPSHOT");
    static final String SERVICE_NAME = "service-name";
    static final String EXPORTER_TYPE = "exporter-type";
    static final String ENDPOINT = "endpoint";
    static final String SPAN_PROCESSOR_TYPE = "span-processor-type";
    static final String BATCH_DELAY = "batch-delay";
    static final String MAX_QUEUE_SIZE = "max-queue-size";
    static final String MAX_EXPORT_BATCH_SIZE = "max-export-batch-size";
    static final String EXPORT_TIMEOUT = "export-timeout";
    static final String SAMPLER_TYPE = "sampler-type";
    static final String RATIO = "ratio";
    static final String TYPE = "type";

    // Groups
    static final String GROUP_EXPORTER = "exporter";
    static final String GROUP_SPAN_PROCESSOR = "span-processor";
    static final String GROUP_SAMPLER = "sampler";

    static final String OPENTELEMETRY_CAPABILITY_NAME = "org.wildfly.extension.opentelemetry";

}

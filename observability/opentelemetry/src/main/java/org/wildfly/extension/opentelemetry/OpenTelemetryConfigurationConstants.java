/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

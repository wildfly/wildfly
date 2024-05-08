/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;

public final class OpenTelemetryConfigurationConstants {

    public static final String SUBSYSTEM_NAME = "opentelemetry";
    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, OpenTelemetrySubsystemExtension.class);
    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    static final String VERTX_DISABLE_DNS_RESOLVER = "vertx.disableDnsResolver";

    private OpenTelemetryConfigurationConstants() {}

    // *****************************************************************************************************************
    // WildFly Model constants
    public static final String SERVICE_NAME = "service-name";
    public static final String EXPORTER_TYPE = "exporter-type";
    public static final String ENDPOINT = "endpoint";
    public static final String SPAN_PROCESSOR_TYPE = "span-processor-type";
    public static final String BATCH_DELAY = "batch-delay";
    public static final String MAX_QUEUE_SIZE = "max-queue-size";
    public static final String MAX_EXPORT_BATCH_SIZE = "max-export-batch-size";
    public static final String EXPORT_TIMEOUT = "export-timeout";
    public static final String SAMPLER_TYPE = "sampler-type";
    public static final String SAMPLER_RATIO = "ratio";
    public static final String TYPE = "type";

    // Groups
    public static final String GROUP_EXPORTER = "exporter";
    public static final String GROUP_SPAN_PROCESSOR = "span-processor";
    public static final String GROUP_SAMPLER = "sampler";

    public static final String OPENTELEMETRY_CAPABILITY_NAME = "org.wildfly.extension.opentelemetry";

    // *****************************************************************************************************************
    // OpenTelemetry constants
    public static final String EXPORTER_JAEGER = "jaeger";
    public static final String EXPORTER_OTLP = "otlp";
    public static final String DEFAULT_OTLP_ENDPOINT = "http://localhost:4317";
    public static final int DEFAULT_BATCH_DELAY = 5000;
    public static final int DEFAULT_EXPORT_TIMEOUT = 30000;
    public static final int DEFAULT_MAX_QUEUE_SIZE = 2048;
    public static final int DEFAULT_MAX_EXPORT_BATCH_SIZE = 512;

    public static final String[] ALLOWED_EXPORTERS = {EXPORTER_JAEGER, EXPORTER_OTLP};
    public static final String SAMPLER_ON = "on";
    public static final String SAMPLER_OFF = "off";
    public static final String[] ALLOWED_SAMPLERS = {SAMPLER_ON, SAMPLER_OFF, SAMPLER_RATIO};
    public static final String SPAN_PROCESSOR_BATCH = "batch";
    public static final String SPAN_PROCESSOR_SIMPLE = "simple";
    public static final String[] ALLOWED_SPAN_PROCESSORS = {SPAN_PROCESSOR_BATCH, SPAN_PROCESSOR_SIMPLE};
}

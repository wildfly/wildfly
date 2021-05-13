package org.wildfly.extension.opentelemetry;

public final class OpenTelemetryConfigurationConstants {
    private OpenTelemetryConfigurationConstants() {
    }

    public static final String SERVICE_NAME = "service-name";
    public static final String EXPORTER = "exporter";
    public static final String ENDPOINT = "endpoint";
//    public static final String TRACES_ENDPOINT = "traces-endpoint";
    public static final String SPAN_PROCESSOR = "span-processor";
    public static final String BATCH_DELAY = "batch-delay";
    public static final String MAX_QUEUE_SIZE = "max-queue-size";
    public static final String MAX_EXPORT_BATCH_SIZE = "max-export-batch-size";
    public static final String EXPORT_TIMEOUT = "export-timeout";
    public static final String SAMPLER = "sampler";
    public static final String SAMPLER_ARG = "sampler-arg";
}

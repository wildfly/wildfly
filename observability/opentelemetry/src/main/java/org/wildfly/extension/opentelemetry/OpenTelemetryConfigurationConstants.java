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

public final class OpenTelemetryConfigurationConstants {
    private OpenTelemetryConfigurationConstants() {}

    public static final String SERVICE_NAME = "service-name";
    public static final String EXPORTER_TYPE = "exporter-type";
    public static final String ENDPOINT = "endpoint";
    public static final String SPAN_PROCESSOR_TYPE = "span-processor-type";
    public static final String BATCH_DELAY = "batch-delay";
    public static final String MAX_QUEUE_SIZE = "max-queue-size";
    public static final String MAX_EXPORT_BATCH_SIZE = "max-export-batch-size";
    public static final String EXPORT_TIMEOUT = "export-timeout";
    public static final String SAMPLER_TYPE = "sampler-type";
    public static final String RATIO = "ratio";
    public static final String TYPE = "type";

    // Groups
    public static final String GROUP_EXPORTER = "exporter";
    public static final String GROUP_SPAN_PROCESSOR = "span-processor";
    public static final String GROUP_SAMPLER = "sampler";

    public static final String OPENTELEMETRY_CAPABILITY_NAME = "org.wildfly.extension.opentelemetry";

}

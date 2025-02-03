/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry.exporter;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import jakarta.enterprise.inject.spi.CDI;

public class JsonLogRecordExporterProvider implements ConfigurableLogRecordExporterProvider {
    @Override
    public LogRecordExporter createExporter(final ConfigProperties config) {
        return CDI.current().select(JsonLogRecordExporter.class).get();
    }

    @Override
    public String getName() {
        return "json";
    }
}

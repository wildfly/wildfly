/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;
import java.util.Set;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Test;

/** Verifies that deployment-owned exporters do not inherit server exporter transport settings. */
public class MicroProfileTelemetryDeploymentProcessorTestCase {

    /** Verifies server exporter endpoint and credentials are not copied into deployment configuration. */
    @Test
    public void deploymentExporterDoesNotInheritServerTransportSettings() {
        Map<String, String> serverProperties = Map.of(
                "otel.exporter.otlp.endpoint", "https://server.example",
                "otel.exporter.otlp.headers", "authorization=server-secret");
        SmallRyeConfig deploymentConfig = new SmallRyeConfigBuilder()
                .withSources(new MapConfigSource(Map.of("otel.logs.exporter", "otlp")))
                .build();

        Map<String, String> resolved = MicroProfileTelemetryDeploymentProcessor.resolveProperties(
                serverProperties, deploymentConfig);

        assertEquals("otlp", resolved.get("otel.logs.exporter"));
        assertFalse(resolved.containsKey("otel.exporter.otlp.endpoint"));
        assertFalse(resolved.containsKey("otel.exporter.otlp.headers"));
    }

    /** Supplies a small deployment-local MicroProfile Config source for the test. */
    private static final class MapConfigSource implements ConfigSource {
        private final Map<String, String> properties;

        /** Creates a config source backed by the supplied properties. */
        private MapConfigSource(Map<String, String> properties) {
            this.properties = properties;
        }

        /** {@inheritDoc} */
        @Override
        public Map<String, String> getProperties() {
            return properties;
        }

        /** {@inheritDoc} */
        @Override
        public Set<String> getPropertyNames() {
            return properties.keySet();
        }

        /** {@inheritDoc} */
        @Override
        public String getValue(String propertyName) {
            return properties.get(propertyName);
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return MapConfigSource.class.getName();
        }

        /** {@inheritDoc} */
        @Override
        public int getOrdinal() {
            return 500;
        }
    }
}

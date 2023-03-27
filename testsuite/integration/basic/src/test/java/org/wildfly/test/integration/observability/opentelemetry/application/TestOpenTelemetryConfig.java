/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.observability.opentelemetry.application;

import java.util.HashMap;
import java.util.Map;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

@Alternative
@Singleton
@Priority(Integer.MAX_VALUE)
public class TestOpenTelemetryConfig implements OpenTelemetryConfig {
    private Map<String, String> properties = new HashMap<>();

    @Override
    public Map<String, String> properties() {
//        properties.put("otel.service.name", BaseOpenTelemetryTest.SERVICE_NAME);
        properties.put("otel.traces.exporter", "in-memory");
        return properties;
    }
}

/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.micrometer;

import java.time.Duration;
import java.util.Map;

import io.micrometer.registry.otlp.OtlpConfig;

public final class WildFlyMicrometerConfig implements OtlpConfig {
    /**
     * The OTLP endpoint to which to push metrics
     */
    private final String endpoint;
    /**
     * How frequently, in seconds, to push metrics
     */
    private final Long step;

    public WildFlyMicrometerConfig(String endpoint, Long step) {
        this.endpoint = endpoint;
        this.step = step;
    }

    @Override
    public String get(String key) {
        return null; // Accept defaults not explicitly overridden below
    }

    @Override
    public Map<String, String> resourceAttributes() {
        Map<String, String> attributes = OtlpConfig.super.resourceAttributes();
        if (!attributes.containsKey("service.name")) {
            attributes.put("service.name", "wildfly");
        }
        return attributes;
    }

    @Override
    public String url() {
        return endpoint;
    }

    @Override
    public Duration step() {
        Duration duration = Duration.ofSeconds(step);
        return duration;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

public final class MicrometerConfigurationConstants {
    private MicrometerConfigurationConstants() {
    }

    public static final String MICROMETER_MODULE = "org.wildfly.extension.micrometer";
    public static final String MICROMETER_API_MODULE = "org.wildfly.micrometer.deployment";

    static final String NAME = "micrometer";

    static final String OTLP_REGISTRY = "otlp-registry";
    public static final String ENDPOINT = "endpoint";
    public static final String STEP = "step";

    static final String PROMETHEUS_REGISTRY = "prometheus-registry";
    public static final String CONTEXT = "context";
    public static final String SECURITY_ENABLED = "security-enabled";
}

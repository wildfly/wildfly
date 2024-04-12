/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

public final class MicrometerConfigurationConstants {
    static final String NAME = "micrometer";

    private MicrometerConfigurationConstants() {
    }

    static final String OTLP_REGISTRY = "otlp-registry";
    public static final String ENDPOINT = "endpoint";
    public static final String STEP = "step";
    public static final String CONTEXT = "context";
}

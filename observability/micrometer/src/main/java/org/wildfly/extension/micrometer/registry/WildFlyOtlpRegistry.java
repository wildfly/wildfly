/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.registry;

import org.wildfly.extension.micrometer.WildFlyMicrometerConfig;
import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.OtlpMeterRegistry;

public class WildFlyOtlpRegistry extends OtlpMeterRegistry implements WildFlyRegistry {

    public WildFlyOtlpRegistry(WildFlyMicrometerConfig config) {
        super(config, Clock.SYSTEM);
    }
}

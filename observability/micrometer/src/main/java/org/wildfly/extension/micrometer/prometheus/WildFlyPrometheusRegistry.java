/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.prometheus;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

public class WildFlyPrometheusRegistry extends PrometheusMeterRegistry implements WildFlyRegistry {
    public WildFlyPrometheusRegistry() {
        super(PrometheusConfig.DEFAULT);
    }
}

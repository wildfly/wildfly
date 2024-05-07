package org.wildfly.extension.micrometer.prometheus;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

public class WildFlyPrometheusRegistry extends PrometheusMeterRegistry implements WildFlyRegistry {
    public WildFlyPrometheusRegistry() {
        super(PrometheusConfig.DEFAULT);
    }
}

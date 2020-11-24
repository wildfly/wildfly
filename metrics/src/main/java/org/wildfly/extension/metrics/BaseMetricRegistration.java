package org.wildfly.extension.metrics;

import org.wildfly.extension.metrics.api.MetricID;
import org.wildfly.extension.metrics.api.MetricRegistration;
import org.wildfly.extension.metrics.api.WildFlyMetric;
import org.wildfly.extension.metrics.api.WildFlyMetricMetadata;
import org.wildfly.extension.metrics.internal.WildFlyMetricRegistry;

public class BaseMetricRegistration extends MetricRegistration {

    private WildFlyMetricRegistry registry;

    public BaseMetricRegistration(WildFlyMetricRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void registerMetric(WildFlyMetricMetadata metadata, WildFlyMetric metric) {
        registry.register(metadata, metric);
    }

    @Override
    public void unregister() {
        for (MetricID id : unregistrationTasks) {
            registry.remove(id);
        }
    }
}

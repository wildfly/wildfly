/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.AdvancedCache;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Enumerates partition handling metrics.
 * @author Paul Ferraro
 */
public enum PartitionHandlingMetric implements Metric<AdvancedCache<?, ?>> {

    AVAILABILITY("availability", ModelType.STRING, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(AdvancedCache<?, ?> cache) {
            return new ModelNode(cache.getAvailability().name());
        }
    },
    ;
    private final AttributeDefinition definition;

    PartitionHandlingMetric(String name, ModelType type, AttributeAccess.Flag metricType) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                .setFlags(metricType)
                .setStorageRuntime()
                .build();
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.eviction.impl.PassivationManager;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Enumerates passivation metrics of a cache.
 * @author Paul Ferraro
 */
public enum CachePassivationMetric implements Metric<PassivationManager> {

    PASSIVATIONS("passivations", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(PassivationManager manager) {
            return new ModelNode(manager.getPassivations());
        }
    },
    ;
    private final AttributeDefinition definition;

    CachePassivationMetric(String name, ModelType type, AttributeAccess.Flag metricType) {
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

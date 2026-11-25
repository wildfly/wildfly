/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.AdvancedCache;
import org.infinispan.partitionhandling.AvailabilityMode;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;

/**
 * Enumerates partition handling operations.
 * @author Paul Ferraro
 */
public enum PartitionHandlingOperation implements RuntimeOperation<AdvancedCache<?, ?>> {

    FORCE_AVAILABLE("force-available") {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, AdvancedCache<?, ?> cache) {
            cache.setAvailability(AvailabilityMode.AVAILABLE);
            return null;
        }
    },
    ;
    private final OperationDefinition definition;

    PartitionHandlingOperation(String name) {
        this.definition = new SimpleOperationDefinitionBuilder(name, InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(ComponentResourceRegistration.PARTITION_HANDLING.getPathElement()))
                .setRuntimeOnly()
                .build();
    }

    @Override
    public OperationDefinition getOperationDefinition() {
        return this.definition;
    }
}

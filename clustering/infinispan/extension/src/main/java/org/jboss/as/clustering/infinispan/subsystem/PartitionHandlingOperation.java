/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.AdvancedCache;
import org.infinispan.partitionhandling.AvailabilityMode;
import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Enumerates partition handling operations.
 * @author Paul Ferraro
 */
public enum PartitionHandlingOperation implements Operation<AdvancedCache<?, ?>> {

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
        this.definition = new SimpleOperationDefinitionBuilder(name, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PartitionHandlingRuntimeResourceDefinition.PATH))
                .setRuntimeOnly()
                .build();
    }

    @Override
    public OperationDefinition getDefinition() {
        return this.definition;
    }
}

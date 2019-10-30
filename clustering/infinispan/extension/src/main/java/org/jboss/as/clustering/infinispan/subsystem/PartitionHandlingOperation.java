/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
        this.definition = new SimpleOperationDefinitionBuilder(name, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PartitionHandlingResourceDefinition.PATH)).setRuntimeOnly().build();
    }

    @Override
    public OperationDefinition getDefinition() {
        return this.definition;
    }
}

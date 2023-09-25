/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.PartitionHandlingResourceDefinition.Attribute.*;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PartitionHandlingConfiguration;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.partitionhandling.PartitionHandling;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Builds a service providing a {@link PartitionHandlingConfiguration}.
 * @author Paul Ferraro
 */
public class PartitionHandlingServiceConfigurator extends ComponentServiceConfigurator<PartitionHandlingConfiguration> {

    private volatile PartitionHandling whenSplit;
    private volatile MergePolicy mergePolicy;

    PartitionHandlingServiceConfigurator(PathAddress address) {
        super(CacheComponent.PARTITION_HANDLING, address);
    }

    @Override
    public PartitionHandlingConfiguration get() {
        return new ConfigurationBuilder().clustering().partitionHandling()
                .whenSplit(this.whenSplit)
                .mergePolicy(this.mergePolicy)
                .create();
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.whenSplit = PartitionHandling.valueOf(WHEN_SPLIT.resolveModelAttribute(context, model).asString());
        this.mergePolicy = MergePolicy.valueOf(MERGE_POLICY.resolveModelAttribute(context, model).asString());
        return this;
    }
}

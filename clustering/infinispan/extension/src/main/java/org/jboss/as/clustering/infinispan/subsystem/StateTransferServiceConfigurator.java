/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.StateTransferResourceDefinition.Attribute.CHUNK_SIZE;
import static org.jboss.as.clustering.infinispan.subsystem.StateTransferResourceDefinition.Attribute.TIMEOUT;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StateTransferConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class StateTransferServiceConfigurator extends ComponentServiceConfigurator<StateTransferConfiguration> {

    private volatile int chunkSize;
    private volatile long timeout;

    StateTransferServiceConfigurator(PathAddress address) {
        super(CacheComponent.STATE_TRANSFER, address);
    }

    @Override
    public StateTransferConfiguration get() {
        boolean timeoutEnabled = this.timeout > 0;
        return new ConfigurationBuilder().clustering().stateTransfer()
                .chunkSize(this.chunkSize)
                .fetchInMemoryState(true)
                .awaitInitialTransfer(timeoutEnabled)
                .timeout(timeoutEnabled ? this.timeout : Long.MAX_VALUE)
                .create();
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.chunkSize = CHUNK_SIZE.resolveModelAttribute(context, model).asInt();
        this.timeout = TIMEOUT.resolveModelAttribute(context, model).asLong();
        return this;
    }
}

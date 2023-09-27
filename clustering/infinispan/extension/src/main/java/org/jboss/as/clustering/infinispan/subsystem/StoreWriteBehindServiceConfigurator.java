/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.StoreWriteBehindResourceDefinition.Attribute.MODIFICATION_QUEUE_SIZE;

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class StoreWriteBehindServiceConfigurator extends ComponentServiceConfigurator<AsyncStoreConfiguration> {

    private volatile int queueSize;

    StoreWriteBehindServiceConfigurator(PathAddress address) {
        super(CacheComponent.STORE_WRITE, address.getParent());
    }

    @Override
    public AsyncStoreConfiguration get() {
        return new ConfigurationBuilder().persistence().addSoftIndexFileStore().async()
                .enable()
                .modificationQueueSize(this.queueSize)
                .create();
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.queueSize = MODIFICATION_QUEUE_SIZE.resolveModelAttribute(context, model).asInt();
        return this;
    }
}

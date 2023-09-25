/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.LockingResourceDefinition.Attribute.ACQUIRE_TIMEOUT;
import static org.jboss.as.clustering.infinispan.subsystem.LockingResourceDefinition.Attribute.CONCURRENCY;
import static org.jboss.as.clustering.infinispan.subsystem.LockingResourceDefinition.Attribute.ISOLATION;
import static org.jboss.as.clustering.infinispan.subsystem.LockingResourceDefinition.Attribute.STRIPING;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class LockingServiceConfigurator extends ComponentServiceConfigurator<LockingConfiguration> {

    private volatile long timeout;
    private volatile int concurrency;
    private volatile IsolationLevel isolation;
    private volatile boolean striping;

    LockingServiceConfigurator(PathAddress address) {
        super(CacheComponent.LOCKING, address);
    }

    @Override
    public LockingConfiguration get() {
        return new ConfigurationBuilder().locking()
                .lockAcquisitionTimeout(this.timeout)
                .concurrencyLevel(this.concurrency)
                .isolationLevel(this.isolation)
                .useLockStriping(this.striping)
                .create();
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.timeout = ACQUIRE_TIMEOUT.resolveModelAttribute(context, model).asLong();
        this.concurrency = CONCURRENCY.resolveModelAttribute(context, model).asInt();
        this.isolation = IsolationLevel.valueOf(ISOLATION.resolveModelAttribute(context, model).asString());
        this.striping = STRIPING.resolveModelAttribute(context, model).asBoolean();
        return this;
    }
}

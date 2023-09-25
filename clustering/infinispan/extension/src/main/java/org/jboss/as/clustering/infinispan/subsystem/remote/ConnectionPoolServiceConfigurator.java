/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConnectionPoolConfiguration;
import org.infinispan.client.hotrod.configuration.ExhaustedAction;
import org.jboss.as.clustering.infinispan.subsystem.ComponentServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Radoslav Husar
 */
public class ConnectionPoolServiceConfigurator extends ComponentServiceConfigurator<ConnectionPoolConfiguration> {

    private volatile ExhaustedAction exhaustedAction;
    private volatile int maxActive;
    private volatile long maxWait;
    private volatile long minEvictableIdleTime;
    private volatile int minIdle;

    ConnectionPoolServiceConfigurator(PathAddress address) {
        super(RemoteCacheContainerComponent.CONNECTION_POOL, address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.exhaustedAction = ExhaustedAction.valueOf(ConnectionPoolResourceDefinition.Attribute.EXHAUSTED_ACTION.resolveModelAttribute(context, model).asString());
        this.maxActive = ConnectionPoolResourceDefinition.Attribute.MAX_ACTIVE.resolveModelAttribute(context, model).asInt(-1);
        this.maxWait = ConnectionPoolResourceDefinition.Attribute.MAX_WAIT.resolveModelAttribute(context, model).asLong(-1L);
        this.minEvictableIdleTime = ConnectionPoolResourceDefinition.Attribute.MIN_EVICTABLE_IDLE_TIME.resolveModelAttribute(context, model).asLong();
        this.minIdle = ConnectionPoolResourceDefinition.Attribute.MIN_IDLE.resolveModelAttribute(context, model).asInt();
        return this;
    }

    @Override
    public ConnectionPoolConfiguration get() {
        return new ConfigurationBuilder().connectionPool()
                .exhaustedAction(this.exhaustedAction)
                .maxActive(this.maxActive)
                .maxWait(this.maxWait)
                .minEvictableIdleTime(this.minEvictableIdleTime)
                .minIdle(this.minIdle)
                .create();
    }
}

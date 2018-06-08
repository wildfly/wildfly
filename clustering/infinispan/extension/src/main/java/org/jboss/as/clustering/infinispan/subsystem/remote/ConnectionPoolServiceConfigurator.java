/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
        this.maxActive = ConnectionPoolResourceDefinition.Attribute.MAX_ACTIVE.resolveModelAttribute(context, model).asInt();
        this.maxWait = ConnectionPoolResourceDefinition.Attribute.MAX_WAIT.resolveModelAttribute(context, model).asLong();
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

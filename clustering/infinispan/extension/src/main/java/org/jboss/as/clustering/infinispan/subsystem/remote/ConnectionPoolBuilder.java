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
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.subsystem.ComponentBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * @author Radoslav Husar
 */
public class ConnectionPoolBuilder extends ComponentBuilder<ConnectionPoolConfiguration> implements ResourceServiceBuilder<ConnectionPoolConfiguration> {

    private volatile ExhaustedAction exhaustedAction;
    private volatile ConnectionPoolStrategy connectionPoolStrategy;
    private volatile int maxActive;
    private volatile int maxIdle;
    private volatile int maxTotal;
    private volatile long maxWait;
    private volatile long minEvictableIdleTime;
    private volatile int minIdle;
    private volatile int numTestsPerEvictionRun;
    private volatile boolean testOnBorrow;
    private volatile boolean testOnReturn;
    private volatile boolean testWhileIdle;
    private volatile long timeBetweenEvictionRuns;

    ConnectionPoolBuilder(PathAddress address) {
        super(RemoteCacheContainerComponent.CONNECTION_POOL, address);
    }

    @Override
    public Builder<ConnectionPoolConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.exhaustedAction = ExhaustedAction.valueOf(ConnectionPoolResourceDefinition.Attribute.EXHAUSTED_ACTION.resolveModelAttribute(context, model).asString());
        this.connectionPoolStrategy = ModelNodes.asEnum(ConnectionPoolResourceDefinition.Attribute.STRATEGY.resolveModelAttribute(context, model), ConnectionPoolStrategy.class);
        this.maxActive = ConnectionPoolResourceDefinition.Attribute.MAX_ACTIVE.resolveModelAttribute(context, model).asInt();
        this.maxIdle = ConnectionPoolResourceDefinition.Attribute.MAX_IDLE.resolveModelAttribute(context, model).asInt();
        this.maxTotal = ConnectionPoolResourceDefinition.Attribute.MAX_TOTAL.resolveModelAttribute(context, model).asInt();
        this.maxWait = ConnectionPoolResourceDefinition.Attribute.MAX_WAIT.resolveModelAttribute(context, model).asLong();
        this.minEvictableIdleTime = ConnectionPoolResourceDefinition.Attribute.MIN_EVICTABLE_IDLE_TIME.resolveModelAttribute(context, model).asLong();
        this.minIdle = ConnectionPoolResourceDefinition.Attribute.MIN_IDLE.resolveModelAttribute(context, model).asInt();
        this.numTestsPerEvictionRun = ConnectionPoolResourceDefinition.Attribute.NUM_TESTS_PER_EVICTION_RUN.resolveModelAttribute(context, model).asInt();
        this.testOnBorrow = ConnectionPoolResourceDefinition.Attribute.TEST_ON_BORROW.resolveModelAttribute(context, model).asBoolean();
        this.testOnReturn = ConnectionPoolResourceDefinition.Attribute.TEST_ON_RETURN.resolveModelAttribute(context, model).asBoolean();
        this.testWhileIdle = ConnectionPoolResourceDefinition.Attribute.TEST_WHILE_IDLE.resolveModelAttribute(context, model).asBoolean();
        this.timeBetweenEvictionRuns = ConnectionPoolResourceDefinition.Attribute.TIME_BETWEEN_EVICTION_RUNS.resolveModelAttribute(context, model).asLong();
        return this;
    }

    @Override
    public ConnectionPoolConfiguration getValue() throws IllegalStateException, IllegalArgumentException {
        return new ConfigurationBuilder().connectionPool()
                .exhaustedAction(this.exhaustedAction)
                .lifo(this.connectionPoolStrategy == ConnectionPoolStrategy.LIFO)
                .maxActive(this.maxActive)
                .maxIdle(this.maxIdle)
                .maxTotal(this.maxTotal)
                .maxWait(this.maxWait)
                .minEvictableIdleTime(this.minEvictableIdleTime)
                .minIdle(this.minIdle)
                .numTestsPerEvictionRun(this.numTestsPerEvictionRun)
                .testOnBorrow(this.testOnBorrow)
                .testOnReturn(this.testOnReturn)
                .testWhileIdle(this.testWhileIdle)
                .timeBetweenEvictionRuns(this.timeBetweenEvictionRuns)
                .create();
    }
}

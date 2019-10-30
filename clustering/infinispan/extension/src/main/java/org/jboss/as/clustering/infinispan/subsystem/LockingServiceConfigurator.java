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

import static org.jboss.as.clustering.infinispan.subsystem.LockingResourceDefinition.Attribute.ACQUIRE_TIMEOUT;
import static org.jboss.as.clustering.infinispan.subsystem.LockingResourceDefinition.Attribute.CONCURRENCY;
import static org.jboss.as.clustering.infinispan.subsystem.LockingResourceDefinition.Attribute.ISOLATION;
import static org.jboss.as.clustering.infinispan.subsystem.LockingResourceDefinition.Attribute.STRIPING;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.dmr.ModelNodes;
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
        this.isolation = ModelNodes.asEnum(ISOLATION.resolveModelAttribute(context, model), IsolationLevel.class);
        this.striping = STRIPING.resolveModelAttribute(context, model).asBoolean();
        return this;
    }
}

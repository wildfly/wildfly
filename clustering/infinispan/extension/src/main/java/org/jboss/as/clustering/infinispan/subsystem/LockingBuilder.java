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
import org.infinispan.configuration.cache.LockingConfigurationBuilder;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public class LockingBuilder extends CacheComponentBuilder<LockingConfiguration> implements ResourceServiceBuilder<LockingConfiguration> {

    private final LockingConfigurationBuilder builder = new ConfigurationBuilder().locking();

    LockingBuilder(String containerName, String cacheName) {
        super(CacheComponent.LOCKING, containerName, cacheName);
    }

    @Override
    public LockingConfiguration getValue() {
        return this.builder.create();
    }

    @Override
    public Builder<LockingConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.builder.lockAcquisitionTimeout(ACQUIRE_TIMEOUT.getDefinition().resolveModelAttribute(context, model).asLong());
        this.builder.concurrencyLevel(CONCURRENCY.getDefinition().resolveModelAttribute(context, model).asInt());
        this.builder.isolationLevel(ModelNodes.asEnum(ISOLATION.getDefinition().resolveModelAttribute(context, model), IsolationLevel.class));
        this.builder.useLockStriping(STRIPING.getDefinition().resolveModelAttribute(context, model).asBoolean());
        return this;
    }
}

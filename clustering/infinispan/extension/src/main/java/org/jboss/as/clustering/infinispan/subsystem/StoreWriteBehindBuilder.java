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

import static org.jboss.as.clustering.infinispan.subsystem.StoreWriteBehindResourceDefinition.Attribute.*;

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public class StoreWriteBehindBuilder extends CacheComponentBuilder<AsyncStoreConfiguration> implements ResourceServiceBuilder<AsyncStoreConfiguration> {

    private final AsyncStoreConfigurationBuilder<?> builder = new ConfigurationBuilder().persistence().addSingleFileStore().async();

    StoreWriteBehindBuilder(String containerName, String cacheName) {
        super(CacheComponent.STORE_WRITE, containerName, cacheName);
    }

    @Override
    public AsyncStoreConfiguration getValue() throws IllegalStateException, IllegalArgumentException {
        return this.builder.create();
    }

    @Override
    public Builder<AsyncStoreConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.builder.flushLockTimeout(FLUSH_LOCK_TIMEOUT.getDefinition().resolveModelAttribute(context, model).asLong());
        this.builder.modificationQueueSize(MODIFICATION_QUEUE_SIZE.getDefinition().resolveModelAttribute(context, model).asInt());
        this.builder.shutdownTimeout(SHUTDOWN_TIMEOUT.getDefinition().resolveModelAttribute(context, model).asLong());
        this.builder.threadPoolSize(THREAD_POOL_SIZE.getDefinition().resolveModelAttribute(context, model).asInt());
        return this;
    }
}

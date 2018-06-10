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

import static org.jboss.as.clustering.infinispan.subsystem.StoreWriteBehindResourceDefinition.Attribute.MODIFICATION_QUEUE_SIZE;
import static org.jboss.as.clustering.infinispan.subsystem.StoreWriteBehindResourceDefinition.Attribute.THREAD_POOL_SIZE;

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
    private volatile int poolSize;

    StoreWriteBehindServiceConfigurator(PathAddress address) {
        super(CacheComponent.STORE_WRITE, address.getParent());
    }

    @Override
    public AsyncStoreConfiguration get() {
        return new ConfigurationBuilder().persistence().addSingleFileStore().async()
                .modificationQueueSize(this.queueSize)
                .threadPoolSize(this.poolSize)
                .create();
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.queueSize = MODIFICATION_QUEUE_SIZE.resolveModelAttribute(context, model).asInt();
        this.poolSize = THREAD_POOL_SIZE.resolveModelAttribute(context, model).asInt();
        return this;
    }
}

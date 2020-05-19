/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import java.util.function.Function;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.BinaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.OperationExecutor;
import org.jboss.as.clustering.controller.OperationFunction;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;

/**
 * @author Paul Ferraro
 */
public abstract class CacheOperationExecutor<C> implements OperationExecutor<C>, Function<Cache<?, ?>, C> {

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;
    private final BinaryCapabilityNameResolver resolver;

    CacheOperationExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        this(executors, BinaryCapabilityNameResolver.PARENT_CHILD);
    }

    CacheOperationExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors, BinaryCapabilityNameResolver resolver) {
        this.executors = executors;
        this.resolver = resolver;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode op, Operation<C> operation) throws OperationFailedException {
        ServiceName name = InfinispanCacheRequirement.CACHE.getServiceName(context, this.resolver);
        FunctionExecutor<Cache<?, ?>> executor = this.executors.get(name);
        return (executor != null) ? executor.execute(new OperationFunction<>(context, op, this, operation)) : null;
    }
}

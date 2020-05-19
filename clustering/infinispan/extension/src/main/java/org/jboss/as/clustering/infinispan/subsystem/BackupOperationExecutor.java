/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.xsite.XSiteAdminOperations;
import org.jboss.as.clustering.controller.BinaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.OperationExecutor;
import org.jboss.as.clustering.controller.OperationFunction;
import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;

/**
 * Operation handler for backup site operations.
 * @author Paul Ferraro
 */
public class BackupOperationExecutor implements OperationExecutor<Map.Entry<String, XSiteAdminOperations>> {

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    public BackupOperationExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode operation, Operation<Map.Entry<String, XSiteAdminOperations>> executable) throws OperationFailedException {
        ServiceName name = InfinispanCacheRequirement.CACHE.getServiceName(context, BinaryCapabilityNameResolver.GRANDPARENT_PARENT);
        Function<Cache<?, ?>, Map.Entry<String, XSiteAdminOperations>> mapper = new Function<Cache<?, ?>, Map.Entry<String, XSiteAdminOperations>>() {
            @Override
            public Map.Entry<String, XSiteAdminOperations> apply(Cache<?, ?> cache) {
                String site = context.getCurrentAddressValue();
                return new AbstractMap.SimpleImmutableEntry<>(site, cache.getAdvancedCache().getComponentRegistry().getLocalComponent(XSiteAdminOperations.class));
            }
        };
        FunctionExecutor<Cache<?, ?>> executor = this.executors.get(name);
        return (executor != null) ? executor.execute(new OperationFunction<>(context, operation, mapper, executable)) : null;
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreSourceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class ClusterPassivationStoreAdd extends PassivationStoreAdd {

    public ClusterPassivationStoreAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    Collection<ServiceController<?>> installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        ClusteredBackingCacheEntryStoreSourceService<?, ?, ?> service = new ClusteredBackingCacheEntryStoreSourceService<Serializable, Cacheable<Serializable>, Serializable>(name);
        ClusteredBackingCacheEntryStoreConfig config = service.getValue();
        config.setCacheContainer(ClusterPassivationStoreResourceDefinition.CACHE_CONTAINER.resolveModelAttribute(context, model).asString());
        ModelNode beanCacheNode = ClusterPassivationStoreResourceDefinition.BEAN_CACHE.resolveModelAttribute(context, model);
        if (beanCacheNode.isDefined()) {
            config.setBeanCache(beanCacheNode.asString());
        }
        config.setClientMappingCache(ClusterPassivationStoreResourceDefinition.CLIENT_MAPPINGS_CACHE.resolveModelAttribute(context, model).asString());
        config.setPassivateEventsOnReplicate(ClusterPassivationStoreResourceDefinition.PASSIVATE_EVENTS_ON_REPLICATE.resolveModelAttribute(context, model).asBoolean());

        ServiceName serviceName = ClusteredBackingCacheEntryStoreSourceService.getPassivationStoreClusterNameServiceName(name);
        ServiceRegistry registry = context.getServiceRegistry(true);
        if (registry.getService(serviceName) != null) {
            context.removeService(serviceName);
        }
        InjectedValue<String> clusterName = new InjectedValue<String>();
        ServiceController<?> controller = context.getServiceTarget().addService(serviceName, new ValueService<String>(clusterName))
                .addDependency(ClusteredBackingCacheEntryStoreSourceService.getCacheContainerClusterNameServiceName(config.getCacheContainer()), String.class, clusterName)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
        return Arrays.asList(this.installBackingCacheEntryStoreSourceService(service, context, model, verificationHandler), controller);
    }
}

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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 */
public class CacheContainerRemove extends AbstractRemoveStepHandler {

    public static final CacheContainerRemove INSTANCE = new CacheContainerRemove();

    private List<Property> remainingCaches = null ;

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {

        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final String containerName = address.getLastElement().getValue();

        // remove any existing cache entries
        for (Property cache : remainingCaches) {
            String cacheName = cache.getName();
            ModelNode cacheModel = cache.getValue();

            // remove JNDI name
            String jndiName = (cacheModel.hasDefined(ModelKeys.JNDI_NAME) ? InfinispanJndiName.toJndiName(cacheModel.get(ModelKeys.JNDI_NAME).asString()) : InfinispanJndiName.defaultCacheJndiName(containerName, cacheName)).getAbsoluteName();
            ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
            context.removeService(bindInfo.getBinderServiceName());
            // remove cache configuration service
            context.removeService(CacheConfigurationService.getServiceName(containerName, cacheName));
            // remove cache service
            context.removeService(CacheService.getServiceName(containerName, cacheName)) ;
        }

        // need to remove all container-related services started, in reverse order
        // remove the BinderService entry
        String jndiName = (model.hasDefined(ModelKeys.JNDI_NAME) ?
                InfinispanJndiName.toJndiName(model.get(ModelKeys.JNDI_NAME).asString()) :
                InfinispanJndiName.defaultCacheContainerJndiName(containerName)).getAbsoluteName();
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        context.removeService(bindInfo.getBinderServiceName()) ;
        // remove the cache container
        context.removeService(EmbeddedCacheManagerService.getServiceName(containerName));
        // check if a channel was installed
        ServiceName channelServiceName = ChannelService.getServiceName(containerName) ;
        ServiceController<?> channelServiceController = context.getServiceRegistry(false).getService(channelServiceName);
        if (channelServiceController != null) {
            context.removeService(channelServiceName);
        }

    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }

    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        // override performRemove() to pick up all the caches we need to remove and their JNDI names
        // before they get wiped in the MODEL phase
        remainingCaches = new ArrayList<Property>() ;

        // populate the list of installed caches in this container
        String[] cacheTypes = {ModelKeys.LOCAL_CACHE, ModelKeys.INVALIDATION_CACHE, ModelKeys.REPLICATED_CACHE, ModelKeys.DISTRIBUTED_CACHE} ;
        for (String cacheType : cacheTypes) {
            // get the caches of a type
            ModelNode caches = model.get(cacheType) ;
            if (caches.isDefined() && caches.getType() == ModelType.OBJECT) {
                List<Property> cacheList = caches.asPropertyList() ;
                // add a clone of each cache to the list
                for (Property cache : cacheList) {
                    String cacheName = cache.getName() ;
                    ModelNode cacheValue = cache.getValue().clone();
                    remainingCaches.add(new Property(cacheName, cacheValue));
                }
            }
        }
        super.performRemove(context, operation, model);
    }
}

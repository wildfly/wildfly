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

import java.util.Locale;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Returns a ModelNode LIST of operations which can re-create the subsystem.
 *
 * @author Paul Ferraro
 */
public class InfinispanSubsystemDescribe implements OperationStepHandler, DescriptionProvider {

    public static final InfinispanSubsystemDescribe INSTANCE = new InfinispanSubsystemDescribe();

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getSubsystemDescribeDescription(locale);
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode result = context.getResult();

        PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement());
        ModelNode subModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        // an add operation to recreate the subsystem ModelNode in its current state
        result.add(InfinispanSubsystemAdd.createOperation(rootAddress.toModelNode(), subModel));

        // add operations to create the cache containers
        if (subModel.hasDefined(ModelKeys.CACHE_CONTAINER)) {
            // list of (cacheContainerName, containerModel)
            for (Property container : subModel.get(ModelKeys.CACHE_CONTAINER).asPropertyList()) {
                ModelNode address = rootAddress.toModelNode();
                address.add(ModelKeys.CACHE_CONTAINER, container.getName());
                result.add(CacheContainerAdd.createOperation(address, container.getValue()));

                // list of (cacheType, OBJECT)
                for (Property cacheTypeList : container.getValue().asPropertyList()) {
                    // add commands for local caches
                    if (cacheTypeList.getName().equals(ModelKeys.LOCAL_CACHE)) {
                        for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                            ModelNode cacheAddress = address.clone() ;
                            cacheAddress.add(ModelKeys.LOCAL_CACHE, cache.getName()) ;
                            result.add(LocalCacheAdd.createOperation(cacheAddress, cache.getValue()));
                        }
                    // add commands for invalidation caches
                    } else if (cacheTypeList.getName().equals(ModelKeys.INVALIDATION_CACHE)) {
                        for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                            ModelNode cacheAddress = address.clone() ;
                            cacheAddress.add(ModelKeys.INVALIDATION_CACHE, cache.getName()) ;
                            result.add(InvalidationCacheAdd.createOperation(cacheAddress, cache.getValue()));
                        }
                    // add commands for distributed caches
                    } else if (cacheTypeList.getName().equals(ModelKeys.REPLICATED_CACHE)) {
                        for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                            ModelNode cacheAddress = address.clone() ;
                            cacheAddress.add(ModelKeys.REPLICATED_CACHE, cache.getName()) ;
                            result.add(ReplicatedCacheAdd.createOperation(cacheAddress, cache.getValue()));
                        }
                    // add commands for distributed caches
                    } else if (cacheTypeList.getName().equals(ModelKeys.DISTRIBUTED_CACHE)) {
                        for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                            ModelNode cacheAddress = address.clone() ;
                            cacheAddress.add(ModelKeys.DISTRIBUTED_CACHE, cache.getName()) ;
                            result.add(DistributedCacheAdd.createOperation(cacheAddress, cache.getValue()));
                        }
                    }
                }
            }
        }

        context.completeStep();
    }
}

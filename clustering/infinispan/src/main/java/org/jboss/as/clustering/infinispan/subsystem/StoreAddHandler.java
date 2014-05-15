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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Base add operation handler for a cache store.
 *
 * This class needs to do the following:
 * <ol>
 * <li>check that its parent has no existing defined cache store</li>
 * <li>process its model attributes</li>
 * <li>create any child resources required for the store resource, such as a set of properties</li>
 * </ol>
 *
 * @author Richard Achmatowicz
 */
public class StoreAddHandler extends AbstractAddStepHandler {

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        final ModelNode model = resource.getModel();

        // need to check that the parent does not contain some other cache store ModelNode
        if (isCacheStoreDefined(context, operation)) {
            String storeName = getDefinedCacheStore(context, operation);
            throw InfinispanLogger.ROOT_LOGGER.cacheStoreAlreadyDefined(storeName);
        }

        // Process attributes
        for (AttributeDefinition attribute: StoreResourceDefinition.PARAMETERS) {
            // we use PROPERTIES only to allow the user to pass in a list of properties on store add commands
            // don't copy these into the model
            if (attribute.getName().equals(StoreResourceDefinition.PROPERTIES.getName())) continue;
            attribute.validateAndSet(operation, model);
        }

        // The cache config parameters  <property name=>value</property>
        if (operation.hasDefined(ModelKeys.PROPERTIES)) {
            for (Property property: operation.get(ModelKeys.PROPERTIES).asPropertyList()) {
                // create a new property=name resource
                Resource param = context.createResource(PathAddress.pathAddress(PathElement.pathElement(ModelKeys.PROPERTY, property.getName())));
                ModelNode value = property.getValue();
                if (!value.isDefined()) {
                    throw InfinispanLogger.ROOT_LOGGER.propertyValueNotDefined(property.getName());
                }
                // set the value of the property
                StorePropertyResourceDefinition.VALUE.validateAndSet(value, param.getModel());
            }
        }
    }

    private static boolean isCacheStoreDefined(OperationContext context, ModelNode operation) {
         ModelNode cache = getCache(context, getCacheAddress(operation));

         return (hasCustomStore(cache) || hasFileStore(cache) ||
                 hasStringKeyedJdbcStore(cache) || hasBinaryKeyedJdbcStore(cache) || hasMixedKeyedJdbcStore(cache) ||
                 hasRemoteStore(cache));
    }

    private static String getDefinedCacheStore(OperationContext context, ModelNode operation) {
        ModelNode cache = getCache(context, getCacheAddress(operation));
        if (hasCustomStore(cache))
            return ModelKeys.STORE;
        else if (hasFileStore(cache))
            return ModelKeys.FILE_STORE;
        else if (hasStringKeyedJdbcStore(cache))
            return ModelKeys.STRING_KEYED_JDBC_STORE;
        else if (hasBinaryKeyedJdbcStore(cache))
            return ModelKeys.BINARY_KEYED_JDBC_STORE;
        else if (hasMixedKeyedJdbcStore(cache))
            return ModelKeys.MIXED_KEYED_JDBC_STORE;
        else if (hasRemoteStore(cache))
            return ModelKeys.REMOTE_STORE;
        else
            return null;
    }

    private static PathAddress getCacheAddress(ModelNode operation) {
        PathAddress cacheStoreAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        PathAddress cacheAddress = cacheStoreAddress.subAddress(0, cacheStoreAddress.size()-1);
        return cacheAddress;
    }

    private static ModelNode getCache(OperationContext context, PathAddress cacheAddress) {
        //Resource rootResource = context.readResourceFromRoot(cacheAddress, true);
        //ModelNode cache = rootResource.getModel();
        ModelNode cache = Resource.Tools.readModel(context.readResourceFromRoot(cacheAddress));
        return cache;
    }

    private static boolean hasCustomStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.STORE) && cache.get(ModelKeys.STORE, ModelKeys.STORE_NAME).isDefined();
    }

    private static boolean hasFileStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.FILE_STORE) && cache.get(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME).isDefined();
    }

    private static boolean hasStringKeyedJdbcStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.STRING_KEYED_JDBC_STORE) && cache.get(ModelKeys.STRING_KEYED_JDBC_STORE, ModelKeys.STRING_KEYED_JDBC_STORE_NAME).isDefined();
    }

    private static boolean hasBinaryKeyedJdbcStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.BINARY_KEYED_JDBC_STORE) && cache.get(ModelKeys.BINARY_KEYED_JDBC_STORE, ModelKeys.BINARY_KEYED_JDBC_STORE_NAME).isDefined();
    }

    private static boolean hasMixedKeyedJdbcStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.MIXED_KEYED_JDBC_STORE) && cache.get(ModelKeys.MIXED_KEYED_JDBC_STORE, ModelKeys.MIXED_KEYED_JDBC_STORE_NAME).isDefined();
    }

    private static boolean hasRemoteStore(ModelNode cache) {
        return cache.hasDefined(ModelKeys.REMOTE_STORE) && cache.get(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME).isDefined();
    }
}
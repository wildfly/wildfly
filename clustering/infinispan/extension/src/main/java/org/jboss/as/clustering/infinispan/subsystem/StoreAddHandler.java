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

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
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
 * @author Paul Ferraro
 */
public class StoreAddHandler extends AbstractAddStepHandler {

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        PathAddress address = Operations.getPathAddress(operation);
        PathAddress cacheAddress = address.subAddress(0, address.size() - 1);

        ModelNode cache = Resource.Tools.readModel(context.readResourceFromRoot(cacheAddress));

        for (StoreType type: StoreType.values()) {
            if (cache.hasDefined(type.pathElement().getKey()) && cache.get(type.pathElement().getKeyValuePair()).isDefined()) {
                throw InfinispanLogger.ROOT_LOGGER.cacheStoreAlreadyDefined(type.pathElement().getKey());
            }
        }

        ModelNode model = resource.getModel();
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
                Resource param = context.createResource(PathAddress.pathAddress(StorePropertyResourceDefinition.pathElement(property.getName())));
                ModelNode value = property.getValue();
                if (!value.isDefined()) {
                    throw InfinispanLogger.ROOT_LOGGER.propertyValueNotDefined(property.getName());
                }
                // set the value of the property
                StorePropertyResourceDefinition.VALUE.validateAndSet(value, param.getModel());
            }
        }
    }
}
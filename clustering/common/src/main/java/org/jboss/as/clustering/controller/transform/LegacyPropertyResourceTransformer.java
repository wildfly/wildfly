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

package org.jboss.as.clustering.controller.transform;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Radoslav Husar
 * @version October 2015
 */
public class LegacyPropertyResourceTransformer implements ResourceTransformer {

    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
        final ModelNode model = resource.getModel();
        final ModelNode properties = model.remove(PROPERTIES);
        final ResourceTransformationContext parentContext = context.addTransformedResourceFromRoot(address, resource);

        transformPropertiesToChildrenResources(properties, address, parentContext);

        context.processChildren(resource);
    }

    public static void transformPropertiesToChildrenResources(ModelNode properties, PathAddress address, ResourceTransformationContext parentContext) {

        if (properties.isDefined()) {
            for (final Property property : properties.asPropertyList()) {
                String key = property.getName();
                ModelNode value = property.getValue();
                Resource propertyResource = Resource.Factory.create();
                propertyResource.getModel().get(VALUE).set(value);
                PathAddress absoluteAddress = address.append(PROPERTY, key);
                parentContext.addTransformedResourceFromRoot(absoluteAddress, propertyResource);
            }
        }
    }
}

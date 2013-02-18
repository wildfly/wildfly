/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class LoggingResourceTransformer implements ResourceTransformer {

    private final AttributeDefinition[] removableAttributes;

    public LoggingResourceTransformer(final AttributeDefinition... removableAttributes) {
        this.removableAttributes = removableAttributes;
    }

    @Override
    public void transformResource(final ResourceTransformationContext context, final PathAddress address, final Resource resource)
            throws OperationFailedException {
        doTransform(resource);
        final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        childContext.processChildren(resource);
    }

    private void doTransform(final Resource resource) {
        final ModelNode model = resource.getModel();
        for (AttributeDefinition attribute : removableAttributes) {
            if (model.has(attribute.getName())) {
                LoggingLogger.ROOT_LOGGER.debugf("Removing attribute '%s' during transform", attribute.getName());
                model.remove(attribute.getName());
            }
        }
    }
}

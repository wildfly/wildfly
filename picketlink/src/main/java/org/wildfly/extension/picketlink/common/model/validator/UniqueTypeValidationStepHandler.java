/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.picketlink.common.model.validator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.ModelElement;

import java.util.Set;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author Pedro Igor
 */
public abstract class UniqueTypeValidationStepHandler implements ModelValidationStepHandler {

    private final ModelElement element;

    public UniqueTypeValidationStepHandler(ModelElement element) {
        this.element = element;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validateType(context, operation);
    }

    protected void validateType(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress pathAddress = context.getCurrentAddress();
        String elementName = context.getCurrentAddressValue();
        ModelNode typeNode = context.readResource(EMPTY_ADDRESS, false).getModel();
        String type = getType(context, typeNode);
        PathAddress parentAddress = pathAddress.getParent();
        Set<Resource.ResourceEntry> children = context.readResourceFromRoot(parentAddress, true).getChildren(this.element.getName());

        for (Resource.ResourceEntry child : children) {
            String existingResourceName = child.getName();
            String existingType = getType(context, child.getModel());

            if (!elementName.equals(existingResourceName) && (type.equals(existingType))) {
                throw ROOT_LOGGER.typeAlreadyDefined(type);
            }
        }
    }

    protected abstract String getType(OperationContext context, ModelNode model) throws OperationFailedException;
}

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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.registry.Resource.ResourceEntry;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author Pedro Igor
 */
public class RequiredChildValidationStepHandler implements ModelValidationStepHandler {

    private final ModelElement childElement;

    public RequiredChildValidationStepHandler(ModelElement childElement) {
        this.childElement = childElement;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validateRequiredChild(context, operation);
    }

    protected void validateRequiredChild(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        Set<ResourceEntry> children = resource.getChildren(this.childElement.getName());

        if (children.isEmpty()) {
            throw ROOT_LOGGER.requiredChild(pathAddress.getLastElement().toString(), this.childElement.getName());
        }
    }
}

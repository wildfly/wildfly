/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.NoSuchElementException;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainControllerMessages;
import org.jboss.as.domain.controller.operations.coordination.ServerOperationResolver;
import org.jboss.as.domain.controller.resources.ServerGroupResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * Validates that the new profile is ok before setting in the model. Setting the servers to be in the restart-required state
 * is handled by ServerOperationResolver.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerGroupProfileWriteAttributeHandler extends ModelOnlyWriteAttributeHandler {

    public static final ServerGroupProfileWriteAttributeHandler INSTANCE = new ServerGroupProfileWriteAttributeHandler();

    private ServerGroupProfileWriteAttributeHandler() {
        super(ServerGroupResourceDefinition.PROFILE);
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode currentValue, Resource resource) throws OperationFailedException {
        if (newValue.equals(currentValue)) {
            //Set an attachment to avoid propagation to the servers, we don't want them to go into restart-required if nothing changed
            ServerOperationResolver.addToDontPropagateToServersAttachment(context, operation);
        }

        // Validate the profile reference.

        // Future proofing: We resolve the profile in Stage.MODEL even though system properties may not be available yet
        // solely because currently the attribute doesn't support expressions. In the future if system properties
        // can safely be resolved in stage model, this profile attribute can be changed and this will still work.
        String profile = ServerGroupResourceDefinition.PROFILE.resolveModelAttribute(context, resource.getModel()).asString();
        try {
            context.readResourceFromRoot(PathAddress.pathAddress(PathElement.pathElement(ServerGroupResourceDefinition.PROFILE.getName(), profile)));
        } catch (Exception e) {
            throw DomainControllerMessages.MESSAGES.noProfileCalled(profile);
        }
    }
}

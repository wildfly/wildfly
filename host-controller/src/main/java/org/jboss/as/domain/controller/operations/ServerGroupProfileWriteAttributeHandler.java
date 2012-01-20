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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainControllerMessages;
import org.jboss.dmr.ModelNode;

/**
 * Validates that the new profile is ok before setting in the model. Setting the servers to be in the restart-required state
 * is handled by ServerOperationResolver.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerGroupProfileWriteAttributeHandler extends StringLengthValidatingHandler {

    public static final ServerGroupProfileWriteAttributeHandler INSTANCE = new ServerGroupProfileWriteAttributeHandler();

    private ServerGroupProfileWriteAttributeHandler() {
        super(1, false);
    }

    @Override
    protected void modelChanged(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode currentValue) throws OperationFailedException {
        if (!newValue.equals(currentValue)) {
            validateProfileName(context, newValue.asString());
        }

        context.completeStep();
    }

    private void validateProfileName(final OperationContext context, final String profileName) throws OperationFailedException {
        final Resource profile = context.getOriginalRootResource().getChild(PathElement.pathElement(PROFILE, profileName));
        if (profile == null) {
            throw DomainControllerMessages.MESSAGES.noProfileCalled(profileName);
        }
    }
}

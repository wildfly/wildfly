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
package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.host.controller.HostControllerMessages;
import org.jboss.dmr.ModelNode;

/**
 * Writes the group attribute of a server group and validates the new value. ServerOperationResolver is responsible for
 * putting the affected server in the restart-required state.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerConfigGroupWriteAttributeHandler extends StringLengthValidatingHandler {

    public static final ServerConfigGroupWriteAttributeHandler INSTANCE = new ServerConfigGroupWriteAttributeHandler();

    public ServerConfigGroupWriteAttributeHandler() {
        super(1, false);
    }

    @Override
    protected void modelChanged(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode currentValue) throws OperationFailedException {

        if (newValue.equals(currentValue)) {
            //If we don't throw this exception here the ServerOperationHandler won't know something went wrong
            //and will put the server into restart-required although nothing changed
            throw HostControllerMessages.MESSAGES.writeAttributeNotChanged(newValue.asString());
        }

        if (context.getOriginalRootResource().getChild(PathElement.pathElement(SERVER_GROUP, newValue.asString())) == null) {
            throw HostControllerMessages.MESSAGES.noServerGroupCalled(newValue.asString());
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
}

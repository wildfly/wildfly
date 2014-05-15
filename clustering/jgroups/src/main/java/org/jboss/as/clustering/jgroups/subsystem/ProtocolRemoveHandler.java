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

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.List;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Implements the operation /subsystem=jgroups/stack=X/protocol=Y:remove()
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ProtocolRemoveHandler implements OperationStepHandler {

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        // read /subsystem=jgroups/stack=* for update
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode subModel = resource.getModel();

         // validate the protocol type to be added
        ModelNode type = ProtocolResourceDefinition.TYPE.validateOperation(operation);
        PathElement protocolRelativePath = PathElement.pathElement(ModelKeys.PROTOCOL, type.asString());

        // if child resource already exists, throw OFE
        // TODO not sure if this works ex expected - it may only confirm a registered resource
        if (!resource.hasChild(protocolRelativePath))  {
            throw JGroupsLogger.ROOT_LOGGER.protocolNotDefined(protocolRelativePath.toString());
        }

        // remove the resource and its children
        context.removeResource(PathAddress.pathAddress(protocolRelativePath));

        // get the current list of protocol names and remove the protocol
        // copy all elements of the list except the one to remove
        // this list is used to maintain order
        List<ModelNode> protocols = subModel.get(ModelKeys.PROTOCOLS).asList();
        ModelNode newList = new ModelNode();
        if (protocols == null) {
            // something wrong
        }
        for (ModelNode protocol : protocols) {
            if (!protocol.asString().equals(type.asString())) {
               newList.add(protocol);
            }
        }
        subModel.get(ModelKeys.PROTOCOLS).set(newList);

        // This needs a reload
        reloadRequiredStep(context);
        context.stepCompleted();
    }

    /**
     * Add a step triggering the {@linkplain org.jboss.as.controller.OperationContext#reloadRequired()} in case the
     * the cache service is installed, since the transport-config operations need a reload/restart and can't be
     * applied to the runtime directly.
     *
     * @param context the operation context
     */
    void reloadRequiredStep(final OperationContext context) {
        if (context.getProcessType().isServer() &&  !context.isBooting()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    // add some condition here if reload needs to be conditional on context
                    // e.g. if a service is not installed, don't do a reload
                    context.reloadRequired();
                    context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}

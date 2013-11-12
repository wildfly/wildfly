/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainControllerMessages;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class ServerGroupRemoveHandler extends AbstractRemoveStepHandler {

    // We need the host controller info since the host name is not available during boot-time
    private final LocalHostControllerInfo hostControllerInfo;
    public ServerGroupRemoveHandler(final LocalHostControllerInfo hostControllerInfo) {
        this.hostControllerInfo = hostControllerInfo;
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performRemove(context, operation, model);
        if (hostControllerInfo != null) {
            final String serverGroup = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
            final ModelNode validate = new ModelNode();
            validate.get(OP).set("validate"); // does not need to exist
            validate.get(OP_ADDR).add(HOST, hostControllerInfo.getLocalHostName());
            context.addStep(validate, new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final Resource host = context.readResource(PathAddress.EMPTY_ADDRESS, false);
                    final Set<Resource.ResourceEntry> entries = host.getChildren(SERVER_CONFIG);
                    final Set<String> foundServer = new HashSet<String>();
                    if (entries != null && ! entries.isEmpty()) {
                        for (final Resource.ResourceEntry entry : entries) {
                            final Resource server = context.readResource(PathAddress.pathAddress(entry.getPathElement()), false);
                            final String group = server.getModel().require(GROUP).asString();
                            if (group.equals(serverGroup)) {
                                foundServer.add(entry.getName());
                            }
                        }
                    }
                    if (!foundServer.isEmpty()) {
                        throw DomainControllerMessages.MESSAGES.cannotRemoveUsedServerGroup(serverGroup, foundServer);
                    }
                    context.stepCompleted();
                }
            }, OperationContext.Stage.VERIFY);
        }
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }
}

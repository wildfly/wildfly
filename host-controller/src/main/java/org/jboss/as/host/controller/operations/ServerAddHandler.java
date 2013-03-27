/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller.operations;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.HostControllerMessages;
import org.jboss.as.host.controller.MasterDomainControllerClient;
import org.jboss.as.host.controller.resources.ServerConfigResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationHandler} adding a new server configuration.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerAddHandler extends AbstractAddStepHandler {

    public static final String OPERATION_NAME = ADD;

    private final LocalHostControllerInfo hostControllerInfo;

    /**
     * Create the ServerAddHandler
     */
    private ServerAddHandler(LocalHostControllerInfo hostControllerInfo) {
        this.hostControllerInfo = hostControllerInfo;
    }

    public static ServerAddHandler create(LocalHostControllerInfo hostControllerInfo) {
        return new ServerAddHandler(hostControllerInfo);
    }

    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {

        final ModelNode model = resource.getModel();
        for (AttributeDefinition attr : ServerConfigResourceDefinition.WRITABLE_ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final PathAddress running = address.subAddress(0, 1).append(PathElement.pathElement(RUNNING_SERVER, address.getLastElement().getValue()));

        //Add the running server
        final ModelNode runningServerAdd = new ModelNode();
        runningServerAdd.get(OP).set(ADD);
        runningServerAdd.get(OP_ADDR).set(running.toModelNode());

        context.addStep(runningServerAdd, new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                context.createResource(PathAddress.EMPTY_ADDRESS);
                context.stepCompleted();
            }
        }, OperationContext.Stage.MODEL, true);

        final String group = model.require(GROUP).asString();
        final String socketBindingGroup = model.hasDefined(SOCKET_BINDING_GROUP) ? model.get(SOCKET_BINDING_GROUP).asString() : null;
        final Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);

        //Check for missing data and pull it down if necessary
        boolean missingData = false;
        if (!context.isBooting() && root.getChild(PathElement.pathElement(SERVER_GROUP, group)) == null) {
            if (hostControllerInfo.isMasterDomainController() || !hostControllerInfo.isRemoteDomainControllerIgnoreUnaffectedConfiguration()) {
                throw HostControllerMessages.MESSAGES.noServerGroupCalled(group);
            } else {
                missingData = true;
            }
        }
        if (socketBindingGroup != null) {
            if (!context.isBooting() && root.getChild(PathElement.pathElement(SOCKET_BINDING_GROUP, socketBindingGroup)) == null) {
                if (hostControllerInfo.isMasterDomainController() || !hostControllerInfo.isRemoteDomainControllerIgnoreUnaffectedConfiguration()) {
                    throw HostControllerMessages.MESSAGES.noSocketBindingGroupCalled(socketBindingGroup);
                } else {
                    missingData = true;
                }
            }
        }

        if (missingData) {
            String serverName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
            pullDownMissingDataFromDc(context, serverName, group, socketBindingGroup);
        }
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        //This will never get called. It is just needed to override the abstract method and is handled by the other populateModel() method
        throw new IllegalArgumentException();
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    private static void pullDownMissingDataFromDc(final OperationContext context, final String serverName, final String serverGroupName, final String socketBindingGroupName) {
        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                //Get the missing data
                final MasterDomainControllerClient masterDomainControllerClient = (MasterDomainControllerClient)context.getServiceRegistry(false).getRequiredService(MasterDomainControllerClient.SERVICE_NAME).getValue();
                masterDomainControllerClient.pullDownDataForUpdatedServerConfigAndApplyToModel(context, serverName, serverGroupName, socketBindingGroupName);
                context.addStep(new OperationStepHandler() {
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        //An extra sanity check, is this necessary?
                        final Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
                        if (!root.hasChild(PathElement.pathElement(SERVER_GROUP, serverGroupName))) {
                            throw HostControllerMessages.MESSAGES.noServerGroupCalled(serverGroupName);
                        }
                        if (socketBindingGroupName != null) {
                            if (!root.hasChild(PathElement.pathElement(SOCKET_BINDING_GROUP, socketBindingGroupName))) {
                                throw HostControllerMessages.MESSAGES.noSocketBindingGroupCalled(socketBindingGroupName);
                            }
                        }
                        context.stepCompleted();
                    }
                }, Stage.MODEL);
                context.stepCompleted();
            }
        }, Stage.MODEL);
    }
}

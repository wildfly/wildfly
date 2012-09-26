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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.RollbackHandler;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the socket-binding-group resource's remove operation.
 * If the socket binding group has running servers this operation will fail
 *
 * @author Kabir Khan
 */
public class DomainSocketBindingGroupRemoveHandler extends AbstractRemoveStepHandler {

    public static final String OPERATION_NAME = REMOVE;

    public static final DomainSocketBindingGroupRemoveHandler INSTANCE = new DomainSocketBindingGroupRemoveHandler();
    private volatile ServerInventory serverInventory;

    /**
     * Create the AbstractSocketBindingRemoveHandler
     */
    protected DomainSocketBindingGroupRemoveHandler() {
    }


    public void initializeServerInventory(ServerInventory serverInventory) {
        //This is a Domain level operation, however the server inventory is not available until the host model has been created
        this.serverInventory = serverInventory;
    }

    protected boolean requiresRuntime(OperationContext context) {
        return context.getProcessType().isServer();
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        //Check that the socket binding group can be removed. This is not possible if there are running servers
        if (!context.getProcessType().isServer()) {
            //We are the host controller

            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    String socketBindingGroupName = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();

                    //Find all the server groups using the socket binding group
                    Set<String> serverGroups = new HashSet<String>();
                    for (ResourceEntry entry : context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS).getChildren(SERVER_GROUP)) {
                        if (entry.getModel().get(SOCKET_BINDING_GROUP).asString().equals(socketBindingGroupName)) {
                            serverGroups.add(entry.getName());
                        }
                    }

                    //Find all the server configs using the socket binding group
                    Set<String> runningServers = new HashSet<String>();
                    for (ResourceEntry entry : context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS).getChildren(HOST).iterator().next().getChildren(SERVER_CONFIG)) {
                        ModelNode configModel = entry.getModel();
                        if (configModel.hasDefined(SOCKET_BINDING_GROUP)) {
                            if (configModel.get(SOCKET_BINDING_GROUP).asString().equals(socketBindingGroupName)) {
                                if (isRunningServer(entry.getName())) {
                                    runningServers.add(entry.getName());
                                }
                            }
                        } else {
                            if (serverGroups.contains(configModel.get(GROUP).asString())) {
                                if (isRunningServer(entry.getName())) {
                                    runningServers.add(entry.getName());
                                }
                            }
                        }
                    }

                    if (!runningServers.isEmpty()) {
                        throw new OperationFailedException("Could not remove socket-binding-group since the following servers are running: " + runningServers);
                    }
                    context.stepCompleted();
                }
            }, Stage.MODEL);
        }

        super.performRemove(context, operation, model);
    }

    private boolean isRunningServer(String serverName) {
        ServerStatus status = serverInventory.determineServerStatus(serverName);
        switch (status) {
            case STARTED:
            case STARTING:
            case STOPPING:
                return true;
            default:
                return false;
        }
    }

}

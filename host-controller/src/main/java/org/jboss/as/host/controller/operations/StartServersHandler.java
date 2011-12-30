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


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.util.Locale;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.HostRunningModeControl;
import org.jboss.as.host.controller.RestartMode;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.process.ProcessInfo;
import org.jboss.dmr.ModelNode;

/**
 * Starts or reconnect all auto-start servers (at boot).
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class StartServersHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "start-servers";

    private final ServerInventory serverInventory;
    private final HostControllerEnvironment hostControllerEnvironment;
    private final HostRunningModeControl runningModeControl;

    /**
     * Create the ServerAddHandler
     */
    public StartServersHandler(final HostControllerEnvironment hostControllerEnvironment, final ServerInventory serverInventory, HostRunningModeControl runningModeControl) {
        this.hostControllerEnvironment = hostControllerEnvironment;
        this.serverInventory = serverInventory;
        this.runningModeControl = runningModeControl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (!context.isBooting()) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.invocationNotAllowedAfterBoot(operation.require(OP))));
        }

        if (context.getRunningMode() == RunningMode.ADMIN_ONLY) {
            throw new OperationFailedException(new ModelNode(MESSAGES.cannotStartServersInvalidMode(context.getRunningMode())));
        }


        final ModelNode domainModel = Resource.Tools.readModel(context.getRootResource());
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // start servers
                final Resource resource =  context.readResource(PathAddress.EMPTY_ADDRESS);
                final ModelNode hostModel = Resource.Tools.readModel(resource);
                if(hostModel.hasDefined(SERVER_CONFIG)) {
                    final ModelNode servers = hostModel.get(SERVER_CONFIG).clone();
                    if (hostControllerEnvironment.isRestart() || runningModeControl.getRestartMode() == RestartMode.HC_ONLY){
                        restartedHcStartOrReconnectServers(servers, domainModel);
                    } else {
                        cleanStartServers(servers, domainModel);
                    }
                }
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        // private operation does not need description
        return new ModelNode();
    }

    private void cleanStartServers(final ModelNode servers, final ModelNode domainModel){
        for(final String serverName : servers.keys()) {
            if(servers.get(serverName, AUTO_START).asBoolean(true)) {
                try {
                    serverInventory.startServer(serverName, domainModel);
                } catch (Exception e) {
                    ROOT_LOGGER.failedToStartServer(e, serverName);
                }
            }
        }
    }

    private void restartedHcStartOrReconnectServers(final ModelNode servers, final ModelNode domainModel){
        Map<String, ProcessInfo> processInfos = serverInventory.determineRunningProcesses();
        for(final String serverName : servers.keys()) {
            ProcessInfo info = processInfos.get(serverInventory.getServerProcessName(serverName));
            boolean auto = servers.get(serverName, AUTO_START).asBoolean(true);
            if (info == null && auto) {
                try {
                    serverInventory.startServer(serverName, domainModel);
                } catch (Exception e) {
                    ROOT_LOGGER.failedToStartServer(e, serverName);
                }
            } else if (info != null){
                //Reconnect the server
                serverInventory.reconnectServer(serverName, domainModel, info.isRunning());
            }
        }
    }
}

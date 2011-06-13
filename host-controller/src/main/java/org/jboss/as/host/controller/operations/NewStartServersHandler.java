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

import java.util.Locale;
import java.util.Map;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.NewServerInventory;
import org.jboss.as.process.ProcessInfo;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Starts or reconnect all auto-start servers (at boot).
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NewStartServersHandler implements NewStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "start-servers";

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");

    private final NewServerInventory serverInventory;
    private final HostControllerEnvironment hostControllerEnvironment;

    /**
     * Create the ServerAddHandler
     */
    public NewStartServersHandler(final HostControllerEnvironment hostControllerEnvironment, final NewServerInventory serverInventory) {
        this.hostControllerEnvironment = hostControllerEnvironment;
        this.serverInventory = serverInventory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

        if (!context.isBooting()) {
            throw new OperationFailedException(new ModelNode().set(String.format("Cannot invoke %s after host boot", operation.require(OP))));
        }

        context.addStep(new NewStepHandler() {
            @Override
            public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                // start servers
                final ModelNode domainModel = context.getModel();
                final ModelNode hostModel = context.readModel(PathAddress.EMPTY_ADDRESS);
                if(hostModel.hasDefined(SERVER_CONFIG)) {
                    final ModelNode servers = hostModel.get(SERVER_CONFIG).clone();
                    if (hostControllerEnvironment.isRestart()){
                        restartedHcStartOrReconnectServers(servers, domainModel);
                    } else {
                        cleanStartServers(servers, domainModel);
                    }
                }
                context.completeStep();
            }
        }, NewOperationContext.Stage.RUNTIME);
        context.completeStep();
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
                    System.out.println("Start server " + serverName + " " + serverInventory);
                    serverInventory.startServer(serverName, domainModel);
                } catch (Exception e) {
                    log.errorf(e, "Failed to start server (%s)", serverName);
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
                    log.errorf(e, "Failed to start server (%s)", serverName);
                }
            } else if (info != null){
                //Reconnect the server
                serverInventory.reconnectServer(serverName, domainModel, info.isRunning());
            }
        }
    }
}

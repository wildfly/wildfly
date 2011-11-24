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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.operations.HackDomainServerLifecycleHandlers;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.process.ProcessInfo;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Does the real work on the ServerInventory for {@link HackDomainServerLifecycleHandlers} to stop/start/restart the
 * servers.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainServerLifecycleHandlerDelegates {

    private static final int TIMEOUT = 10000;

    public static void initializeDelegates(final ServerInventory serverInventory) {
        HackDomainServerLifecycleHandlers.initialize(new StopServersDelegate(serverInventory), new StartServersDelegate(serverInventory), new RestartServersDelegate(serverInventory));
    }

    private abstract static class LifecycleHandlerDelegate implements OperationStepHandler {
        final ServerInventory serverInventory;
        LifecycleHandlerDelegate(ServerInventory serverInventory) {
            this.serverInventory = serverInventory;
        }

        String getServerGroupName(final ModelNode operation) {
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            if (address.size() == 0) {
                return null;
            }
            return address.getLastElement().getValue();
        }

        Set<String> getServersForGroup(final ModelNode model, final String groupName){
            if (groupName == null) {
                return Collections.emptySet();
            }
            final String hostName = model.get(HOST).keys().iterator().next();
            final ModelNode serverConfig = model.get(HOST, hostName).get(SERVER_CONFIG);
            final Set<String> servers = new HashSet<String>();
            for (Property config : serverConfig.asPropertyList()) {
                if (groupName.equals(config.getValue().get(GROUP).asString())) {
                    servers.add(config.getName());
                }
            }
            return servers;
        }
    }


    private static class StopServersDelegate extends LifecycleHandlerDelegate {
        StopServersDelegate(ServerInventory serverInventory) {
            super(serverInventory);
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final String group = getServerGroupName(operation);
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    if (group != null) {
                        final ModelNode model = Resource.Tools.readModel(context.getRootResource());
                        for (String server : getServersForGroup(model, group)) {
                            serverInventory.stopServer(server, TIMEOUT);
                        }
                    } else {
                        serverInventory.stopServers(TIMEOUT);
                    }
                    context.completeStep();
                }
            }, Stage.RUNTIME);
            context.completeStep();
        }
    }

    private static class StartServersDelegate extends LifecycleHandlerDelegate {
        StartServersDelegate(final ServerInventory serverInventory) {
            super(serverInventory);
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final ModelNode model = Resource.Tools.readModel(context.getRootResource());
            final String group = getServerGroupName(operation);
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final String hostName = model.get(HOST).keys().iterator().next();
                    final ModelNode serverConfig = model.get(HOST, hostName).get(SERVER_CONFIG);
                    final Set<String> serversInGroup = getServersForGroup(model, group);
                    for (Property config : serverConfig.asPropertyList()) {
                        final ServerStatus status = serverInventory.determineServerStatus(config.getName());
                        if (status != ServerStatus.STARTING && status != ServerStatus.STARTED) {
                            if (group == null || serversInGroup.contains(config.getName())) {
                                if (status != ServerStatus.STOPPED) {
                                    serverInventory.stopServer(config.getName(), TIMEOUT);
                                }
                                serverInventory.startServer(config.getName(), model);
                            }
                        }
                    }
                    context.completeStep();
                }
            }, Stage.RUNTIME);

            context.completeStep();
        }
    }

    private static class RestartServersDelegate extends LifecycleHandlerDelegate {
        RestartServersDelegate(final ServerInventory serverInventory) {
            super(serverInventory);
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final ModelNode model = Resource.Tools.readModel(context.getRootResource());
                    final String group = getServerGroupName(operation);
                    context.addStep(new OperationStepHandler() {
                        @Override
                        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                            Map<String, ProcessInfo> processes = serverInventory.determineRunningProcesses(true);
                            final Set<String> serversInGroup = getServersForGroup(model, group);
                            for (String serverName : processes.keySet()) {
                                final String serverModelName = serverInventory.getProcessServerName(serverName);
                                if (group == null || serversInGroup.contains(serverModelName)) {
                                    serverInventory.restartServer(serverModelName, TIMEOUT, model);
                                }
                            }
                            context.completeStep();
                        }
                    }, Stage.RUNTIME);
                    context.completeStep();
                }
            }, Stage.RUNTIME);
            context.completeStep();
        }
    }
}

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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP_SERVERS;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.descriptions.DomainResolver;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.process.ProcessInfo;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * An operation handler for the :stop-servers, :restart-servers and :start-servers commands. This belongs in the
 * domain model but needs access to the server inventory which is initialized when setting up the host model.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainServerLifecycleHandlers {

    public static final String RESTART_SERVERS_NAME = RESTART_SERVERS;
    public static final String START_SERVERS_NAME = START_SERVERS;
    public static final String STOP_SERVERS_NAME = STOP_SERVERS;

    private static final int TIMEOUT = 10000;

    public static void initializeServerInventory(ServerInventory serverInventory) {
        StopServersLifecycleHandler.INSTANCE.setServerInventory(serverInventory);
        StartServersLifecycleHandler.INSTANCE.setServerInventory(serverInventory);
        RestartServersLifecycleHandler.INSTANCE.setServerInventory(serverInventory);
    }

    public static void registerDomainHandlers(ManagementResourceRegistration registration) {
        registerHandlers(registration, false);
    }

    public static void registerServerGroupHandlers(ManagementResourceRegistration registration) {
        registerHandlers(registration, true);
    }

    private static void registerHandlers(ManagementResourceRegistration registration, boolean serverGroup) {
        registration.registerOperationHandler(getOperationDefinition(serverGroup, StopServersLifecycleHandler.OPERATION_NAME), StopServersLifecycleHandler.INSTANCE);
        registration.registerOperationHandler(getOperationDefinition(serverGroup, StartServersLifecycleHandler.OPERATION_NAME), StartServersLifecycleHandler.INSTANCE);
        registration.registerOperationHandler(getOperationDefinition(serverGroup, RestartServersLifecycleHandler.OPERATION_NAME), RestartServersLifecycleHandler.INSTANCE);
    }

    private static OperationDefinition getOperationDefinition(boolean serverGroup, String operationName) {
        return new SimpleOperationDefinition(operationName, DomainResolver.getResolver(serverGroup ? ModelDescriptionConstants.SERVER_GROUP : ModelDescriptionConstants.DOMAIN));
    }

    private abstract static class AbstractHackLifecycleHandler implements OperationStepHandler {
        volatile ServerInventory serverInventory;

        protected AbstractHackLifecycleHandler() {
        }

        /**
         * To be called when setting up the host model
         */
        void setServerInventory(ServerInventory serverInventory) {
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
            if(! serverConfig.isDefined()) {
                return Collections.emptySet();
            }
            final Set<String> servers = new HashSet<String>();
            for (Property config : serverConfig.asPropertyList()) {
                if (groupName.equals(config.getValue().get(GROUP).asString())) {
                    servers.add(config.getName());
                }
            }
            return servers;
        }

    }

    private static class StopServersLifecycleHandler extends AbstractHackLifecycleHandler {
        static final String OPERATION_NAME = STOP_SERVERS_NAME;
        static final StopServersLifecycleHandler INSTANCE = new StopServersLifecycleHandler();

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final String group = getServerGroupName(operation);
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    if (group != null) {
                        final ModelNode model = Resource.Tools.readModel(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, true));
                        for (String server : getServersForGroup(model, group)) {
                            serverInventory.stopServer(server, TIMEOUT);
                        }
                    } else {
                        serverInventory.stopServers(TIMEOUT);
                    }
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            }, Stage.RUNTIME);

            context.stepCompleted();
        }
    }

    private static class StartServersLifecycleHandler extends AbstractHackLifecycleHandler {
        static final String OPERATION_NAME = START_SERVERS_NAME;
        static final StartServersLifecycleHandler INSTANCE = new StartServersLifecycleHandler();

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final ModelNode model = Resource.Tools.readModel(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, true));
            final String group = getServerGroupName(operation);
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final String hostName = model.get(HOST).keys().iterator().next();
                    final ModelNode serverConfig = model.get(HOST, hostName).get(SERVER_CONFIG);
                    final Set<String> serversInGroup = getServersForGroup(model, group);
                    if(serverConfig.isDefined()) {
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
                    }
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            }, Stage.RUNTIME);

            context.stepCompleted();
        }
    }

    private static class RestartServersLifecycleHandler extends AbstractHackLifecycleHandler {
        static final String OPERATION_NAME = RESTART_SERVERS_NAME;
        static final RestartServersLifecycleHandler INSTANCE = new RestartServersLifecycleHandler();


        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final ModelNode model = Resource.Tools.readModel(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, true));
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
                            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                        }
                    }, Stage.RUNTIME);
                    context.stepCompleted();
                }
            }, Stage.RUNTIME);
            context.stepCompleted();
        }
    }
}

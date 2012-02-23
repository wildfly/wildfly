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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.descriptions.DomainRootDescription;
import org.jboss.as.domain.controller.descriptions.ServerGroupDescription;
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
        StopServersLifecycleHandler.DOMAIN_INSTANCE.setServerInventory(serverInventory);
        StartServersLifecycleHandler.DOMAIN_INSTANCE.setServerInventory(serverInventory);
        RestartServersLifecycleHandler.DOMAIN_INSTANCE.setServerInventory(serverInventory);
        StopServersLifecycleHandler.SERVER_GROUP_INSTANCE.setServerInventory(serverInventory);
        StartServersLifecycleHandler.SERVER_GROUP_INSTANCE.setServerInventory(serverInventory);
        RestartServersLifecycleHandler.SERVER_GROUP_INSTANCE.setServerInventory(serverInventory);
    }

    public static void registerDomainHandlers(ManagementResourceRegistration registration) {
        registration.registerOperationHandler(StopServersLifecycleHandler.OPERATION_NAME, StopServersLifecycleHandler.DOMAIN_INSTANCE, StopServersLifecycleHandler.DOMAIN_INSTANCE);
        registration.registerOperationHandler(StartServersLifecycleHandler.OPERATION_NAME, StartServersLifecycleHandler.DOMAIN_INSTANCE, StartServersLifecycleHandler.DOMAIN_INSTANCE);
        registration.registerOperationHandler(RestartServersLifecycleHandler.OPERATION_NAME, RestartServersLifecycleHandler.DOMAIN_INSTANCE, RestartServersLifecycleHandler.DOMAIN_INSTANCE);
    }

    public static void registerServerGroupHandlers(ManagementResourceRegistration registration) {
        registration.registerOperationHandler(StopServersLifecycleHandler.OPERATION_NAME, StopServersLifecycleHandler.SERVER_GROUP_INSTANCE, StopServersLifecycleHandler.SERVER_GROUP_INSTANCE);
        registration.registerOperationHandler(StartServersLifecycleHandler.OPERATION_NAME, StartServersLifecycleHandler.SERVER_GROUP_INSTANCE, StartServersLifecycleHandler.SERVER_GROUP_INSTANCE);
        registration.registerOperationHandler(RestartServersLifecycleHandler.OPERATION_NAME, RestartServersLifecycleHandler.SERVER_GROUP_INSTANCE, RestartServersLifecycleHandler.SERVER_GROUP_INSTANCE);
    }

    private abstract static class AbstractHackLifecycleHandler implements OperationStepHandler, DescriptionProvider {
        volatile ServerInventory serverInventory;
        final boolean domain;

        protected AbstractHackLifecycleHandler(final boolean domain) {
            this.domain = domain;
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
        static final StopServersLifecycleHandler DOMAIN_INSTANCE = new StopServersLifecycleHandler(true);
        static final StopServersLifecycleHandler SERVER_GROUP_INSTANCE = new StopServersLifecycleHandler(false);

        public StopServersLifecycleHandler(final boolean domain) {
            super(domain);
        }

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
                    context.completeStep();
                }
            }, Stage.RUNTIME);
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            if (domain) {
                return DomainRootDescription.getStopServersOperation(locale);
            } else {
                return ServerGroupDescription.getStopServersOperation(locale);
            }
        }
    }

    private static class StartServersLifecycleHandler extends AbstractHackLifecycleHandler {
        static final String OPERATION_NAME = START_SERVERS_NAME;
        static final StartServersLifecycleHandler DOMAIN_INSTANCE = new StartServersLifecycleHandler(true);
        static final StartServersLifecycleHandler SERVER_GROUP_INSTANCE = new StartServersLifecycleHandler(false);

        public StartServersLifecycleHandler(final boolean domain) {
            super(domain);
        }

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
                    context.completeStep();
                }
            }, Stage.RUNTIME);

            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            if (domain) {
                return DomainRootDescription.getStartServersOperation(locale);
            } else {
                return ServerGroupDescription.getStartServersOperation(locale);
            }
        }
    }

    private static class RestartServersLifecycleHandler extends AbstractHackLifecycleHandler {
        static final String OPERATION_NAME = RESTART_SERVERS_NAME;
        static final RestartServersLifecycleHandler DOMAIN_INSTANCE = new RestartServersLifecycleHandler(true);
        static final RestartServersLifecycleHandler SERVER_GROUP_INSTANCE = new RestartServersLifecycleHandler(false);

        public RestartServersLifecycleHandler(final boolean domain) {
            super(domain);
        }


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
                            context.completeStep();
                        }
                    }, Stage.RUNTIME);
                    context.completeStep();
                }
            }, Stage.RUNTIME);
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            if (domain) {
                return DomainRootDescription.getRestartServersOperation(locale);
            } else {
                return ServerGroupDescription.getRestartServersOperation(locale);
            }
        }
    }
}

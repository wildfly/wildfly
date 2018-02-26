/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.clustering;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Implementation of {@link ServerSetupTask} which runs provided CLI commands. Since the API is based around class instances, the extending
 * class implementations need to configure the {@link CLIServerSetupTask#builder}.
 *
 * @author Radoslav Husar
 */
public class CLIServerSetupTask implements ServerSetupTask {

    private static final Logger LOG = Logger.getLogger(CLIServerSetupTask.class);
    protected final Builder builder = new Builder();

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        NodeBuilder node = builder.configuration.get(containerId);
        if (node != null && !node.setupCommands.isEmpty()) {
            this.executeCommands(managementClient, node.setupCommands, node.batch, node.reloadOnSetup);
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        NodeBuilder node = builder.configuration.get(containerId);
        if (node != null && !node.teardownCommands.isEmpty()) {
            this.executeCommands(managementClient, node.teardownCommands, node.batch, node.reloadOnTearDown);
        }
    }

    private void executeCommands(ManagementClient managementClient, List<String> commands, boolean useBatch, boolean reload) throws Exception {
        if (commands.isEmpty()) return;

        CommandContext context = CLITestUtil.getCommandContext();
        context.connectController();

        if (useBatch) {
            context.getBatchManager().activateNewBatch();
            Batch batch = context.getBatchManager().getActiveBatch();
            for (String command : commands) {
                batch.add(context.toBatchedCommand(command));
            }
            ModelNode commandModel = batch.toRequest();

            // Add {allow-resource-service-restart=true} operation flag manually since its not exposed on the Batch
            // Currently fails with: https://issues.jboss.org/browse/ARQ-1135
            //commandModel.get(ModelDescriptionConstants.OPERATION_HEADERS).get(ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART).set(true);

            ModelNode result = managementClient.getControllerClient().execute(commandModel);

            LOG.infof("Executed batch %s with result %s", commands, result.toJSONString(true));
            this.checkResult(result);
        } else {
            for (String command : commands) {
                ModelNode commandModel = context.buildRequest(command);
                ModelNode result = managementClient.getControllerClient().execute(commandModel);

                LOG.infof("Executed single command %s with result %s", commands, result.toJSONString(true));
                this.checkResult(result);
            }
        }

        if (reload) {
            LOG.infof("Reloading server %s if its in a 'reload-required' state.", managementClient.getMgmtAddress());
            ServerReload.reloadIfRequired(managementClient);
        }
    }

    private void checkResult(ModelNode result) {
        if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            throw new RuntimeException("CLIServerSetupTask failed! Failed with: " + failureDesc);
        }
    }

    public static class Builder {
        private final Map<String, NodeBuilder> configuration = new HashMap<>();

        /**
         * Configure commands to be run for a specific node. Multiple calls to this method can be made to add more commands.
         *
         * @param node node to run setup and teardown commands on
         */
        public NodeBuilder node(String node) {
            if (configuration.containsKey(node)) {
                return configuration.get(node);
            }

            NodeBuilder nodeBuilder = new NodeBuilder(this);
            configuration.put(node, nodeBuilder);
            return nodeBuilder;
        }

        /**
         * Configure commands to be run for a group of nodes. Cannot be called multiple times on nodes that already have some configuration
         * defined. Use {@link Builder#node(java.lang.String)} for node-specific overrides.
         *
         * @param nodes nodes to run setup and teardown commands on
         */
        public NodeBuilder node(String... nodes) {
            if (Stream.of(nodes).anyMatch(configuration::containsKey)) {
                throw new IllegalStateException("Node-specific overrides need to be done for per node.");
            }

            NodeBuilder nodeBuilder = new NodeBuilder(this);
            Stream.of(nodes).forEach(n -> this.configuration.put(n, nodeBuilder));
            return nodeBuilder;
        }
    }

    public static class NodeBuilder {
        private final Builder parentBuilder;
        private final List<String> setupCommands = new LinkedList<>();
        private final List<String> teardownCommands = new LinkedList<>();
        private boolean batch = true;
        private boolean reloadOnSetup = true;
        private boolean reloadOnTearDown = true;

        NodeBuilder(Builder parentBuilder) {
            this.parentBuilder = parentBuilder;
        }

        /**
         * Adds a single command to be run in setup phase.
         */
        public NodeBuilder setup(String setupCommand) {
            setupCommands.add(setupCommand);
            return this;
        }

        /**
         * Adds a formatted single command to be run in setup phase.
         */
        public NodeBuilder setup(String formatSetupCommand, Object... formatArguments) {
            return setup(String.format(formatSetupCommand, formatArguments));
        }

        /**
         * Adds a single command to be run in teardown phase.
         */
        public NodeBuilder teardown(String teardownCommand) {
            teardownCommands.add(teardownCommand);
            return this;
        }

        /**
         * Adds a single formatted command to be run in teardown phase.
         */
        public NodeBuilder teardown(String formatTeardownCommand, Object... formatArguments) {
            return teardown(String.format(formatTeardownCommand, formatArguments));
        }

        /**
         * Configure whether the commands should be run in a batch. Defaults configuration is to batch.
         */
        public NodeBuilder batch(boolean batch) {
            this.batch = batch;
            return this;
        }

        /**
         * Configure whether to reload the server after setup if it's in a 'reload-required' state.
         */
        public NodeBuilder reloadOnSetup(boolean reloadOnSetup) {
            this.reloadOnSetup = reloadOnSetup;
            return this;
        }

        /**
         * Configure whether to reload the server on teardown if it's in a 'reload-required' state.
         */
        public NodeBuilder reloadOnTearDown(boolean reloadOnTearDown) {
            this.reloadOnTearDown = reloadOnTearDown;
            return this;
        }

        /**
         * Returns a reference to the parent builder to be used for chaining.
         */
        public Builder parent() {
            return parentBuilder;
        }
    }
}
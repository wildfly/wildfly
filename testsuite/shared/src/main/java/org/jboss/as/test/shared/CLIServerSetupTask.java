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
package org.jboss.as.test.shared;

import static org.jboss.as.test.shared.ManagementServerSetupTask.createContainerConfigurationBuilder;
import static org.jboss.as.test.shared.ManagementServerSetupTask.createScriptBuilder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ManagementServerSetupTask.CommandSet;
import org.jboss.as.test.shared.ManagementServerSetupTask.ContainerConfiguration;
import org.jboss.as.test.shared.ManagementServerSetupTask.ContainerConfigurationBuilder;
import org.jboss.as.test.shared.ManagementServerSetupTask.ScriptBuilder;

/**
 * Implementation of {@link ServerSetupTask} which runs provided CLI commands. Since the API is based around class instances, the extending
 * class implementations need to configure the {@link CLIServerSetupTask#builder}.
 *
 * @author Radoslav Husar
 * @deprecated Use {@link ManagementServerSetupTask} instead
 */
@Deprecated
public class CLIServerSetupTask implements ServerSetupTask {

    protected final Builder builder = new Builder();

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        new ManagementServerSetupTask(containerId, this.createContainerConfiguration(containerId, ContainerConfigurationBuilder::setupScript, NodeBuilder::getSetupCommands)).setup(managementClient, containerId);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        new ManagementServerSetupTask(containerId, this.createContainerConfiguration(containerId, ContainerConfigurationBuilder::tearDownScript, NodeBuilder::getTearDownCommands)).tearDown(managementClient, containerId);
    }

    private ContainerConfiguration createContainerConfiguration(String containerId, BiFunction<ContainerConfigurationBuilder, List<List<String>>, ContainerConfigurationBuilder> scriptFunction, Function<NodeBuilder, List<String>> batch) {
        ContainerConfigurationBuilder containerBuilder = createContainerConfigurationBuilder();
        NodeBuilder builder = this.builder.configuration.get(containerId);
        if (builder != null) {
            ScriptBuilder scriptBuilder = createScriptBuilder();
            CommandSet<?> commands = (builder.isBatched()) ? scriptBuilder.startBatch() : scriptBuilder;
            for (String command : batch.apply(builder)) {
                commands.add(command);
            }
            scriptFunction.apply(containerBuilder, scriptBuilder.build());
        }
        return containerBuilder.build();
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
         * defined. Use {@link Builder#node(String)} for node-specific overrides.
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

        NodeBuilder(Builder parentBuilder) {
            this.parentBuilder = parentBuilder;
        }

        List<String> getSetupCommands() {
            return this.setupCommands;
        }

        List<String> getTearDownCommands() {
            return this.teardownCommands;
        }

        boolean isBatched() {
            return this.batch;
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
         * Configure whether the commands should be run in a batch. Default configuration is to batch.
         */
        public NodeBuilder batch(boolean batch) {
            this.batch = batch;
            return this;
        }

        /**
         * Configure whether to reload the server after setup if it's in a 'reload-required' state.
         */
        public NodeBuilder reloadOnSetup(boolean reloadOnSetup) {
            return this;
        }

        /**
         * Configure whether to reload the server on teardown if it's in a 'reload-required' state.
         */
        public NodeBuilder reloadOnTearDown(boolean reloadOnTearDown) {
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
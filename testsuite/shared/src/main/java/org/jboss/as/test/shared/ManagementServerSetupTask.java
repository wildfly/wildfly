/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.dmr.ModelNode;

/**
 * {@link ServerSetupTask} that runs a list of batchable management operations on a set of containers.
 * @author Paul Ferraro
 */
public class ManagementServerSetupTask implements ServerSetupTask {

    public interface ContainerSetConfiguration {
        /**
         * Returns the configuration for the specified container, or the default configuration.
         * @param container a container identifier
         * @return a container configuration
         */
        ContainerConfiguration getContainerConfiguration(String container);
    }

    public interface ContainerConfiguration {
        /**
         * Returns the setup script for a container.
         * @return a list of CLI command batches
         */
        List<List<String>> getSetupScript();

        /**
         * Returns the tear-down script for a container.
         * @return a list of CLI command batches
         */
        List<List<String>> getTearDownScript();
    }

    public interface Builder<C> {
        /**
         * Builds this configuration.
         * @return the built configuration.
         */
        C build();
    }

    public interface ContainerSetConfigurationBuilder extends Builder<ContainerSetConfiguration> {
        /**
         * Adds a set of contains with the same configuration.
         * @param containers a set of container identifiers
         * @param configuration a container configuration
         * @return a reference to this builder
         */
        ContainerSetConfigurationBuilder addContainers(Set<String> containers, ContainerConfiguration configuration);

        /**
         * Adds a container configuration.
         * @param container a container identifier
         * @param configuration a container configuration
         * @return a reference to this builder
         */
        default ContainerSetConfigurationBuilder addContainer(String container, ContainerConfiguration configuration) {
            return this.addContainers(Set.of(container), configuration);
        }

        /**
         * Adds a container configuration.
         * @param container a container identifier
         * @param configuration a container configuration
         * @return a reference to this builder
         */
        ContainerSetConfigurationBuilder defaultContainer(ContainerConfiguration configuration);
    }

    public interface ContainerConfigurationBuilder extends Builder<ContainerConfiguration> {
        /**
         * Defines a script to run during test setup.
         * @param script a list of CLI batches
         * @return a reference to this builder
         */
        ContainerConfigurationBuilder setupScript(List<List<String>> script);

        /**
         * Defines a script to run during test tear-down.
         * @param script a list of CLI batches
         * @return a reference to this builder
         */
        ContainerConfigurationBuilder tearDownScript(List<List<String>> script);
    }

    public interface CommandSet<B> {
        /**
         * Adds the specified command to the associated set.
         * @param command a CLI command
         * @return a reference to this builder
         */
        B add(String command);

        /**
         * Adds the specified parameterized command to the associated set.
         * @param command a CLI command
         * @return a reference to this builder
         */
        default B add(String pattern, Object... params) {
            return this.add(String.format(Locale.ROOT, pattern, params));
        }
    }

    public interface ScriptBuilder extends Builder<List<List<String>>>, CommandSet<ScriptBuilder> {
        /**
         * Starts a batch of operations.
         * @return a reference to the batch builder.
         */
        BatchBuilder startBatch();
    }

    public interface BatchBuilder extends CommandSet<BatchBuilder> {
        /**
         * Ends a batch of operations.
         * @return a reference to the script builder.
         */
        ScriptBuilder endBatch();
    }

    /**
     * Create a new builder for a set of container configurations.
     * @return a new builder
     */
    public static ContainerSetConfigurationBuilder createContainerSetConfigurationBuilder() {
        return new ContainerSetConfigurationBuilder() {
            private final Map<String, ContainerConfiguration> containers = new HashMap<>();
            private ContainerConfiguration defaultContainer = null;

            @Override
            public ContainerSetConfigurationBuilder addContainers(Set<String> containers, ContainerConfiguration configuration) {
                for (String container : containers) {
                    this.containers.put(container, configuration);
                }
                return this;
            }

            @Override
            public ContainerSetConfigurationBuilder defaultContainer(ContainerConfiguration configuration) {
                this.defaultContainer = configuration;
                return this;
            }

            @Override
            public ContainerSetConfiguration build() {
                Map<String, ContainerConfiguration> containers = this.containers;
                ContainerConfiguration defaultConfig = this.defaultContainer;
                return new ContainerSetConfiguration() {
                    @Override
                    public ContainerConfiguration getContainerConfiguration(String container) {
                        return containers.getOrDefault(container, defaultConfig);
                    }
                };
            }
        };
    }

    /**
     * Create a new builder for a container configuration.
     * @return a new builder
     */
    public static ContainerConfigurationBuilder createContainerConfigurationBuilder() {
        return new ContainerConfigurationBuilder() {
            private List<List<String>> setupScript = List.of();
            private List<List<String>> tearDownScript = List.of();

            @Override
            public ContainerConfigurationBuilder setupScript(List<List<String>> batches) {
                this.setupScript = batches;
                return this;
            }

            @Override
            public ContainerConfigurationBuilder tearDownScript(List<List<String>> batches) {
                this.tearDownScript = batches;
                return this;
            }

            @Override
            public ContainerConfiguration build() {
                List<List<String>> setupScript = Collections.unmodifiableList(this.setupScript);
                List<List<String>> tearDownScript = Collections.unmodifiableList(this.tearDownScript);
                return new ContainerConfiguration() {
                    @Override
                    public List<List<String>> getSetupScript() {
                        return setupScript;
                    }

                    @Override
                    public List<List<String>> getTearDownScript() {
                        return tearDownScript;
                    }
                };
            }
        };
    }

    /**
     * Create a new builder for a script.
     * @return a new builder
     */
    public static ScriptBuilder createScriptBuilder() {
        return new ScriptBuilder() {
            private List<List<String>> batches = new LinkedList<>();

            @Override
            public List<List<String>> build() {
                return Collections.unmodifiableList(this.batches);
            }

            @Override
            public ScriptBuilder add(String command) {
                this.batches.add(List.of(command));
                return this;
            }

            @Override
            public BatchBuilder startBatch() {
                ScriptBuilder builder = this;
                List<String> batch = new LinkedList<>();
                this.batches.add(batch);
                return new BatchBuilder() {
                    @Override
                    public BatchBuilder add(String command) {
                        batch.add(command);
                        return this;
                    }

                    @Override
                    public ScriptBuilder endBatch() {
                        return builder;
                    }
                };
            }
        };
    }

    private final ContainerSetConfiguration config;

    public ManagementServerSetupTask(ContainerSetConfiguration config) {
        this.config = config;
    }

    public ManagementServerSetupTask(ContainerConfiguration defaultConfig) {
        this(createContainerSetConfigurationBuilder().defaultContainer(defaultConfig).build());
    }

    public ManagementServerSetupTask(String container, ContainerConfiguration config) {
        this(createContainerSetConfigurationBuilder().addContainer(container, config).build());
    }

    public ManagementServerSetupTask(Set<String> containers, ContainerConfiguration config) {
        this(createContainerSetConfigurationBuilder().addContainers(containers, config).build());
    }

    @Override
    public void setup(ManagementClient client, String containerId) throws Exception {
        this.configure(client, containerId, ContainerConfiguration::getSetupScript);
    }

    @Override
    public void tearDown(ManagementClient client, String containerId) throws Exception {
        this.configure(client, containerId, ContainerConfiguration::getTearDownScript);
    }

    private void configure(ManagementClient client, String containerId, Function<ContainerConfiguration, List<List<String>>> script) throws Exception {
        ContainerConfiguration config = this.config.getContainerConfiguration(containerId);
        if (config != null) {
            List<List<String>> batches = script.apply(config);
            if (!batches.isEmpty()) {
                CommandContext context = CLITestUtil.getCommandContext();
                context.connectController();

                for (List<String> batch : batches) {
                    if (!batch.isEmpty()) {
                        ModelNode model = createCommandModel(context, batch);
                        ModelNode result = client.getControllerClient().execute(model);

                        if (result.get(ClientConstants.OUTCOME).asString().equals(ClientConstants.FAILED)) {
                            throw new RuntimeException(model.toJSONString(true) + ": " + result.get(ClientConstants.FAILURE_DESCRIPTION).toString());
                        }
                    }
                }

                ServerReload.reloadIfRequired(client);
            }
        }
    }

    private static ModelNode createCommandModel(CommandContext context, List<String> commands) throws CommandFormatException {
        if (commands.size() == 1) {
            // Single commands do not need a batch
            return context.buildRequest(commands.get(0));
        }
        BatchManager manager = context.getBatchManager();
        manager.activateNewBatch();
        Batch batch = manager.getActiveBatch();
        for (String command : commands) {
            batch.add(context.toBatchedCommand(command));
        }
        return batch.toRequest();
    }
}

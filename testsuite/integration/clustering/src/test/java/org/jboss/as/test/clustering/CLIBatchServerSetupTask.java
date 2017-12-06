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

import java.util.Arrays;
import java.util.List;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Implementation of {@link ServerSetupTask} which runs provided CLI commands.
 *
 * @author Radoslav Husar
 */
public class CLIBatchServerSetupTask implements ServerSetupTask {

    private static final Logger LOG = Logger.getLogger(CLIBatchServerSetupTask.class);
    private final List<String> nodes;
    private final List<String> setupCommands;
    private final List<String> tearDownCommands;

    public CLIBatchServerSetupTask(String[] setupCommands, String[] tearDownCommands) {
        this(null, setupCommands, tearDownCommands);
    }

    public CLIBatchServerSetupTask(String[] nodes, String[] setupCommands, String[] tearDownCommands) {
        this(nodes == null ? null : Arrays.asList(nodes), Arrays.asList(setupCommands), Arrays.asList(tearDownCommands));
    }

    /**
     * Constructs a {@link ServerSetupTask} which is run after the container is started and before the deployment is deployed and its
     * corresponding tear down commands run after undeployment.
     *
     * @param nodes            list of nodes to run the commands on used to run specific setup on concrete nodes; {@code null} to run on all
     * @param setupCommands    list of CLI commands to run on container setup
     * @param tearDownCommands list of CLI commands to run on container tear down, specifically undoing all changes done in setup commands
     */
    public CLIBatchServerSetupTask(List<String> nodes, List<String> setupCommands, List<String> tearDownCommands) {
        this.nodes = nodes;
        this.setupCommands = setupCommands;
        this.tearDownCommands = tearDownCommands;
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        if (nodes == null || nodes.contains(containerId)) {
            this.executeCommands(managementClient, setupCommands);
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (nodes == null || nodes.contains(containerId)) {
            this.executeCommands(managementClient, tearDownCommands);
        }
    }

    private void executeCommands(ManagementClient managementClient, List<String> commands) throws Exception {
        if (commands.isEmpty()) return;

        CommandContext context = CLITestUtil.getCommandContext();
        context.connectController();

        context.getBatchManager().activateNewBatch();
        Batch batch = context.getBatchManager().getActiveBatch();
        commands.forEach(c -> {
            try {
                batch.add(context.toBatchedCommand(c));
            } catch (CommandFormatException e) {
                throw new RuntimeException(e);
            }
        });
        ModelNode commandModel = batch.toRequest();

        // Currently fails with: https://issues.jboss.org/browse/ARQ-1135
        // Add {allow-resource-service-restart=true} operation flag manually since its not exposed on the Batch
        //commandModel.get(ModelDescriptionConstants.OPERATION_HEADERS).get(ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART).set(true);

        ModelNode result = managementClient.getControllerClient().execute(commandModel);

        // TODO do we need to throw an exception here if the op fails?
        LOG.infof("Executed %s with result %s", commands, result.toJSONString(true));
    }
}
/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.shared;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.util.LoggingUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Used to search for strings in logs, it adds a handler so the search is done on a small file
 * @author tmiyar
 *
 */
public abstract class TestLogHandlerSetupTask implements ServerSetupTask {

    private final Logger LOGGER = Logger.getLogger(TestLogHandlerSetupTask.class);
    private final Deque<ModelNode> removeOps = new ArrayDeque<>();

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {

        final Operations.CompositeOperationBuilder builder = Operations.CompositeOperationBuilder.create();

        //add handler
        final ModelNode handlerAddress = Operations.createAddress(SUBSYSTEM, "logging", "file-handler", getHandlerName());
        ModelNode addTestLogOp = Operations.createAddOperation(handlerAddress);
        addTestLogOp.get("level").set(getLevel());
        addTestLogOp.get("append").set("true");
        addTestLogOp.get("encoding").set("UTF-8");
        ModelNode file = new ModelNode();
        file.get("relative-to").set("jboss.server.log.dir");
        file.get("path").set(getLogFileName());
        addTestLogOp.get("file").set(file);
        addTestLogOp.get("formatter").set("%-5p [%c] (%t) %s%e%n");
        builder.addStep(addTestLogOp);
        removeOps.add(Operations.createRemoveOperation(handlerAddress));

        //add category with new handler
        Collection<String> categories = getCategories();
        if (categories == null || categories.isEmpty()) {
            LOGGER.warn("getCategories() returned empty collection.");
            return;
        }

        for (String category : categories) {
            if (category == null || category.isEmpty()) {
                LOGGER.warn("Empty category name provided.");
                continue;
            }
            final ModelNode loggerAddress = Operations.createAddress(SUBSYSTEM, "logging", "logger", category);
            ModelNode op = Operations.createAddOperation(loggerAddress);
            op.get("level").set(getLevel());
            op.get("handlers").add(getHandlerName());
            builder.addStep(op);
            removeOps.addFirst(Operations.createRemoveOperation(loggerAddress));
        }
        applyUpdate(managementClient.getControllerClient(), builder.build(), false);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        Path logPath = LoggingUtil.getLogPath(managementClient, "file-handler", getHandlerName());

        // Remove the loggers and handler
        final Operations.CompositeOperationBuilder builder = Operations.CompositeOperationBuilder.create();
        ModelNode removeOp;
        while ((removeOp = removeOps.pollFirst()) != null) {
            builder.addStep(removeOp);
        }
        applyUpdate(managementClient.getControllerClient(), builder.build(), false);

        // remove file, note this needs to be done after the operations have been executed as we need to ensure that
        // no FD's are open. This can be an issue on Windows.
        Files.deleteIfExists(logPath);
    }

    public abstract Collection<String> getCategories();
    public abstract String getLevel();
    public abstract String getHandlerName();
    public abstract String getLogFileName();

    protected void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowFailure) throws IOException {
        applyUpdate(client, Operation.Factory.create(update), allowFailure);
    }

    private void applyUpdate(final ModelControllerClient client, Operation update, boolean allowFailure) throws IOException {
        ModelNode result = client.execute(update);
        if (!Operations.isSuccessfulOutcome(result)) {
            if (allowFailure) {
                LOGGER.tracef("Failed to configure logger: %s", Operations.getFailureDescription(result).asString());
            } else {
                throw new RuntimeException("Failed to configure logger: " + Operations.getFailureDescription(result).asString());
            }
        } else {
            LOGGER.trace(Operations.readResult(result).asString());
        }
    }
}
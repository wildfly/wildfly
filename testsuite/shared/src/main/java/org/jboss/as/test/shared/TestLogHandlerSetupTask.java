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
import java.util.Collection;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
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

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {

        //add handler
        ModelNode addTestLogOp = org.jboss.as.controller.operations.common.Util.createAddOperation(PathAddress.pathAddress().append(SUBSYSTEM, "logging")
                .append("periodic-rotating-file-handler", getHandlerName()));
        addTestLogOp.get("level").set(getLevel());
        addTestLogOp.get("append").set("true");
        addTestLogOp.get("suffix").set(".yyyy-MM-dd");
        ModelNode file = new ModelNode();
        file.get("relative-to").set("jboss.server.log.dir");
        file.get("path").set(getLogFileName());
        addTestLogOp.get("file").set(file);
        addTestLogOp.get("formatter").set("%-5p [%c] (%t) %s%e%n");
        applyUpdate(managementClient.getControllerClient(), addTestLogOp, false);

        //add category with new handler
        Collection<String> categories = getCategories();
        if (categories == null || categories.isEmpty()) {
            LOGGER.warn("getCategories() returned empty collection.");
            return;
        }

        for (String category : categories) {
            if (category == null || category.length() == 0) {
                LOGGER.warn("Empty category name provided.");
                continue;
            }
            ModelNode op = org.jboss.as.controller.operations.common.Util.createAddOperation(PathAddress.pathAddress().append(SUBSYSTEM, "logging").append("logger", category));
            op.get("level").set(getLevel());
            op.get("handlers").add(getHandlerName());
            applyUpdate(managementClient.getControllerClient(), op, false);
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        // remove file
        Path logPath = LoggingUtil.getLogPath(managementClient, "periodic-rotating-file-handler", getHandlerName());
        Files.deleteIfExists(logPath);

        // remove category with new handler
        Collection<String> categories = getCategories();
        for (String category : categories) {
            if (category == null || category.length() == 0) {
                LOGGER.warn("Empty category name provided.");
                continue;
            }
            ModelNode op = org.jboss.as.controller.operations.common.Util.createRemoveOperation(PathAddress.pathAddress().append(SUBSYSTEM, "logging").append("logger", category));
            applyUpdate(managementClient.getControllerClient(), op, false);
        }
        // remove handler
        ModelNode removeTestLogOp = org.jboss.as.controller.operations.common.Util.createRemoveOperation(PathAddress.pathAddress().append(SUBSYSTEM, "logging")
                .append("periodic-rotating-file-handler", getHandlerName()));
        applyUpdate(managementClient.getControllerClient(), removeTestLogOp, false);
    }

    public abstract Collection<String> getCategories();
    public abstract String getLevel();
    public abstract String getHandlerName();
    public abstract String getLogFileName();

    protected void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowFailure) throws IOException {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && (allowFailure || "success".equals(result.get("outcome").asString()))) {
            if (result.hasDefined("result")) {
                LOGGER.trace(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }
}

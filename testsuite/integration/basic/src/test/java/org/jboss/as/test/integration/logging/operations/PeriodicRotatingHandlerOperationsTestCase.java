/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.logging.operations;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE;

/**
 * @author <a href="mailto:pkremens@redhat.com">Petr Kremensky</a>
 */
@RunWith(Arquillian.class)
public class PeriodicRotatingHandlerOperationsTestCase extends AbstractLoggingOperationsTestCase {
    private static final Logger log = Logger.getLogger(PeriodicRotatingHandlerOperationsTestCase.class.getName());
    private static final String FILE_NAME = "periodic-rotating-handler.log";
    private static final String HANDLER_NAME = PeriodicRotatingHandlerOperationsTestCase.class.getSimpleName();
    private static final ModelNode HANDLER_ADDRESS = createPeriodicRotatingFileHandlerAddress(HANDLER_NAME).toModelNode();
    private static final String PROFILE_NAME = "test-profile";
    private static final ModelNode PROFILE_ADDRESS = createProfileAddress(PROFILE_NAME).toModelNode();
    private static File logFile;

    @ContainerResource
    private ManagementClient managementClient;

    @Override
    protected ManagementClient getManagementClient() {
        return managementClient;
    }

    @After
    public void cleanUp() {
        cleanUpRemove(HANDLER_ADDRESS);
        cleanUpRemove(PROFILE_ADDRESS);
        deleteLogFiles(logFile);
    }

    @Test
    public void testRemove() throws Exception {
        addHandler(HANDLER_ADDRESS);
        executeOperation(Operations.createRemoveOperation(HANDLER_ADDRESS));
        verifyRemoved(HANDLER_ADDRESS);

        // Create the profile
        final ModelNode addOp = Operations.createAddOperation(PROFILE_ADDRESS);
        executeOperation(addOp);
        ModelNode address = createPeriodicRotatingFileHandlerAddress(PROFILE_NAME, HANDLER_NAME).toModelNode();
        addHandler(address);
        executeOperation(Operations.createRemoveOperation(address));
        verifyRemoved(address);
    }

    @Test
    public void testPeriodicHandler() throws Exception {
        testPeriodicHandler(null);

        // Create the profile
        final ModelNode addOp = Operations.createAddOperation(PROFILE_ADDRESS);
        executeOperation(addOp);

        testPeriodicHandler(PROFILE_NAME);
    }

    private void testPeriodicHandler(final String profileName) throws Exception {
        ModelNode address = createPeriodicRotatingFileHandlerAddress(profileName, HANDLER_NAME).toModelNode();

        // Add the handler
        addHandler(address);

        // Write each attribute and check the value
        testWrite(address, "append", "false");
        testWrite(address, "autoflush", "false");
        testWrite(address, "enabled", "false");
        testWrite(address, "encoding", "utf-8");
        final ModelNode file = new ModelNode().setEmptyObject();
        file.get("path").set(logFile.getAbsolutePath() + "-write");
        testWrite(address, "file", file);
        testWrite(address, "filter-spec", "deny");
        testWrite(address, "formatter", "[test] %d{HH:mm:ss,SSS} %-5p [%c] %s%E%n");
        testWrite(address, "level", "INFO");
        testWrite(address, "suffix", "dd_MMM");

        // anything smaller than minute is not allowed
        testFailure(address, "suffix", "dd_MMM_ss");

        // Undefine attributes
        testUndefine(address, "append");
        testUndefine(address, "autoflush");
        testUndefine(address, "enabled");
        testUndefine(address, "encoding");
        testUndefine(address, "filter-spec");
        testUndefine(address, "formatter");
        testUndefine(address, "level");
    }

    private void addHandler(ModelNode address) throws Exception {
        logFile = getAbsoluteLogFilePath(managementClient, FILE_NAME);
        final ModelNode addOp = Operations.createAddOperation(address);
        ModelNode file = new ModelNode();
        addOp.get("suffix").set(".yyyy-MM-dd");
        file.get("path").set(logFile.getAbsolutePath());
        addOp.get(FILE).set(file);
        log.info(addOp.asString());
        ModelNode result = executeOperation(addOp);
        log.info(result.asString());
    }
}
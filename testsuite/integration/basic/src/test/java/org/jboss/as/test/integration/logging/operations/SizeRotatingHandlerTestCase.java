/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author <a href="mailto:pkremens@redhat.com">Petr Kremensky</a>
 */
@RunWith(Arquillian.class)
public class SizeRotatingHandlerTestCase extends AbstractLoggingOperationsTestCase {

    private static String fileName = "size-rotating-reload.log";
    private static final String SIZE_HANDLER_NAME = "sizeRotatingHandlerTestCase";
    private static final ModelNode SIZE_HANDLER_ADDRESS = createAddress("size-rotating-file-handler", SIZE_HANDLER_NAME).toModelNode();

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    ContainerController controller;

    @ArquillianResource(DefaultLoggingServlet.class)
    private URL url;
    private File logFile = null;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "default-logging.war");
        archive.addClasses(DefaultLoggingServlet.class);
        return archive;
    }

    /*
     * Create basic logging configuration
     */
    private void loggingSetup() throws Exception {
        logFile = getAbsoluteLogFilePath(managementClient, fileName);
        // Clear logs
        clearLogs(logFile);

        final ModelControllerClient client = managementClient.getControllerClient();

        // Create the size rotating handler
        ModelNode op = Operations.createAddOperation(SIZE_HANDLER_ADDRESS);
        ModelNode file = new ModelNode();
        file.get("path").set(logFile.getAbsolutePath());
        op.get(FILE).set(file);
        validateResponse(op);

        // Add the handler to the root-logger
        op = Operations.createOperation("add-handler", createRootLoggerAddress().toModelNode());
        op.get(ModelDescriptionConstants.NAME).set(SIZE_HANDLER_NAME);
        validateResponse(op);
    }

    @Test
    @RunAsClient
    public void testEnabled() throws Exception {
        final String disabledMsg = "SizeRotatingHandlerTestCase - This message should not appear in log file";
        final String enabledMsg = "SizeRotatingHandlerTestCase - This message should appear in log file";
        fileName = "size-rotating-reload-testEnabled.log";
        loggingSetup();

        // disable handler
        ModelNode op = Operations.createOperation(DISABLE, SIZE_HANDLER_ADDRESS);
        validateResponse(op);
        op = Operations.createReadAttributeOperation(SIZE_HANDLER_ADDRESS, ENABLED);
        Assert.assertFalse("Handler should be disabled.", validateResponse(op, true).asBoolean());
        searchLog(disabledMsg, false);

        // enable handler
        op = Operations.createOperation(ENABLE, SIZE_HANDLER_ADDRESS);
        validateResponse(op);
        op = Operations.createReadAttributeOperation(SIZE_HANDLER_ADDRESS, ENABLED);
        Assert.assertTrue("Handler should be enabled.", validateResponse(op, true).asBoolean());
        searchLog(enabledMsg, true);
    }

    @Test
    @RunAsClient
    public void rotationTest() throws Exception {
        final String message = "SizeRotatingHandlerTestCase - This is my dummy message which is gonna fill my log file";
        final String newMessage = "SizeRotatingHandlerTestCase - This is new message";
        final int maxBackupIndex = 3;
        fileName = "size-rotating-reload-rotationTest.log";
        loggingSetup();

        ModelNode op = Operations.createWriteAttributeOperation(SIZE_HANDLER_ADDRESS, "max-backup-index", maxBackupIndex);
        validateResponse(op);
        op = Operations.createWriteAttributeOperation(SIZE_HANDLER_ADDRESS, "rotate-size", "2k");
        validateResponse(op);
        op = Operations.createReadResourceOperation(SIZE_HANDLER_ADDRESS);
        validateResponse(op);

        for (int i = 0; i < 100; i++) {
            makeLog(message);
        }
        checkLogs(message, true);

        // check that file size is not greater than 2k and it contains message
        int count = 0;
        for (File file : logFile.getParentFile().listFiles()) {
            if (file.getName().contains(logFile.getName())) {
                long length = file.length();
                Assert.assertTrue(String.format("File %s is bigger than allowed rotate-size. File length = %d",
                        file.getName(), length), length < 2500);
                checkLogs(message, true, file);
                count++;
            }
        }
        // verify max-backup-index
        Assert.assertEquals("Incorrect number of log files were found.", maxBackupIndex + 1, count);
        searchLog(newMessage, true);
    }


    @After
    public void cleanUp() throws Exception {

        // Remove the handler from the root-logger
        ModelNode op = Operations.createOperation("remove-handler", createRootLoggerAddress().toModelNode());
        op.get(ModelDescriptionConstants.NAME).set(SIZE_HANDLER_NAME);
        validateResponse(op);

        // Remove the size rotating handler
        op = Operations.createRemoveOperation(SIZE_HANDLER_ADDRESS);
        validateResponse(op);

        // Clear logs
        clearLogs(logFile);
    }

    @Override
    protected ManagementClient getManagementClient() {
        return managementClient;
    }

    /*
     * Clear all log files for test (don't forget for rotated ones)
     */
    private void clearLogs(File file) {
        for (File f : file.getParentFile().listFiles()) {
            if (f.getName().contains(logFile.getName())) {
                f.delete();
                if (f.exists()) {
                    Assert.fail("Unable to delete file: " + f.getName());
                }
            }
        }
    }

    private ModelNode validateResponse(ModelNode operation) throws Exception {
        return validateResponse(operation, false);
    }

    private ModelNode validateResponse(ModelNode operation, boolean validateResult) throws Exception {
        final ModelNode response = executeOperation(operation);
        if (!Operations.isSuccessfulOutcome(response)) {
            Assert.fail(Operations.getFailureDescription(response).toString());
        }
        if (validateResult) {
            Assert.assertTrue("result exists", response.hasDefined(RESULT));
        }
        return response.get(RESULT);
    }

    /*
     * Make log of message, and search logFile
     */
    private void searchLog(final String msg, final boolean expected) throws Exception {
        makeLog(msg);
        checkLogs(msg, expected);
    }

    private void checkLogs(final String msg, final boolean expected) throws Exception {
        checkLogs(msg, expected, logFile);
    }

    /*
     * Search file for message
     */
    private void checkLogs(final String msg, final boolean expected, File file) throws Exception {

        BufferedReader reader = null;
        // check logs
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            boolean logFound = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains(msg)) {
                    logFound = true;
                    break;
                }
            }
            Assert.assertTrue("Message: \"" + msg + "\" was not found in file: " + file.getName(), logFound == expected);
        } finally {
            safeClose(reader);
        }
    }

    private void makeLog(String msg) throws Exception {
        int statusCode = getResponse(new URL(url, "logger?msg=" + URLEncoder.encode(msg, "utf-8")));
        Assert.assertTrue("Invalid response statusCode: " + statusCode, statusCode == HttpServletResponse.SC_OK);
    }
}


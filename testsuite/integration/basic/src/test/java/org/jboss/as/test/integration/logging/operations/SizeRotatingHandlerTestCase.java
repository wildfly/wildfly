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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author <a href="mailto:pkremens@redhat.com">Petr Kremensky</a>
 */
@RunWith(Arquillian.class)
public class SizeRotatingHandlerTestCase extends AbstractLoggingOperationsTestCase {

    private static String fileName = "size-rotating-handler.log";
    private static final String HANDLER_NAME = SizeRotatingHandlerTestCase.class.getSimpleName();
    private static final ModelNode HANDLER_ADDRESS = createSizeRotatingFileHandlerAddress(HANDLER_NAME).toModelNode();

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource(DefaultLoggingServlet.class)
    private URL url;
    private File logFile = null;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "default-logging.war");
        archive.addClasses(DefaultLoggingServlet.class);
        return archive;
    }

    @Override
    protected ManagementClient getManagementClient() {
        return managementClient;
    }

    @After
    public void cleanUp() throws Exception {
        // Remove the handler from the root-logger
        ModelNode op = Operations.createOperation("remove-handler", createRootLoggerAddress().toModelNode());
        op.get(ModelDescriptionConstants.NAME).set(HANDLER_NAME);
        managementClient.getControllerClient().execute(op);

        // Remove handler
        cleanUpRemove(HANDLER_ADDRESS);

        // Clear logs
        deleteLogFiles(logFile);
    }

    @Test
    @RunAsClient
    public void testEnabled() throws Exception {
        final String disabledMsg = "SizeRotatingHandlerTestCase - This message should not appear in log file";
        final String enabledMsg = "SizeRotatingHandlerTestCase - This message should appear in log file";
        fileName = "size-rotating-reload-testEnabled.log";
        loggingSetup();

        // disable handler
        ModelNode op = Operations.createOperation(DISABLE, HANDLER_ADDRESS);
        validateResponse(op);
        op = Operations.createReadAttributeOperation(HANDLER_ADDRESS, ENABLED);
        Assert.assertFalse("Handler should be disabled.", validateResponse(op, true).asBoolean());
        searchLog(disabledMsg, false);

        // enable handler
        op = Operations.createOperation(ENABLE, HANDLER_ADDRESS);
        validateResponse(op);
        op = Operations.createReadAttributeOperation(HANDLER_ADDRESS, ENABLED);
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

        ModelNode op = Operations.createWriteAttributeOperation(HANDLER_ADDRESS, "max-backup-index", maxBackupIndex);
        validateResponse(op);
        op = Operations.createWriteAttributeOperation(HANDLER_ADDRESS, "rotate-size", "2k");
        validateResponse(op);
        op = Operations.createReadResourceOperation(HANDLER_ADDRESS);
        validateResponse(op);

        for (int i = 0; i < 100; i++) {
            makeLog(message);
        }
        checkLogs(message, true);

        // Check that file size is not greater than 2k and it contains message
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
        // Verify max-backup-index
        Assert.assertEquals("Incorrect number of log files were found.", maxBackupIndex + 1, count);
        searchLog(newMessage, true);
    }

    @Test
    @RunAsClient
    public void testSuffix() throws Exception {
        final String str1 = "first-string";
        final String str2 = "second-string";
        final String str3 = "third-string";
        final String suffix = "MMM_yyyy";

        loggingSetup();
        final String resolvedSuffix = new SimpleDateFormat(suffix).format(Calendar.getInstance().getTime());
        final File firstBackup = new File(logFile.getAbsolutePath() + resolvedSuffix + ".1");
        final File secondBackup = new File(logFile.getAbsolutePath() + resolvedSuffix + ".2");
        final File lastBackup = new File(logFile.getAbsolutePath() + ".1");

        Assert.assertTrue("There shouldn't be any rotated file at the beginning of the test.", countBackups() < 1);

        ModelNode op = Operations.createWriteAttributeOperation(HANDLER_ADDRESS, "suffix", suffix);
        validateResponse(op);
        op = Operations.createWriteAttributeOperation(HANDLER_ADDRESS, "rotate-size", "1k");
        validateResponse(op);
        op = Operations.createWriteAttributeOperation(HANDLER_ADDRESS, "max-backup-index", 2);
        validateResponse(op);

        // Test first file rotation
        rotateLogFile(str1, 1);
        searchLog(str2, true);
        checkLogs(str1, true, firstBackup);
        // Test second file rotation
        rotateLogFile(str2, 2);
        searchLog(str3, true);
        checkLogs(str2, true, firstBackup);
        checkLogs(str1, true, secondBackup);

        // Remove the suffix and wait for another rotation
        op = Operations.createUndefineAttributeOperation(HANDLER_ADDRESS, "suffix");
        validateResponse(op);

        // Verify that the oldest log file was rotated
        for (int i = 0; i < 100; i++) {
            makeLog(lastBackup.getName() + i);
            if (lastBackup.exists()) {
                break;
            }
        }

        // BZ1148842 - Undefining suffix on size-rotating-file-handler sets invalid value on suffix
        Assert.assertTrue("Failed to find rotated file once suffix was undefined.", lastBackup.exists());
        checkLogs(lastBackup.getName(), true, lastBackup);
    }

    /*
     * Log file should rotate after ~10 iterations. Using this to avoid infinite loop
     */
    private void rotateLogFile(String message, int expectedBackups) throws Exception {
        for (int i = 0; i < 100; i++) {
            makeLog(message);
            if (countBackups() == expectedBackups) {
                return;
            }
        }
        Assert.fail("Failed to find a backup log files.");
    }

    private int countBackups() {
        int backups = -1;
        if (logFile.exists()) {
            for (File file : logFile.getParentFile().listFiles()) {
                if (file.getName().contains(fileName)) {
                    System.out.println(file.getName());
                    backups++;
                }
            }
        }
        return backups;
    }

    /*
     * Create basic logging configuration
     */
    private void loggingSetup() throws Exception {
        logFile = getAbsoluteLogFilePath(managementClient, fileName);
        // Clear logs
        deleteLogFiles(logFile);

        // Create the size rotating handler
        ModelNode op = Operations.createAddOperation(HANDLER_ADDRESS);
        ModelNode file = new ModelNode();
        file.get("path").set(logFile.getAbsolutePath());
        op.get(FILE).set(file);
        validateResponse(op);

        // Add the handler to the root-logger
        op = Operations.createOperation("add-handler", createRootLoggerAddress().toModelNode());
        op.get(ModelDescriptionConstants.NAME).set(HANDLER_NAME);
        validateResponse(op);
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
        Assert.assertTrue("File '" + file.getName() + "' doesn't exists.", file.exists());
        BufferedReader reader = null;
        // check logs
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
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


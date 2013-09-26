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

package org.jboss.as.test.manualmode.logging;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author <a href="mailto:pkremens@redhat.com">Petr Kremensky</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SizeAppenderRestartTestCase {
    private static final Logger log = Logger.getLogger(SizeAppenderRestartTestCase.class);
    private static final String CONTAINER = "default-jbossas";
    private static final String DEPLOYMENT = "logging-deployment";
    private static final String FILE_NAME = "sizeAppenderRestartTestCase.log";
    private static final String SIZE_HANDLER_NAME = "sizeAppenderRestartTestCase";
    private static final ModelNode SIZE_HANDLER_ADDRESS = new ModelNode();
    private static final ModelNode ROOT_LOGGER_ADDRESS = new ModelNode();
    private static File logFile;
    @ArquillianResource
    private ContainerController container;
    @ArquillianResource
    private Deployer deployer;
    private final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
    private final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());


    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        archive.addClasses(LoggingServlet.class);
        return archive;
    }

    @Test
    @InSequence(-1)
    public void startContainer() throws Exception {
        SIZE_HANDLER_ADDRESS.add(SUBSYSTEM, "logging")
                .add("size-rotating-file-handler", SIZE_HANDLER_NAME);
        ROOT_LOGGER_ADDRESS.add(SUBSYSTEM, "logging")
                .add("root-logger", "ROOT");

        // Remove log files
        clearLogs(logFile);
        // Start the server
        container.start(CONTAINER);
        Assert.assertTrue("Container is not started", managementClient.isServerInRunningState());
        // Deploy the servlet
        deployer.deploy(DEPLOYMENT);

        logFile = getAbsoluteLogFilePath(client);

        // Create the size-rotating handler
        ModelNode op = Operations.createAddOperation(SIZE_HANDLER_ADDRESS);
        ModelNode file = new ModelNode();
        file.get("path").set(logFile.getAbsolutePath());
        op.get(FILE).set(file);
        validateResponse(op);

        // Add the handler to the root-logger
        op = Operations.createOperation("add-handler", ROOT_LOGGER_ADDRESS);
        op.get(NAME).set(SIZE_HANDLER_NAME);
        validateResponse(op);
    }

    @Test
    @InSequence(1)
    public void stopContainer() throws Exception {
        // Remove the servlet
        deployer.undeploy(DEPLOYMENT);

        // Remove the handler from the root-logger
        ModelNode op = Operations.createOperation("remove-handler", ROOT_LOGGER_ADDRESS);
        op.get(NAME).set(SIZE_HANDLER_NAME);
        validateResponse(op);

        // Remove the size-rotating handler
        op = Operations.createRemoveOperation(SIZE_HANDLER_ADDRESS);
        validateResponse(op);

        // Stop the container
        container.stop(CONTAINER);
        Assert.assertFalse("Container is not stopped", managementClient.isServerInRunningState());

        // Remove log files
        clearLogs(logFile);

        safeClose(client);
        // safeClose the managementClient
        if (managementClient != null) try {
            managementClient.close();
        } catch (Exception ignore) {
            // ignore
        }
    }

    /*
     * append = true:    restart -> logs are appended to same log file, unless it reach rotate-size
     */
    @Test
    public void appendTrueTest(@ArquillianResource URL url) throws Exception {
        long fileSize = appendTest("true", url);
        log.info("original file size  = " + fileSize);
        log.info("new file size       = " + logFile.length());
        Assert.assertTrue("Size of log file should be bigger after reload", fileSize < logFile.length());
    }

    /*
     * append = false:   restart -> original log file is rewritten
     */
    @Test
    public void appendFalseTest(@ArquillianResource URL url) throws Exception {
        long fileSize = appendTest("false", url);
        log.info("original file size  = " + fileSize);
        log.info("new file size       = " + logFile.length());
        Assert.assertTrue("Size of log file should be smaller after reload", fileSize > logFile.length());
    }

    private long appendTest(String append, URL url) throws Exception {
        final String message = "SizeAppenderRestartTestCase - This is my dummy message which is gonna fill my log file";
        long fileSize;
        // set append attribute, rotate-on boot and reload server
        ModelNode op = Operations.createWriteAttributeOperation(SIZE_HANDLER_ADDRESS, "append", append);
        validateResponse(op);
        op = Operations.createWriteAttributeOperation(SIZE_HANDLER_ADDRESS, "rotate-on-boot", false);
        validateResponse(op);
        restartServer(true);

        // make some (more than server start) logs, remember the size of log file, reload server, check new size of file
        op = Operations.createReadResourceOperation(SIZE_HANDLER_ADDRESS);
        validateResponse(op);
        for (int i = 0; i < 100; i++) {
            makeLog(message, url);
        }
        checkLogs(message);
        fileSize = logFile.length();
        restartServer(false);

        // logFile.getParentFile().listFiles().length creates array with length of 3
        int count = 0;
        File[] logs = null;
        try {
            logs = logFile.getParentFile().listFiles();
        } catch (NullPointerException npe) {
            Assert.fail("Failed to find any log file");
        }
        for (File f : logs) {
            if (f.getName().contains(logFile.getName())) {
                count++;
            }
        }
        Assert.assertEquals("There should be only one log file", 1, count);
        return fileSize;
    }

    /*
     * rotate-on-boot = true:   restart -> log file is rotated, logs are written to new file
     */
    @Test
    public void rotateFileOnRestartTest(@ArquillianResource URL url) throws Exception {
        final String oldMessage = "SizeAppenderRestartTestCase - This is old message";
        final String newMessage = "SizeAppenderRestartTestCase - This is new message";
        ModelNode op = Operations.createWriteAttributeOperation(SIZE_HANDLER_ADDRESS, "rotate-on-boot", true);
        validateResponse(op);
        restartServer(true);

        // make some logs, remember file size, restart
        makeLog(oldMessage, url);
        checkLogs(oldMessage);
        restartServer(false);

        // make log to new rotated log file
        makeLog(newMessage, url);
        checkLogs(newMessage);

        // verify that file was rotated
        int count = 0;
        File[] logs = null;
        try {
            logs = logFile.getParentFile().listFiles();
        } catch (NullPointerException npe) {
            Assert.fail("Failed to find any log file");
        }
        for (File file : logs) {
            if (file.getName().contains(logFile.getName())) {
                count++;
                if (file.getName().equals(logFile.getName() + ".1")) {
                    checkLogs(newMessage, false, file);
                    checkLogs(oldMessage, true, file);
                }
            }
        }
        Assert.assertEquals("There should be two log files", 2, count);
    }

    private void restartServer(boolean deleteLogs) {
        Assert.assertTrue("Container is not running", managementClient.isServerInRunningState());
        // Stop the container
        container.stop(CONTAINER);
        if (deleteLogs) {
            clearLogs(logFile);
        }
        // Start the server again
        container.start(CONTAINER);
        Assert.assertTrue("Container is not started", managementClient.isServerInRunningState());
    }

    private void checkLogs(final String msg) throws Exception {
        checkLogs(msg, true, logFile);
    }

    /*
     * Search file for message
     */
    private void checkLogs(final String msg, final boolean expected, File file) throws Exception {
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
        log.info("Message: \"" + msg + "\" was found in file: " + file.getName());
    }

    private void makeLog(String msg, URL url) throws Exception {
        int statusCode = getResponse(new java.net.URL(url, "logger?msg=" + URLEncoder.encode(msg, "utf-8")));
        Assert.assertTrue("Invalid response statusCode: " + statusCode, statusCode == HttpServletResponse.SC_OK);
    }

    private int getResponse(URL url) throws IOException {
        return ((HttpURLConnection) url.openConnection()).getResponseCode();
    }


    private void validateResponse(ModelNode operation) throws Exception {
        ModelNode response;
        log.info(operation.asString());
        response = client.execute(operation);
        log.info(response.asString());
        if (!SUCCESS.equals(response.get(OUTCOME).asString())) {
            Assert.fail(response.get(FAILURE_DESCRIPTION).toString());
        }
    }

    private void clearLogs(File file) {
        File[] logs = null;
        try {
            logs = file.getParentFile().listFiles();
        } catch (NullPointerException ignore) {
        }
        if (logs != null) {
            for (File f : logs) {
                if (f.getName().contains(logFile.getName())) {
                    f.delete();
                    log.info("Deleted: " + f.getAbsolutePath());
                    if (f.exists()) {
                        Assert.fail("Unable to delete file: " + f.getName());
                    }
                }
            }
        }
    }

    private static File getAbsoluteLogFilePath(final ModelControllerClient client) throws IOException, MgmtOperationException {
        final ModelNode address = new ModelNode().setEmptyList();
        address.add(PATH, "jboss.server.log.dir");
        final ModelNode op = Operations.createReadAttributeOperation(address, PATH);
        final ModelNode result = client.execute(op);
        if (Operations.isSuccessfulOutcome(result)) {
            return new File(Operations.readResult(result).asString(), FILE_NAME);
        }
        throw new MgmtOperationException("Failed to read the path resource", op, result);
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Exception ignore) {
            // ignore
        }
    }
}
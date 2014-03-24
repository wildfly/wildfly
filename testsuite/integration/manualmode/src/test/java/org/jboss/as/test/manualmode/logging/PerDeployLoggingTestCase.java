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

package org.jboss.as.test.manualmode.logging;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.test.manualmode.logging.Log4jAppenderTestCase.safeClose;

/**
 * Create a deployment with per-deploy logging configuration file.
 * Verify that per-deploy logging has preference to logging subsystem (use-deployment-logging-config=true).
 * Set use-deployment-logging-config=false and verify that per-deploy configuration is no longer used.
 *
 * @author <a href="mailto:pkremens@redhat.com">Petr Kremensky</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PerDeployLoggingTestCase {
    private static final Logger log = Logger.getLogger(PerDeployLoggingTestCase.class.getName());
    private static final String CONTAINER = "default-jbossas";
    private static final String DEPLOYMENT = "per-deploy-logging";

    private static final String DEPLOYMENT_LOG_MESSAGE = "Deployment logging configuration message.";
    private static final String LOGGING_SUBSYSTEM_MESSAGE = "Per deploy logging disabled message.";
    private static final String PER_DEPLOY_ATTRIBUTE = "use-deployment-logging-config";
    private static final String FILE_HANDLER_NAME = "customFileAppender";
    private static final String FILE_HANDLER_FILE_NAME = "custom-file-logger.log";
    private static final String PER_DEPLOY_FILE_NAME = "per-deploy-logging.log";

    private static File customLog;
    private static File perDeployLog;

    private static ModelNode LOGGING_SUBSYSTEM = new ModelNode().setEmptyList();
    private static ModelNode ROOT_LOGGER_ADDRESS = new ModelNode().setEmptyList();
    private static ModelNode FILE_HANDLER_ADDRESS = new ModelNode().setEmptyList();


    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        archive.addClasses(LoggingServlet.class)
                .addAsResource("logging/per-deploy-logging.properties", "jboss-logging.properties");
        return archive;
    }

    @BeforeClass
    public static void setupAddress() {
        LOGGING_SUBSYSTEM.add(ModelDescriptionConstants.SUBSYSTEM, "logging");
        FILE_HANDLER_ADDRESS.add(LOGGING_SUBSYSTEM.get(0))
                .add(FILE_HANDLER, FILE_HANDLER_NAME);
        ROOT_LOGGER_ADDRESS.add(LOGGING_SUBSYSTEM.get(0))
                .add("root-logger", "ROOT");
    }

    @Before
    public void prepareContainer() throws Exception {
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        try {
            // Start the container
            container.start(CONTAINER);

            // Get custom log files
            perDeployLog = getAbsoluteLogFilePath(client, PER_DEPLOY_FILE_NAME);
            customLog = getAbsoluteLogFilePath(client, FILE_HANDLER_FILE_NAME);

            // Create the custom file handler
            ModelNode op = Operations.createAddOperation(FILE_HANDLER_ADDRESS);
            ModelNode file = new ModelNode();
            file.get(PATH).set(customLog.getAbsolutePath());
            op.get(FILE).set(file);
            validateResponse(op, client);

            // Add the handler to the root-logger
            op = Operations.createOperation("add-handler", ROOT_LOGGER_ADDRESS);
            op.get(NAME).set(FILE_HANDLER_NAME);
            validateResponse(op, client);

        } finally {
            safeClose(managementClient);
            safeClose(client);
        }
        deployer.deploy(DEPLOYMENT);
    }

    @After
    public void stopContainer() throws Exception {
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        try {
            // Remove the servlet
            deployer.undeploy(DEPLOYMENT);

            // Set use-deployment-logging-config to true
            ModelNode op = Operations.createWriteAttributeOperation(LOGGING_SUBSYSTEM, PER_DEPLOY_ATTRIBUTE, true);
            validateResponse(op, client);

            // Remove the custom handler from the root-logger
            op = Operations.createOperation("remove-handler", ROOT_LOGGER_ADDRESS);
            op.get(NAME).set(FILE_HANDLER_NAME);
            validateResponse(op, client);

            // Remove the custom file handler
            op = Operations.createRemoveOperation(FILE_HANDLER_ADDRESS);
            validateResponse(op, client);

            // Stop the container
            container.stop(CONTAINER);
            clearLogFiles();
        } finally {
            safeClose(client);
        }
    }

    @Test
    public void disablePerDeployLogging(@ArquillianResource URL url) throws Exception {
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        Assert.assertTrue("Container is not started", managementClient.isServerInRunningState());

        try {
            // Per-deploy logging test
            Response response = getResponse(new URL(url, "logger?msg=" + URLEncoder.encode(DEPLOYMENT_LOG_MESSAGE, "utf-8")));
            Assert.assertTrue("Invalid response statusCode: " + response + " URL: " + url, response.statusCode == HttpServletResponse.SC_OK);
            checkLogs(DEPLOYMENT_LOG_MESSAGE, perDeployLog, true);
            checkLogs(DEPLOYMENT_LOG_MESSAGE, customLog, false);

            // Set use-deployment-logging-config to false
            ModelNode op = Operations.createWriteAttributeOperation(LOGGING_SUBSYSTEM, PER_DEPLOY_ATTRIBUTE, false);
            validateResponse(op, client);

            // Restart the container and clean the logs
            container.stop(CONTAINER);
            clearLogFiles();
            container.start(CONTAINER);

            // Per-deploy logging disabled test
            response = getResponse(new URL(url, "logger?msg=" + URLEncoder.encode(LOGGING_SUBSYSTEM_MESSAGE, "utf-8")));
            Assert.assertTrue("Invalid response statusCode: " + response + " URL: " + url, response.statusCode == HttpServletResponse.SC_OK);
            checkLogs(LOGGING_SUBSYSTEM_MESSAGE, perDeployLog, false);
            checkLogs(LOGGING_SUBSYSTEM_MESSAGE, customLog, true);

        } finally {
            safeClose(managementClient);
            safeClose(client);
        }
    }

    private Response getResponse(final URL url) throws IOException {
        final HttpURLConnection connection = ((HttpURLConnection) url.openConnection());
        return new Response(connection.getResponseCode(), connection.getResponseMessage());
    }

    private class Response {
        final int statusCode;
        final String message;

        Response(final int statusCode, final String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        @Override
        public String toString() {
            return "Response: " + statusCode + " - " + message;
        }
    }

    static File getAbsoluteLogFilePath(final ModelControllerClient client, final String fileName) throws IOException, MgmtOperationException {
        final ModelNode address = new ModelNode().setEmptyList();
        address.add(ModelDescriptionConstants.PATH, "jboss.server.log.dir");
        final ModelNode op = Operations.createReadAttributeOperation(address, ModelDescriptionConstants.PATH);
        final ModelNode result = client.execute(op);
        if (Operations.isSuccessfulOutcome(result)) {
            return new File(Operations.readResult(result).asString(), fileName);
        }
        throw new MgmtOperationException("Failed to read the path resource", op, result);
    }

    private void clearLogFiles() {
        File[] logFiles = {customLog, perDeployLog};
        for (File log : logFiles) {
            if (log.exists()) {
                Assert.assertTrue("Failed to delete " + log.getName(), log.delete());
            }
        }
    }

    static void checkLogs(final String msg, final File file, final boolean expected) throws Exception {
        boolean logFound = false;
        if (file.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(msg)) {
                        logFound = true;
                        break;
                    }
                }
            } finally {
                safeClose(reader);
            }
        }
        Assert.assertEquals(String.format("Message '%s' found in %s", msg, file.getName()), expected, logFound);
    }

    private void validateResponse(ModelNode operation, ModelControllerClient client) throws Exception {
        ModelNode response;
        log.info(operation.asString());
        response = client.execute(operation);
        log.info(response.asString());
        if (!SUCCESS.equals(response.get(OUTCOME).asString())) {
            safeClose(client);
            Assert.fail(response.get(FAILURE_DESCRIPTION).toString());
        }
    }
}

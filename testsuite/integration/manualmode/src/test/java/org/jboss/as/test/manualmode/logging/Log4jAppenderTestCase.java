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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;

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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class Log4jAppenderTestCase {

    public static final String CONTAINER = "default-jbossas";
    public static final String DEPLOYMENT = "logging-deployment";
    private static final String FILE_NAME = "log4j-appender-file.log";
    private static final String CUSTOM_HANDLER_NAME = "customFileAppender";
    private static ModelNode CUSTOM_HANDLER_ADDRESS = new ModelNode().setEmptyList();
    private static ModelNode ROOT_LOGGER_ADDRESS = new ModelNode().setEmptyList();

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    private File logFile = null;

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        archive.addClasses(LoggingServlet.class);
        return archive;
    }

    @BeforeClass
    public static void setupAddresses() {
        CUSTOM_HANDLER_ADDRESS = new ModelNode().setEmptyList();
        CUSTOM_HANDLER_ADDRESS.add(ModelDescriptionConstants.SUBSYSTEM, "logging")
                .add("custom-handler", CUSTOM_HANDLER_NAME);

        ROOT_LOGGER_ADDRESS = new ModelNode().setEmptyList();
        ROOT_LOGGER_ADDRESS.add(ModelDescriptionConstants.SUBSYSTEM, "logging")
                .add("root-logger", "ROOT");
    }

    @Before
    public void startContainer() throws Exception {
        // Start the server
        container.start(CONTAINER);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        try {
            logFile = getAbsoluteLogFilePath(client);

            // Create the custom handler
            ModelNode op = Operations.createAddOperation(CUSTOM_HANDLER_ADDRESS);
            op.get("class").set("org.apache.log4j.FileAppender");
            op.get("module").set("org.apache.log4j");
            ModelNode opProperties = op.get("properties").setEmptyObject();
            opProperties.get("file").set(logFile.getAbsolutePath());
            opProperties.get("immediateFlush").set(true);
            client.execute(op);

            // Add the handler to the root-logger
            op = Operations.createOperation("add-handler", ROOT_LOGGER_ADDRESS);
            op.get(ModelDescriptionConstants.NAME).set(CUSTOM_HANDLER_NAME);
            client.execute(op);

            // Stop the container
            container.stop(CONTAINER);
            // Start the server again
            container.start(CONTAINER);

            Assert.assertTrue("Container is not started", managementClient.isServerInRunningState());

            // Deploy the servlet
            deployer.deploy(DEPLOYMENT);
        } finally {
            safeClose(managementClient);
            safeClose(client);
        }
    }

    @After
    public void stopContainer() throws Exception {
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        try {
            // Remove the servlet
            deployer.undeploy(DEPLOYMENT);

            // Remove the handler from the root-logger
            ModelNode op = Operations.createOperation("remove-handler", ROOT_LOGGER_ADDRESS);
            op.get(ModelDescriptionConstants.NAME).set(CUSTOM_HANDLER_NAME);
            client.execute(op);

            // Remove the custom handler
            op = Operations.createRemoveOperation(CUSTOM_HANDLER_ADDRESS);
            client.execute(op);

            if (logFile != null) logFile.delete();

            // Stop the container
            container.stop(CONTAINER);
        } finally {
            safeClose(client);
        }
    }

    @Test
    public void logAfterReload(@ArquillianResource URL url) throws Exception {
        // Write the message to the server
        final String msg = "Logging test: Log4jCustomHandlerTestCase.logAfterReload";
        searchLog(url, msg, true);
    }

    private void searchLog(final URL url, final String msg, final boolean expected) throws Exception {
        BufferedReader reader = null;
        try {
            final Response response = getResponse(new URL(url, "logger?msg=" + URLEncoder.encode(msg, "utf-8")));
            Assert.assertTrue("Invalid response statusCode: " + response + " URL: " + url, response.statusCode == HttpServletResponse.SC_OK);
            // check logs
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8));
            String line;
            boolean logFound = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains(msg)) {
                    logFound = true;
                    break;
                }
            }
            Assert.assertTrue(logFound == expected);
        } finally {
            safeClose(reader);
        }
    }

    static File getAbsoluteLogFilePath(final ModelControllerClient client) throws IOException, MgmtOperationException {
        final ModelNode address = new ModelNode().setEmptyList();
        address.add(ModelDescriptionConstants.PATH, "jboss.server.log.dir");
        final ModelNode op = Operations.createReadAttributeOperation(address, ModelDescriptionConstants.PATH);
        final ModelNode result = client.execute(op);
        if (Operations.isSuccessfulOutcome(result)) {
            return new File(Operations.readResult(result).asString(), FILE_NAME);
        }
        throw new MgmtOperationException("Failed to read the path resource", op, result);
    }

    static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Exception ignore) {
            // ignore
        }
    }

    Response getResponse(final URL url) throws IOException {
        final HttpURLConnection connection = ((HttpURLConnection) url.openConnection());
        return new Response(connection.getResponseCode(), connection.getResponseMessage());
    }

    static class Response {
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
}

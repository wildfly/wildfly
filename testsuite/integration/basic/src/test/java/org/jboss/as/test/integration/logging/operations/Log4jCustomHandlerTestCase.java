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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ServerSetup(Log4jCustomHandlerTestCase.Log4jCustomHandlerSetUp.class)
@RunWith(Arquillian.class)
public class Log4jCustomHandlerTestCase extends AbstractLoggingOperationsTestCase {

    private static final String FILE_NAME = "log4j-appender-file.log";
    private static final String CUSTOM_HANDLER_NAME = "customFileAppender";
    private static final PathAddress CUSTOM_HANDLER_ADDRESS = createCustomHandlerAddress(CUSTOM_HANDLER_NAME);
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

    @Before
    public void setLogFile() throws Exception {
        if (logFile == null) {
            logFile = getAbsoluteLogFilePath(managementClient, FILE_NAME);
        }
    }

    @Override
    protected ManagementClient getManagementClient() {
        return managementClient;
    }

    @Test
    @RunAsClient
    public void defaultLoggingTest() throws Exception {
        final String msg = "Logging test: Log4jCustomHandlerTestCase.defaultLoggingTest";
        searchLog(msg, true);
    }

    @Test
    @RunAsClient
    public void disabledLoggerTest() throws Exception {
        setEnabled(CUSTOM_HANDLER_ADDRESS, false);

        try {
            final String msg = "Logging Test: Log4jCustomHandlerTestCase.disabledLoggerTest";
            searchLog(msg, false);
        } finally {
            setEnabled(CUSTOM_HANDLER_ADDRESS, true);
        }
    }

    private void searchLog(final String msg, final boolean expected) throws Exception {
        BufferedReader reader = null;
        try {
            int statusCode = getResponse(new URL(url, "logger?msg=" + URLEncoder.encode(msg, "utf-8")));
            Assert.assertTrue("Invalid response statusCode: " + statusCode, statusCode == HttpServletResponse.SC_OK);
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
            Assert.assertTrue(msg + " not found in " + logFile, logFound == expected);
        } finally {
            safeClose(reader);
        }
    }

    static class Log4jCustomHandlerSetUp implements ServerSetupTask {

        private File logFile;

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            logFile = getAbsoluteLogFilePath(managementClient, FILE_NAME);
            final ModelControllerClient client = managementClient.getControllerClient();

            // Create the custom handler
            ModelNode op = Operations.createAddOperation(CUSTOM_HANDLER_ADDRESS.toModelNode());
            op.get("class").set("org.apache.log4j.FileAppender");
            op.get("module").set("org.apache.log4j");
            ModelNode opProperties = op.get("properties").setEmptyObject();
            opProperties.get("file").set(logFile.getAbsolutePath());
            opProperties.get("immediateFlush").set(true);
            execute(client, op);

            // Add the handler to the root-logger
            op = Operations.createOperation("add-handler", createRootLoggerAddress().toModelNode());
            op.get(ModelDescriptionConstants.NAME).set(CUSTOM_HANDLER_NAME);
            execute(client, op);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelControllerClient client = managementClient.getControllerClient();

            // Remove the handler from the root-logger
            ModelNode op = Operations.createOperation("remove-handler", createRootLoggerAddress().toModelNode());
            op.get(ModelDescriptionConstants.NAME).set(CUSTOM_HANDLER_NAME);
            execute(client, op);

            // Remove the custom handler
            op = Operations.createRemoveOperation(CUSTOM_HANDLER_ADDRESS.toModelNode());
            execute(client, op);

            if (logFile != null) logFile.delete();

        }

    }
}

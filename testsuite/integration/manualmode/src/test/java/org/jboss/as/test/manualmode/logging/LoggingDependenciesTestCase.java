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

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
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
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.test.manualmode.logging.Log4jAppenderTestCase.safeClose;

/**
 * Create a deployment with dependency to log4j module.
 * Verify, that it can be deployed (server logging modules should be used by default).
 * Disable the server logging modules (add-logging-api-dependencies=false).
 * Verify, that exception is thrown during deployment.
 *
 * @author <a href="mailto:pkremens@redhat.com">Petr Kremensky</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LoggingDependenciesTestCase {
    private static final Logger log = Logger.getLogger(LoggingDependenciesTestCase.class.getName());
    private static final String CONTAINER = "default-jbossas";
    private static final String DEPLOYMENT = "log4j-deployment";
    private static ModelNode LOGGING_SUBSYSTEM = new ModelNode().setEmptyList();
    private static final String API_DEPENDENCIES = "add-logging-api-dependencies";

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        archive.addClasses(Log4jServlet.class);
        return archive;
    }

    @BeforeClass
    public static void setupAddress() {
        LOGGING_SUBSYSTEM.add(ModelDescriptionConstants.SUBSYSTEM, "logging");
    }

    @Before
    public void startContainer() throws Exception {
        // Start the container
        container.start(CONTAINER);
    }

    @Test(expected = DeploymentException.class)
    public void disableLoggingDependencies() throws Exception {
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        Assert.assertTrue("Container is not started", managementClient.isServerInRunningState());

        try {
            // Test default behaviour
            deployer.deploy(DEPLOYMENT);
            deployer.undeploy(DEPLOYMENT);
        } catch (Exception ex) {
            safeClose(managementClient);
            safeClose(client);
            Assert.fail("Exception caught while deploying the application with dependency on log4j module.");
        }

        try {
            // Set add-logging-api-dependencies to false
            ModelNode op = Operations.createWriteAttributeOperation(LOGGING_SUBSYSTEM, API_DEPENDENCIES, false);
            validateResponse(op, client);
            // Restart the container, expect the exception during deployment
            container.stop(CONTAINER);
            container.start(CONTAINER);
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

            // Set add-logging-api-dependencies to true
            ModelNode op = Operations.createWriteAttributeOperation(LOGGING_SUBSYSTEM, API_DEPENDENCIES, true);
            validateResponse(op, client);

            // Stop the container
            container.stop(CONTAINER);
        } finally {
            safeClose(client);
        }
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
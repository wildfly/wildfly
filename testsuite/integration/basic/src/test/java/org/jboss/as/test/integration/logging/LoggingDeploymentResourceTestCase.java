/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.logging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that deployments that have a {@code logging.properties} file are configured correctly.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LoggingDeploymentResourceTestCase {

    private static final String WAR_DEPLOYMENT_NAME = "logging-test-war.war";
    private static final String EAR_DEPLOYMENT_NAME = "logging-test-ear.ear";
    private static final String EAR_WAR_DEPLOYMENT_NAME = "logging-test-ear.war";
    private static final String EAR_PARENT_DEPLOYMENT_NAME = "logging-test-parent-ear.ear";
    private static final String EAR_CHILD_WAR_DEPLOYMENT_NAME = "logging-test-child-ear.war";


    @ArquillianResource
    private static ManagementClient client;

    @Deployment(name = WAR_DEPLOYMENT_NAME)
    public static WebArchive createWarDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT_NAME);
        // Create a logging.properties file
        final Properties config = createLoggingConfig("test-logging-war.log");
        war.addAsManifestResource(createAsset(config), "logging.properties");
        return war;
    }

    @Deployment(name = EAR_DEPLOYMENT_NAME)
    public static EnterpriseArchive createEarDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_NAME);
        // Create a logging.properties file
        final Properties config = createLoggingConfig("test-logging-ear.log");
        ear.addAsManifestResource(createAsset(config), "logging.properties");
        ear.addAsModule(ShrinkWrap.create(WebArchive.class, EAR_WAR_DEPLOYMENT_NAME).addClass(Dummy.class));
        return ear;
    }

    @Deployment(name = EAR_PARENT_DEPLOYMENT_NAME)
    public static EnterpriseArchive createEarSepDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_PARENT_DEPLOYMENT_NAME);
        // Create a logging.properties file
        final Properties earConfig = createLoggingConfig("test-logging-parent-ear.log");
        ear.addAsManifestResource(createAsset(earConfig), "logging.properties");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, EAR_CHILD_WAR_DEPLOYMENT_NAME);
        // Create a logging.properties file
        final Properties warConfig = createLoggingConfig("test-logging-child-war.log");
        war.addAsManifestResource(createAsset(warConfig), "logging.properties");
        ear.addAsModule(war);
        return ear;
    }

    @OperateOnDeployment(WAR_DEPLOYMENT_NAME)
    @Test
    public void testWarDeploymentConfigurationResource() throws Exception {
        final ModelNode loggingConfiguration = readDeploymentResource(WAR_DEPLOYMENT_NAME, WAR_DEPLOYMENT_NAME + "/META-INF/logging.properties");
        // The address should have logging.properties
        final Deque<Property> resultAddress = new ArrayDeque<>(Operations.getOperationAddress(loggingConfiguration).asPropertyList());
        Assert.assertTrue("The configuration path did not include logging.properties", resultAddress.getLast().getValue().asString().contains("logging.properties"));

        final ModelNode handler = loggingConfiguration.get("handler", "FILE");
        Assert.assertTrue("The FILE handler was not found effective configuration", handler.isDefined());
        Assert.assertTrue(handler.hasDefined("properties"));
        String fileName = null;
        // Find the fileName property
        for (Property property : handler.get("properties").asPropertyList()) {
            if ("fileName".equals(property.getName())) {
                fileName = property.getValue().asString();
                break;
            }
        }
        Assert.assertNotNull("fileName property not found", fileName);
        Assert.assertTrue(fileName.endsWith("test-logging-war.log"));
    }

    @OperateOnDeployment(EAR_DEPLOYMENT_NAME)
    @Test
    public void testEarDeploymentConfigurationResource() throws Exception {
        ModelNode loggingConfiguration = readDeploymentResource(EAR_DEPLOYMENT_NAME, EAR_DEPLOYMENT_NAME + "/META-INF/logging.properties");
        // The address should have logging.properties
        Deque<Property> resultAddress = new ArrayDeque<>(Operations.getOperationAddress(loggingConfiguration).asPropertyList());
        Assert.assertTrue("The configuration path did not include logging.properties", resultAddress.getLast().getValue().asString().contains("logging.properties"));

        ModelNode handler = loggingConfiguration.get("handler", "FILE");
        Assert.assertTrue("The FILE handler was not found effective configuration", handler.isDefined());
        Assert.assertTrue(handler.hasDefined("properties"));
        String fileName = null;
        // Find the fileName property
        for (Property property : handler.get("properties").asPropertyList()) {
            if ("fileName".equals(property.getName())) {
                fileName = property.getValue().asString();
                break;
            }
        }
        Assert.assertNotNull("fileName property not found", fileName);
        Assert.assertTrue(fileName.endsWith("test-logging-ear.log"));

        // Check the WAR which should inherit the EAR's logging.properties
        loggingConfiguration = readSubDeploymentResource(EAR_DEPLOYMENT_NAME, EAR_WAR_DEPLOYMENT_NAME, EAR_DEPLOYMENT_NAME + "/META-INF/logging.properties");
        // The address should have logging.properties
        resultAddress = new ArrayDeque<>(Operations.getOperationAddress(loggingConfiguration).asPropertyList());
        Assert.assertTrue("The configuration path did not include logging.properties", resultAddress.getLast().getValue().asString().contains("logging.properties"));

        handler = loggingConfiguration.get("handler", "FILE");
        Assert.assertTrue("The FILE handler was not found effective configuration", handler.isDefined());
        Assert.assertTrue(handler.hasDefined("properties"));
        fileName = null;
        // Find the fileName property
        for (Property property : handler.get("properties").asPropertyList()) {
            if ("fileName".equals(property.getName())) {
                fileName = property.getValue().asString();
                break;
            }
        }
        Assert.assertNotNull("fileName property not found", fileName);
        Assert.assertTrue(fileName.endsWith("test-logging-ear.log"));
    }

    @OperateOnDeployment(EAR_PARENT_DEPLOYMENT_NAME)
    @Test
    public void testDeploymentConfigurationResource() throws Exception {
        ModelNode loggingConfiguration = readDeploymentResource(EAR_PARENT_DEPLOYMENT_NAME, EAR_PARENT_DEPLOYMENT_NAME + "/META-INF/logging.properties");
        // The address should have logging.properties
        Deque<Property> resultAddress = new ArrayDeque<>(Operations.getOperationAddress(loggingConfiguration).asPropertyList());
        Assert.assertTrue("The configuration path did not include logging.properties", resultAddress.getLast().getValue().asString().contains("logging.properties"));

        ModelNode handler = loggingConfiguration.get("handler", "FILE");
        Assert.assertTrue("The FILE handler was not found effective configuration", handler.isDefined());
        Assert.assertTrue(handler.hasDefined("properties"));
        String fileName = null;
        // Find the fileName property
        for (Property property : handler.get("properties").asPropertyList()) {
            if ("fileName".equals(property.getName())) {
                fileName = property.getValue().asString();
                break;
            }
        }
        Assert.assertNotNull("fileName property not found", fileName);
        Assert.assertTrue(fileName.endsWith("test-logging-parent-ear.log"));

        // Check the WAR which should have it's own configuration
        loggingConfiguration = readSubDeploymentResource(EAR_PARENT_DEPLOYMENT_NAME, EAR_CHILD_WAR_DEPLOYMENT_NAME, EAR_CHILD_WAR_DEPLOYMENT_NAME + "/META-INF/logging.properties");
        // The address should have logging.properties
        resultAddress = new ArrayDeque<>(Operations.getOperationAddress(loggingConfiguration).asPropertyList());
        Assert.assertTrue("The configuration path did not include logging.properties", resultAddress.getLast().getValue().asString().contains("logging.properties"));

        handler = loggingConfiguration.get("handler", "FILE");
        Assert.assertTrue("The FILE handler was not found effective configuration", handler.isDefined());
        Assert.assertTrue(handler.hasDefined("properties"));
        fileName = null;
        // Find the fileName property
        for (Property property : handler.get("properties").asPropertyList()) {
            if ("fileName".equals(property.getName())) {
                fileName = property.getValue().asString();
                break;
            }
        }
        Assert.assertNotNull("fileName property not found", fileName);
        Assert.assertTrue(fileName.endsWith("test-logging-child-war.log"));
    }

    static ModelNode executeOperation(final ModelNode op) throws IOException {
        ModelNode result = client.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            Assert.assertTrue(Operations.getFailureDescription(result).toString(), false);
        }
        return result;
    }

    /**
     * Reads the deployment resource.
     *
     * @param deploymentName    the name of the deployment
     * @param configurationName the name of the configuration for the address
     *
     * @return the model for the deployment
     *
     * @throws IOException if an error occurs connecting to the server
     */
    static ModelNode readDeploymentResource(final String deploymentName, final String configurationName) throws IOException {
        ModelNode address = Operations.createAddress("deployment", deploymentName, "subsystem", "logging", "configuration", configurationName);
        ModelNode op = Operations.createReadResourceOperation(address, true);
        op.get("include-runtime").set(true);
        final ModelNode result = Operations.readResult(executeOperation(op));
        // Add the address on the result as the tests might need it
        result.get(ModelDescriptionConstants.OP_ADDR).set(address);
        return result;
    }

    /**
     * Reads the deployment resource.
     *
     * @param deploymentName    the name of the deployment
     * @param subDeploymentName the name of the sub-deployment to read the configuration from
     * @param configurationName the name of the configuration for the address
     *
     * @return the model for the deployment
     *
     * @throws IOException if an error occurs connecting to the server
     */
    static ModelNode readSubDeploymentResource(final String deploymentName, final String subDeploymentName, final String configurationName) throws IOException {
        ModelNode address = Operations.createAddress("deployment", deploymentName, "subdeployment", subDeploymentName, "subsystem", "logging", "configuration", configurationName);
        ModelNode op = Operations.createReadResourceOperation(address, true);
        op.get("include-runtime").set(true);
        final ModelNode result = Operations.readResult(executeOperation(op));
        // Add the address on the result as the tests might need it
        result.get(ModelDescriptionConstants.OP_ADDR).set(address);
        return result;
    }

    private static Asset createAsset(final Properties properties) {
        return () -> {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                properties.store(out, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new ByteArrayInputStream(out.toByteArray());
        };
    }


    private static Properties createLoggingConfig(final String fileName) {
        final Properties config = new Properties();
        config.setProperty("logger.handlers", "FILE");

        config.setProperty("handler.FILE", "org.jboss.logmanager.handlers.PeriodicRotatingFileHandler");
        config.setProperty("handler.FILE.level", "${jboss.boot.server.log.console.level:ALL}");
        config.setProperty("handler.FILE.properties", "autoFlush,fileName");
        config.setProperty("handler.FILE.autoFlush", "true");
        config.setProperty("handler.FILE.fileName", "${jboss.server.log.dir}/" + fileName);
        config.setProperty("handler.FILE.formatter", "PATTERN");

        config.setProperty("formatter.PATTERN", "org.jboss.logmanager.formatters.PatternFormatter");
        config.setProperty("formatter.PATTERN.properties", "pattern");
        config.setProperty("formatter.PATTERN.pattern", "%d{HH:mm:ss,SSS} %C.%M:%L (%t) %5p %c{1}:%L - %m%n");
        return config;
    }

}
